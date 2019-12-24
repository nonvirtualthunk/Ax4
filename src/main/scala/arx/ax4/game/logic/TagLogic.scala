package arx.ax4.game.logic

import arx.ax4.game.entities.TagData
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.WorldView

object TagLogic {
	def sumOfFlags(entity : Entity, flags : Set[Taxon])(implicit view : WorldView) : Int = {
		val f = allFlags(entity)
		flags.map(t => f.getOrElse(t, 0)).sum
	}

	def hasTag(entity : Entity, tag : Taxon)(implicit view : WorldView) : Boolean = allTags(entity).contains(tag)

	def allFlags(entity : Entity)(implicit view : WorldView) : Map[Taxon, Int] = entity.dataOpt[TagData].map(_.flags).getOrElse(Map())

	def allTags(entity : Entity)(implicit view : WorldView) : Set[Taxon] = entity.dataOpt[TagData].map(_.tags).getOrElse(Set())
}
