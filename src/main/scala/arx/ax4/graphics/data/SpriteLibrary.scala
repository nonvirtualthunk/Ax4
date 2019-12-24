package arx.ax4.graphics.data

import arx.engine.control.components.windowing.widgets.{SpriteDefinition, SpriteProvider}
import arx.engine.entity.{Taxon, Taxonomy}
import arx.graphics.{Image, TToImage}
import arx.resource.ResourceManager

import scala.collection.mutable

object SpriteLibrary extends SpriteProvider {
	var sprites : Map[Taxon, SpriteDefinition] = Map()


	def iconFor(t : Taxon) : TToImage = {
		val examineQueue = new mutable.Queue[Taxon]()
		examineQueue.enqueue(t)
		while (examineQueue.nonEmpty) {
			val nextT = examineQueue.dequeue()
			sprites.get(nextT) match {
				case Some(SpriteDefinition(icon,_)) => return icon
				case _ => // do nothing
			}
			nextT.parents.foreach(p => examineQueue.enqueue(p))
		}
		ResourceManager.defaultImage
	}

	override def getSpriteDefinitionFor(t : Taxon) : Option[SpriteDefinition] = {
		val examineQueue = new mutable.Queue[Taxon]()
		examineQueue.enqueue(t)
		while (examineQueue.nonEmpty) {
			val nextT = examineQueue.dequeue()
			sprites.get(nextT) match {
				case sd @ Some(_) => return sd
				case _ => // do nothing
			}
			nextT.parents.foreach(p => examineQueue.enqueue(p))
		}
		None
	}
	override def spriteDefinitionFor(t : Taxon) : SpriteDefinition = {
		getSpriteDefinitionFor(t).getOrElse(SpriteDefinition(ResourceManager.defaultImage, ResourceManager.defaultImage))
	}

	{
		for (overallSpritesConf <- ResourceManager.sml("graphics/data/entity/Sprites.sml").fieldOpt("Sprites")) {
			for ((spriteTaxon, spriteConf) <- overallSpritesConf.fields) {
				val iconStr = spriteConf.icon.str
				val icon16Str = spriteConf.icon16.strOrElse(iconStr)
				sprites += (Taxonomy.byNameExpr(spriteTaxon) -> SpriteDefinition(iconStr, icon16Str))
			}
		}
	}
}


object CardImageLibrary {
//	var cardImages = Map[Taxon, TToImage]()
//
//	def cardImageFor(taxon : Taxon) : Image = {
//		cardImageOpt(taxon).getOrElse(ResourceManager.blankImage)
//	}
//
//	def cardImageOpt(taxon : Taxon) : Option[Image] = {
//		cardImages.get(taxon) match {
//			case Some(img) => Some(img.image)
//			case None => taxon.parents.flatMap(cardImageOpt).headOption
//		}
//	}
//
//	{
//		for (overallSpritesConf <- ResourceManager.sml("graphics/data/entity/CardImages.sml").fieldOpt("CardImages")) {
//			for ((cardTaxon, image) <- overallSpritesConf.fields) {
//
//				cardImages += (Taxonomy.byNameExpr(cardTaxon) -> (image.str : TToImage))
//			}
//		}
//	}

	var cardImages = Map[String, TToImage]()

	def cardImageFor(name : String) : Image = {
		cardImageOpt(name).getOrElse(ResourceManager.blankImage)
	}

	def cardImageOpt(name : String) : Option[Image] = {
		cardImages.get(name.toLowerCase()).map(_.image)
	}

	{
		for (overallSpritesConf <- ResourceManager.sml("graphics/data/entity/CardImages.sml").fieldOpt("CardImages")) {
			for ((cardName, image) <- overallSpritesConf.fields) {

				cardImages += (cardName.toLowerCase() -> (image.str : TToImage))
			}
		}
	}

}