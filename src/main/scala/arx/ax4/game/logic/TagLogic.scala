package arx.ax4.game.logic

import arx.Prelude
import arx.Prelude.sign
import arx.ax4.game.entities.Companions.TagData
import arx.ax4.game.entities.{AttackModifier, FlagEquivalency, FlagLibrary, TagData}
import arx.ax4.game.event.ChangeFlagEvent
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._

object TagLogic {
	def sumOfFlags(entity : Entity, flags : Set[Taxon])(implicit view : WorldView) : Int = {
		val TD = entity[TagData]
		flags.map(f => flagValue(TD, f)).sum
	}

	def rawFlagValue(ent: Entity, flag: Taxon)(implicit view : WorldView) : Int = rawFlagValue(ent[TagData], flag)
	def rawFlagValue(td: TagData, flag: Taxon)(implicit view : WorldView) : Int = td.flags.getOrElse(flag, 0)

	def flagValue(td : TagData, flag : Taxon)(implicit view : WorldView) : Int = {
		val equivalentFlagsSum = FlagEquivalency.flagEquivalences.get(flag)
			.map(eq => {
				val otherFlagValue = flagValue(td, eq.taxon)
				otherFlagValue * eq.multiplier + (sign(otherFlagValue) * eq.adder)
			})
			.sum
		rawFlagValue(td, flag) + equivalentFlagsSum
	}
	def flagValue(entity : Entity, flag : Taxon)(implicit view : WorldView) : Int = {

	import arx.Prelude._

	entity.dataOpt[TagData].map(td => flagValue(td, flag)).getOrElse(0)
	}
	def flagValue(entity : Entity, flag : String)(implicit view : WorldView) : Int = {
		flagValue(entity, Taxonomy(flag, "Flags"))
	}

	def changeFlagBy(entity : Entity, flag : String, delta : Int, limitToZero : Boolean = true)(implicit world : World) : Int = {
		changeFlagBy(entity, Taxonomy(flag, "Flags"), delta, limitToZero)
	}
	def changeFlagBy(entity : Entity, flag : Taxon, delta : Int, limitToZero : Boolean)(implicit world : World) : Int = {
		val flagInfo = FlagLibrary.getWithKind(flag)

		val curValue = rawFlagValue(entity, flag)(world.view)
		val newLimitedValue = if (limitToZero || flagInfo.exists(_.limitToZero)) {
			(curValue + delta).max(0)
		} else {
			curValue + delta
		}

		val newValue = if (flagInfo.exists(_.binary)) {
			newLimitedValue.max(0).min(1)
		} else {
			newLimitedValue
		}

		if (newValue != curValue) {
			world.startEvent(ChangeFlagEvent(entity, flag, curValue, newValue))
			world.modify(entity, TagData.flags.incrementKey(flag, newValue - curValue))
			world.endEvent(ChangeFlagEvent(entity, flag, curValue, newValue))
		}
		curValue
	}

	def changeFlagTo(entity : Entity, flag : Taxon, value : Int)(implicit world : World) : Int = {
		val curValue = rawFlagValue(entity, flag)(world.view)
		if (curValue != value) {
			world.startEvent(ChangeFlagEvent(entity, flag, curValue, value))
			world.modify(entity, TagData.flags.incrementKey(flag, value - curValue))
			world.endEvent(ChangeFlagEvent(entity, flag, curValue, value))
		}
		curValue
	}

	def hasTag(entity : Entity, tag : Taxon)(implicit view : WorldView) : Boolean = allTags(entity).contains(tag)

	def allFlags(entity : Entity)(implicit view : WorldView) : Map[Taxon, Int] = entity.dataOpt[TagData].map(_.flags).getOrElse(Map())

	def allFlagAttackModifiers(entity : Entity)(implicit view : WorldView) : Map[Taxon, Vector[AttackModifier]] = {
		for ((flag,count) <- allFlags(entity); if count > 0; flagInfo <- FlagLibrary.getWithKind(flag)) yield {
			flag -> flagInfo.attackModifiers
		}
	}

	def allTags(entity : Entity)(implicit view : WorldView) : Set[Taxon] = entity.dataOpt[TagData].map(_.tags).getOrElse(Set())
}
