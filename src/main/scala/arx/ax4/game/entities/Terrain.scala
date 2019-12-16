package arx.ax4.game.entities

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import arx.ax4.game.entities.Companions.Terrain
import arx.ax4.game.event.EntityCreated
import arx.core.async.Executor
import arx.core.introspection.{Clazz, CopyAssistant, ReflectionAssistant, TEagerSingleton}
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.metrics.Metrics
import arx.core.representation.ConfigValue
import arx.engine.data.TAuxData
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
import arx.engine.world.World
import arx.resource.ResourceManager

import scala.reflect.ClassTag

@GenerateCompanion
class Terrain extends AxAuxData {
	var descriptors = List[String]() // i.e. [rich] hills, [fertile] plains, what have you
	var fertility = 0
	var cover = 0
	var elevation = 0
	var moveCost = Sext(1)
	var kind : Taxon = Taxonomy.UnknownThing
}

object TerrainLibrary extends AuxDataLibrary(Terrain) {
	override protected def topLevelField: String = "Terrain"
	override protected def createBlank(): Terrain = new Terrain

	override def initialLoad(): Unit = {
		load("game/data/terrain/Terrains.sml")
	}

	override def defaultNamespace: String = "Terrains"
}


trait Library[T] extends TEagerSingleton {
	protected val initialLoadComplete = new CountDownLatch(1)

	protected var byKind = Map[Taxon, T]()

	protected def topLevelField : String
	protected def createBlank() : T

	def withKind(kind : Taxon) = {
		initialLoadComplete.await()
		byKind.getOrElse(kind, createBlank())
	}

	def load(config : ConfigValue) : Unit

	final def load(smlPath : String) : Unit = load(ResourceManager.sml(smlPath))

	def initialLoad()

	Executor.submitAsync(() => {
		initialLoad()
		initialLoadComplete.countDown()
		Metrics.checkpoint(s"${this.getClass.getSimpleName} initial load complete")
	})
}


abstract class AuxDataLibrary[T <: TAuxData](clazz : Clazz[T]) extends Library[T] {

	def defaultNamespace : String

	def load(config : ConfigValue) : Unit = synchronized {
		for (topLevelConf <- config.fieldOpt(topLevelField)) {
			for ((kind, dataConf) <- topLevelConf.fields) {
				val auxData = createBlank().loadFromConfig(dataConf)
				for (kindField <- clazz.fields.get("kind")) {
					kindField.setValue(auxData, Taxonomy(kind, defaultNamespace))
				}
				byKind += Taxonomy(kind, defaultNamespace) -> auxData
			}
		}
	}
}


class EntityArchetype {
	private var _data = Map[Clazz[_ <: TAuxData], TAuxData]()

	def data[T <: TAuxData](clazz : Clazz[T]) : T = {
		dataOpt(clazz).get
	}

	def dataOpt[T <: TAuxData](clazz : Clazz[T]) : Option[T] = {
		_data.get(clazz).asInstanceOf[Option[T]]
	}

	def attachData(clazz : Clazz[_  <: TAuxData], data : TAuxData): Unit = {
		_data += clazz -> data
	}

	def createEntity(world : World) : Entity = {
		val ent = world.createEntity()
		world.startEvent(EntityCreated(ent))
		_data.foreach {
			case (clazz,data) => world.attachDataByClass(ent, CopyAssistant.copyShallow(data), clazz.runtimeClass)
		}
		world.endEvent(EntityCreated(ent))
		ent
	}
}

abstract class EntityArchetypeLibrary extends Library [EntityArchetype] {

	protected def fixedDataTypes : Seq[_ <: Clazz[_ <: TAuxData]]
	protected def conditionalDataTypes : Map[String, _ <: Clazz[_ <: TAuxData]]

	override protected def createBlank(): EntityArchetype = new EntityArchetype


	override def withKind(kind: Taxon): EntityArchetype = {
		initialLoadComplete.await()
		byKind(kind)
	}

	def defaultNamespace : String

	def load(config : ConfigValue) : Unit = synchronized {
		for (topLevelConf <- config.fieldOpt(topLevelField)) {
			for ((kind, dataConf) <- topLevelConf.fields) {
				val arch = createBlank()

				val ident = new IdentityData(Taxonomy(kind, defaultNamespace))
				arch.attachData(IdentityData, ident)

				val matchedConditionalTypes = conditionalDataTypes.filter {
					case (k,v) => dataConf.hasField(k)
				}.values

				for (clazz <- fixedDataTypes ++ matchedConditionalTypes) {
					val data = clazz.instantiate
					data.loadFromConfig(dataConf)
					arch.attachData(clazz, data)
				}

				byKind += Taxonomy(kind, defaultNamespace) -> arch
			}
		}
	}
}