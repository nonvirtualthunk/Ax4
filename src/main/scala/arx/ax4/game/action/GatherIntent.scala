package arx.ax4.game.action
import arx.ax4.game.entities.Companions.ResourceSourceData
import arx.ax4.game.entities.{GatherMethod, GatherProspect, Resource, ResourceKey, ResourceSourceData, TargetPattern, Tiles, UntargetedGatherProspect}
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

			override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = {
				if (!resultsSoFar.fullySatisfied(hexSelector)) {
					Some(hexSelector)
				} else if (resourceSelector.resources.isEmpty || !resultsSoFar.fullySatisfied(resourceSelector)) {
					val tileEnt = Tiles.tileAt(resultsSoFar.build().single(hexSelector).vec)
					val rsrcs = view.data[ResourceSourceData](tileEnt)
					val prospects = rsrcs.resources.flatMap { case (key,rsrc) => rsrc.gatherMethods.map(method => GatherSelectionProspect(entity, tileEnt, key, method)) }
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
}


case class GatherAction(gatherer : Entity, target : Entity, resourceKey : ResourceKey, method : GatherMethod) extends GameAction {
	override def identity: Taxon = Taxonomy("gather action")

	override def entity: Entity = gatherer
}



case class GatherSelectionProspect(gatherer : Entity, target : Entity, key : ResourceKey, method : GatherMethod) {
	def toGatherProspect(view : WorldView) : Option[GatherProspect] = {
		GatherLogic.bestToolFor(InventoryLogic.heldAndEquippedItems(gatherer)(view), method)(view).map(tool => GatherProspect(gatherer, target, key, method, tool))
	}
}

case class ResourceGatherSelector(resources : Iterable[GatherSelectionProspect]) extends Selector[GatherSelectionProspect] {

	override def satisfiedBy(view: WorldView, a: Any): Option[(GatherSelectionProspect, Int)] = {
		implicit val implview = view
		a match {
			case gsp @ GatherSelectionProspect(gatherer, target, key, method) =>
				val tools = InventoryLogic.heldAndEquippedItems(gatherer)
				GatherLogic.bestToolFor(tools, method) match {
					case Some(tool) =>
						if (GatherLogic.canGather(GatherProspect(gatherer, target, key, method, tool))) {
							Some(gsp -> 1)
						} else {
							None
						}
					case None => None
				}
			case _ => None
		}
	}

	override def description: String = "resource"
}