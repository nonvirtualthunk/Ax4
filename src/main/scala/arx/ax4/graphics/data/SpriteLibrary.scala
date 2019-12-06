package arx.ax4.graphics.data

import arx.engine.control.components.windowing.widgets.{SpriteDefinition, SpriteProvider}
import arx.engine.entity.{Taxon, Taxonomy}
import arx.graphics.TToImage
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
