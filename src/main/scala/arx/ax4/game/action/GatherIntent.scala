package arx.ax4.game.action
import arx.ax4.game.entities.Companions.ResourceSourceData
import arx.ax4.game.entities.{BaseGatherProspect, GatherMethod, GatherProspect, Resource, ResourceKey, ResourceSourceData, TargetPattern, Tiles, UntargetedGatherProspect}
import arx.ax4.game.logic.{GatherLogic, InventoryLogic}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView

object GatherIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = {
		Left(new GameActionIntentInstance {
			val hexSelector = BiasedHexSelector(TargetPattern.Point, (view, v) => {
				val tileEnt = Tiles.tileAt(v.vec)
				view.dataOpt[ResourceSourceData](tileEnt).exists( rsrc => rsrc.resources.nonEmpty )
			})

			var resourceSelector = ResourceGatherSelector(Nil)

			override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = {
				if (!resultsSoFar.fullySatisfied(hexSelector)) {
					Some(hexSelector)
				} else if (resourceSelector.resources.isEmpty || !resultsSoFar.fullySatisfied(resourceSelector)) {
					val tileEnt = Tiles.tileAt(resultsSoFar.build().single(hexSelector).vec)
					val prospects = GatherLogic.gatherProspectsFor(entity, tileEnt)
					resourceSelector = ResourceGatherSelector(prospects)
					Some(resourceSelector)
				} else {
					None
				}
			}

			override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
				val bhex = selectionResult.single(hexSelector)
				val rsrc = selectionResult.single(resourceSelector)

				GatherAction(entity, Tiles.tileAt(bhex.vec), rsrc.key, rsrc.method) :: Nil
			}
		})
	}

	override def displayName(implicit view: WorldView): String = "Gather"
}


case class GatherAction(gatherer : Entity, target : Entity, resourceKey : ResourceKey, method : GatherMethod) extends GameAction {
	override def identity: Taxon = Taxonomy("gather action")

	override def entity: Entity = gatherer
}



case class GatherSelectionProspect(gatherer : Entity, target : Entity, key : ResourceKey, method : GatherMethod) extends BaseGatherProspect {
	def toGatherProspect(view : WorldView) : Option[GatherProspect] = {
		method.toolRequirements match {
			case Some(_) => GatherLogic.bestToolFor(InventoryLogic.heldAndEquippedItems(gatherer)(view), method)(view).map(tool => GatherProspect(gatherer, target, key, method, Some(tool)))
			case _ => Some(GatherProspect(gatherer, target, key, method, None))
		}
	}
}

case class ResourceGatherSelector(resources : Iterable[GatherSelectionProspect]) extends Selector[GatherSelectionProspect] {

	override def satisfiedBy(view: WorldView, a: Any): Option[(GatherSelectionProspect, Int)] = {
		implicit val implview = view
		a match {
			case gsp @ GatherSelectionProspect(gatherer, target, key, method) =>
				if (GatherLogic.canGather(gsp)) {
					Some(gsp -> 1)
				} else {
					None
				}
			case _ => None
		}
	}

	override def description: String = "resource"
}