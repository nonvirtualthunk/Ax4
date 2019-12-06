package arx.ax4.game.logic

import arx.ax4.game.action.GatherSelectionProspect
import arx.ax4.game.entities.Companions.{CharacterInfo, Resource, ResourceSourceData}
import arx.ax4.game.entities.{GatherMethod, GatherProspect, ItemLibrary, ResourceKey, ResourceSourceData}
import arx.ax4.game.event.{GatherEvent, ResourceGatheredEvent}
import arx.engine.entity.{Entity, Taxonomy}
import arx.engine.world.{NestedKeyedModifier, World, WorldView}

object GatherLogic {
	import arx.core.introspection.FieldOperations._

	def bestToolFor(tools : Iterable[Entity], gatherMethod : GatherMethod)(implicit view : WorldView) : Option[Entity] = {
		// TODO : incorporate some sort of notion of tool quality
		gatherMethod.toolRequirements.flatMap(req => tools.find(t => req.isTrueFor(view, t)))
	}

	def gatherProspectsFor(gatherer : Entity, target : Entity)(implicit view : WorldView) = {
		view.dataOpt[ResourceSourceData](target) match {
			case Some(rsrcs) =>
				rsrcs.resources.flatMap {
					case (key,rsrc) => rsrc.gatherMethods.map(method => GatherSelectionProspect(gatherer, target, key, method))
				}
			case None => Nil
		}
	}

	def gather(prospect : GatherProspect)(implicit game : World): Unit = {
		implicit val view = game.view
		import prospect._

		game.startEvent(GatherEvent(prospect))
		game.modify(gatherer, CharacterInfo.actionPoints reduceBy method.actionCost)
		game.modify(gatherer, CharacterInfo.stamina reduceBy method.staminaCost)

		val skillLevel = SkillsLogic.effectiveSkillLevel(gatherer, method.skills)
		SkillsLogic.gainSkillXP(gatherer, method.skills, SkillsLogic.xpGainFor(skillLevel, method.difficulty))

		val resourceSrcData = target[ResourceSourceData]
		val resource = resourceSrcData.resources(key)
		val amountGained = method.amount.min(resource.amount.currentValue)

		game.startEvent(ResourceGatheredEvent(gatherer, resource.kind, amountGained))
		val arch = ItemLibrary.withKind(resource.kind)
		for (_ <- 0 until amountGained) {
			InventoryLogic.transferItem(item = arch.createEntity(game), to = Some(gatherer))
		}

		game.modify(target, NestedKeyedModifier(ResourceSourceData.resources, key, Resource.amount reduceBy amountGained))

		game.endEvent(ResourceGatheredEvent(gatherer, resource.kind, amountGained))

		game.endEvent(GatherEvent(prospect))
	}

	def cantGatherReason(prospect : GatherSelectionProspect)(implicit view : WorldView) : Option[String] = {
		prospect.toGatherProspect(view) match {
			case Some(p) => cantGatherReason(p)
			case None => Some("No tool")
		}

	}

	def cantGatherReason(prospect : GatherProspect)(implicit view : WorldView) : Option[String] = {
		val skillLevel = SkillsLogic.effectiveSkillLevel(prospect.gatherer, prospect.method.skills)
		if (prospect.method.difficulty > skillLevel) {
			Some("Insufficient skill")
		} else if (!prospect.method.requirements.isTrueFor(view, prospect)) {
			Some("Does not meet requirements")
		} else if (prospect.method.actionCost > CharacterLogic.curActionPoints(prospect.gatherer)) {
			Some("Insufficient AP")
		} else {
			None
		}
	}

	def canGather(prospect : GatherSelectionProspect)(implicit view : WorldView) : Boolean = {
		cantGatherReason(prospect).isEmpty
	}

	def canGather(prospect : GatherProspect)(implicit view : WorldView) : Boolean = {
		cantGatherReason(prospect).isEmpty
	}
}
