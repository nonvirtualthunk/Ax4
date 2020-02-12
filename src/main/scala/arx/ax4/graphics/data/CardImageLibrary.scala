package arx.ax4.graphics.data

import arx.graphics.{Image, TToImage}
import arx.resource.ResourceManager

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