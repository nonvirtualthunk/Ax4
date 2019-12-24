package arx.ax4.game.action
import arx.ax4.game.entities.Companions.ResourceSourceData
import arx.ax4.game.entities.{BaseGatherProspect, GatherMethod, GatherProspect, Resource, ResourceKey, ResourceSourceData, TargetPattern, Tiles}
import arx.ax4.game.logic.{GatherLogic, InventoryLogic}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView


case class GatherAction(gatherer : Entity, target : Entity, resourceKey : ResourceKey, method : GatherMethod) extends GameAction {
	override def identity: Taxon = Taxonomy("gather action")

	override def entity: Entity = gatherer
}


case class ResourceGatherSelector(resources : Iterable[GatherProspect], selectable : Selectable) extends Selector[GatherProspect](selectable) {
	override def satisfiedBy(view: WorldView, a: Any): Option[(GatherProspect, Int)] = {
		implicit val implview = view
		a match {
			case gsp @ GatherProspect(gatherer, target, key, method) =>
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