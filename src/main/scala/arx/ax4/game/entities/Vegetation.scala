package arx.ax4.game.entities

import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigLoadable, TNestedData}
import arx.engine.entity.{Taxon, Taxonomy}
import arx.resource.ResourceManager
import arx.Prelude._

@GenerateCompanion
case class VegetationLayer(var layer: Int = 0,
									var cover: Int = 0,
									var moveCost: Sext = Sext(0),
									var kind: Taxon = Taxonomy.UnknownThing)
	extends ConfigLoadable with TNestedData

@GenerateCompanion
class Vegetation extends AxAuxData {
	var layers = Vector[VegetationLayer]()

	def cover = layers.isum(_.cover)
	def moveCost = layers.map(_.moveCost).foldLeft(Sext(0))(_ + _)
	def kinds = layers.map(_.kind)
}

object VegetationLibrary {
	val Sentinel = VegetationLayer()
	var vegetationLayersByKind = Map[Taxon, VegetationLayer]()

	def withKind(kind : Taxon) = vegetationLayersByKind.getOrElse(kind, Sentinel)

	protected def load(config : ConfigValue) : Unit = {
		for (vegConf <- config.fieldOpt("Vegetation")) {
			for ((kind, dataConf) <- vegConf.fields) {
				val vlayer = VegetationLayer().loadFromConfig(dataConf)
				vlayer.kind = Taxonomy(kind)
				vegetationLayersByKind += Taxonomy(kind) -> vlayer
			}
		}
	}

	load(ResourceManager.sml("game/data/vegetation/Vegetations.sml"))
}