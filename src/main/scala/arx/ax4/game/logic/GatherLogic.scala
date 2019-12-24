package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.{CharacterInfo, Resource, ResourceSourceData}
import arx.ax4.game.entities.{GatherMethod, GatherProspect, ItemLibrary, ResourceKey, ResourceSourceData}
import arx.ax4.game.event.{GatherEvent, ResourceGatheredEvent}
import arx.engine.entity.{Entity, Taxonomy}
import arx.engine.world.{NestedKeyedModifier, World, WorldView}

object GatherLogic {
	import arx.core.introspection.FieldOperations._

	def gatherProspectsFor(gatherer : Entity, target : Entity)(implicit view : WorldView) = {
		view.dataOpt[ResourceSourceData](target) match {
			case Some(rsrcs) =>
				rsrcs.resources.flatMap {
					case (key,rsrc) => rsrc.gatherMethods.map(method => GatherProspect(gatherer, target, key, method))
				}
			case None => Nil
		}
	}

	def gather(prospect : GatherProspect)(implicit game : World): Unit = {
		implicit val view = game.view
		import prospect._

		game.startEvent(GatherEvent(prospect))
		game.modify(gatherer, CharacterInfo.actionPoints reduceBy method.actionCostDelta)
		game.modify(gatherer, CharacterInfo.stamina reduceBy method.staminaCostDelta)

		val skillLevel = SkillsLogic.effectiveSkillLevel(gatherer, method.skills)
		SkillsLogic.gainSkillXP(gatherer, method.skills, SkillsLogic.xpGainFor(skillLevel, method.difficulty))

		val resourceSrcData = target[ResourceSourceData]
		val resource = resourceSrcData.resources(key)
		val maxAmountGained = method.amount + TagLogic.sumOfFlags(prospect.gatherer, method.gatherFlags)
		val amountGained = maxAmountGained.min(resource.amount.currentValue)

		game.startEvent(ResourceGatheredEvent(gatherer, resource.kind, amountGained))
		val arch = ItemLibrary.withKind(resource.kind)
		for (_ <- 0 until amountGained) {
			InventoryLogic.transferItem(item = arch.createEntity(game), to = Some(gatherer))
		}

		game.modify(target, NestedKeyedModifier(ResourceSourceData.resources, key, Resource.amount reduceBy amountGained))

		game.endEvent(ResourceGatheredEvent(gatherer, resource.kind, amountGained))

		game.endEvent(GatherEvent(prospect))
	}

	def cantGatherReason(prospect : GatherProspect)(implicit view : WorldView) : Option[String] = {
		val skillLevel = SkillsLogic.effectiveSkillLevel(prospect.gatherer, prospect.method.skills)
		if (prospect.method.difficulty > skillLevel) {
			Some("Insufficient skill")
		} else if (!prospect.method.requirements.isTrueFor(view, prospect)) {
			Some("Does not meet requirements")
		} else if (prospect.method.actionCostDelta > CharacterLogic.curActionPoints(prospect.gatherer)) {
			Some("Insufficient AP")
		} else {
			None
		}
	}

	def canGather(prospect : GatherProspect)(implicit view : WorldView) : Boolean = {
		cantGatherReason(prospect).isEmpty
	}
}
