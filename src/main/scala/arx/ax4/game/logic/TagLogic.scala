package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.TagData
import arx.ax4.game.entities.{FlagEquivalency, FlagLibrary, TagData}
import arx.ax4.game.event.ChangeFlagEvent
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._

object TagLogic {
	def sumOfFlags(entity : Entity, flags : Set[Taxon])(implicit view : WorldView) : Int = {
		val TD = entity[TagData]
		flags.map(f => flagValue(TD, f)).sum
	}

	def flagValue(td : TagData, flag : Taxon)(implicit view : WorldView) : Int = {
		td.flags.getOrElse(flag, 0) + FlagEquivalency.flagEquivalences.get(flag).map(f => flagValue(td, f)).sum
	}
	def flagValue(entity : Entity, flag : Taxon)(implicit view : WorldView) : Int = {
		entity.dataOpt[TagData].map(td => flagValue(td, flag)).getOrElse(0)
	}
	def flagValue(entity : Entity, flag : String)(implicit view : WorldView) : Int = {
		flagValue(entity, Taxonomy(flag, "Flags"))
	}

	def changeFlagBy(entity : Entity, flag : String, delta : Int, limitToZero : Boolean = true)(implicit world : World) : Int = {
		changeFlagBy(entity, Taxonomy(flag, "Flags"), delta, limitToZero)
	}
	def changeFlagBy(entity : Entity, flag : Taxon, delta : Int, limitToZero : Boolean)(implicit world : World) : Int = {
		val curValue = flagValue(entity, flag)(world.view)
		val newValue = if (limitToZero) {
			(curValue + delta).max(0)
		} else {
			curValue + delta
		}
		if (newValue != curValue) {
			world.startEvent(ChangeFlagEvent(entity, flag, curValue, newValue))
			world.modify(entity, TagData.flags.incrementKey(flag, newValue - curValue))
			world.endEvent(ChangeFlagEvent(entity, flag, curValue, newValue))
		}
		curValue
	}

	def changeFlagTo(entity : Entity, flag : Taxon, value : Int)(implicit world : World) : Int = {
		val curValue = flagValue(entity, flag)(world.view)
		world.startEvent(ChangeFlagEvent(entity, flag, curValue, 0))
		world.modify(entity, TagData.flags.incrementKey(flag, -curValue))
		world.endEvent(ChangeFlagEvent(entity, flag, curValue, 0))
		curValue
	}

	def hasTag(entity : Entity, tag : Taxon)(implicit view : WorldView) : Boolean = allTags(entity).contains(tag)

	def allFlags(entity : Entity)(implicit view : WorldView) : Map[Taxon, Int] = entity.dataOpt[TagData].map(_.flags).getOrElse(Map())

	def allTags(entity : Entity)(implicit view : WorldView) : Set[Taxon] = entity.dataOpt[TagData].map(_.tags).getOrElse(Set())
}
