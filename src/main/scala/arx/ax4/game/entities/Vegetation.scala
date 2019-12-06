package arx.ax4.game.entities

import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigLoadable, Reduceable, TNestedData}
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
import arx.resource.ResourceManager
import arx.Prelude._
import arx.ax4.game.entities.Conditionals.{BaseGatherConditional, EntityConditional}
import arx.ax4.game.logic.InventoryLogic
import arx.engine.world.WorldView

import scala.collection.SortedMap

@GenerateCompanion
case class VegetationLayer(var cover: Int = 0,
									var moveCost: Sext = Sext(0),
									var kind: Taxon = Taxonomy.UnknownThing)
	extends ConfigLoadable with TNestedData

@GenerateCompanion
class Vegetation extends AxAuxData {
	var layers = SortedMap[VegetationLayerType, VegetationLayer]()

	def cover = layers.values.isum(_.cover)
	def moveCost = layers.values.map(_.moveCost).foldLeft(Sext(0))(_ + _)
	def kinds = layers.values.map(_.kind)
}

object VegetationLibrary {
	val Sentinel = VegetationLayer()
	var vegetationLayersByKind = Map[Taxon, VegetationLayer]()

	def withKind(kind : Taxon) = vegetationLayersByKind.getOrElse(kind, Sentinel)

	protected def load(config : ConfigValue) : Unit = {
		for (vegConf <- config.fieldOpt("Vegetation")) {
			for ((kind, dataConf) <- vegConf.fields) {
				val vlayer = VegetationLayer().loadFromConfig(dataConf)
				vlayer.kind = Taxonomy(kind, "Vegetations")
				vegetationLayersByKind += Taxonomy(kind, "Vegetations") -> vlayer
			}
		}
	}

	load(ResourceManager.sml("game/data/vegetation/Vegetations.sml"))
}

@GenerateCompanion
class ResourceSourceData extends AxAuxData {
	var resources : Map[ResourceKey, Resource] = Map()
}

// e.g. ResourceOrigin(Vegetation(Canopy), Wood)
case class ResourceKey(origin : ResourceOrigin, kind : Taxon)

sealed abstract class VegetationLayerType(val i : Int) extends Ordered[VegetationLayerType] {
	override def compare(that: VegetationLayerType): Int = this.i - that.i
}
case object VegetationLayerType {
	case object GroundCover extends VegetationLayerType(0)
	case object Shrubbery extends VegetationLayerType(1)
	case object Canopy extends VegetationLayerType(2)
}

sealed trait ResourceOrigin
case object ResourceOrigin {
	case class Vegetation(layer : VegetationLayerType) extends ResourceOrigin
	case object Terrain extends  ResourceOrigin
}


import UnitOfGameTimeFloat._

@GenerateCompanion
case class Resource(var kind : Taxon = Taxonomy.UnknownThing,
						  var amount : Reduceable[Int] = Reduceable(1),
						  var recoveryAmount : Int = 0,
						  var recoveryPeriod : UnitOfGameTime = 1.gameDay,
						  var canRecoverFromZero : Boolean = true,
						  var structural : Boolean = false,
						  var gatherMethods : Vector[GatherMethod] = Vector()) extends TNestedData

@GenerateCompanion
case class GatherMethod(var name : String = "unnamed",
								var toolRequirements : Option[EntityConditional] = None,
								var requirements : BaseGatherConditional = Conditionals.all,
								var actionCost : Int = 1,
								var staminaCost : Int = 1,
								var skills : Seq[Taxon] = Nil,
								var difficulty : Int = 0,
								var amount : Int = 1) extends TNestedData


trait BaseGatherProspect {
	def gatherer : Entity
	def target : Entity
	def key : ResourceKey
}
case class UntargetedGatherProspect(gatherer : Entity, target : Entity, key : ResourceKey) extends BaseGatherProspect
case class GatherProspect(gatherer : Entity, target : Entity, key : ResourceKey, method : GatherMethod, tool : Option[Entity]) extends BaseGatherProspect

object GatherConditionals {

}
