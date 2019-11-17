package arx.ax4.graphics.data

import arx.engine.entity.{Taxon, Taxonomy}
import arx.graphics.TToImage
import arx.resource.ResourceManager

import scala.collection.mutable

object SpriteLibrary {
	var sprites : Map[Taxon, SpriteDefinition] = Map()


	def iconFor(t : Taxon) : TToImage = {
		val examineQueue = new mutable.Queue[Taxon]()
		examineQueue.enqueue(t)
		while (examineQueue.nonEmpty) {
			val nextT = examineQueue.dequeue()
			sprites.get(nextT) match {
				case Some(SpriteDefinition(icon)) => return icon
				case _ => // do nothing
			}
			nextT.parents.foreach(p => examineQueue.enqueue(p))
		}
		ResourceManager.defaultImage
	}

	{
		for (overallSpritesConf <- ResourceManager.sml("graphics/data/entity/Sprites.sml").fieldOpt("Sprites")) {
			for ((spriteTaxon, spriteConf) <- overallSpritesConf.fields) {
				sprites += (Taxonomy(spriteTaxon) -> SpriteDefinition(spriteConf.icon.str))
			}
		}
	}
}


case class SpriteDefinition(icon : TToImage) {}