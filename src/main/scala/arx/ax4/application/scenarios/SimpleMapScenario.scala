package arx.ax4.application.scenarios

import arx.Prelude
import arx.application.Noto
import arx.ax4.control.components.{TacticalUIActionControl, TacticalUIControl}
import arx.ax4.game.components.TurnComponent
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile, TurnData}
import arx.ax4.game.entities.{AllegianceData, AxAuxData, CharacterInfo, CombatData, Equipment, FactionData, Inventory, Physical, Terrain, TerrainLibrary, Tile, Tiles, TurnData, Vegetation, VegetationLayer, VegetationLibrary, WeaponLibrary}
import arx.ax4.game.logic.InventoryLogic
import arx.ax4.graphics.components.{AnimationGraphicsComponent, EntityGraphics, TacticalUIGraphics, TileGraphics}
import arx.ax4.graphics.data.{AxGraphicsData, TacticalUIData}
import arx.core.async.Executor
import arx.core.metrics.Metrics
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2i}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, HexRingIterator}
import arx.engine.Scenario
import arx.engine.control.ControlEngine
import arx.engine.control.components.ControlComponent
import arx.engine.control.components.windowing.WindowingControlComponent
import arx.engine.control.event.KeyPressEvent
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, IdentityData, Taxonomy}
import arx.engine.event.WorldCreatedEvent
import arx.engine.game.GameEngine
import arx.engine.graphics.GraphicsEngine
import arx.engine.graphics.components.windowing.WindowingGraphicsComponent
import arx.engine.graphics.data.PovData
import arx.engine.world.{Universe, World, WorldQuery}
import arx.game.data.RandomizationWorldData
import arx.graphics.helpers.RGBA
import arx.graphics.pov.{PixelCamera, TopDownCamera}
import org.lwjgl.glfw.GLFW

import scala.io.StdIn

object SimpleMapScenario extends Scenario {
	import arx.core.introspection.FieldOperations._

	var playerCharacter : Entity = _

	override def gameWorld(universe: Universe): World = {
		val world = new World
		world.registerSubtypesOf[AxAuxData]()
		Metrics.checkpoint("SimpleMap world subtypes registered")

		var created = 0
		for (r <- 0 until 6) {
			for (apos <- HexRingIterator(AxialVec.Zero, r)) {
				val ent = world.createEntity(Tiles.tileEntityId(apos.q, apos.r))
				world.attachDataWith[Tile](ent, t => t.position = AxialVec3(apos, 0))
				val veg = new Vegetation
				if (apos.q > apos.r && r > 1) {
					veg.layers = Vector(VegetationLibrary.withKind(Taxonomy("grass")))
				}
				world.attachData(ent, veg)
				val terrainKind = if (r <= 1) {
					Taxonomy("Mountains")
				} else if (r <= 4) {
					Taxonomy("Hills")
				} else {
					Taxonomy("Flatland")
				}
				val terrain = TerrainLibrary.withKind(terrainKind)
				world.attachData(ent, terrain)
				created += 1
			}
		}
		Metrics.checkpoint("SimpleMap hexes created")

		val player = world.createEntity()
		world.attachDataWith[FactionData](player, fd => {
			fd.playerControlled = true
			fd.color = RGBA(0.8f,0.1f,0.2f,1.0f)
		})

		val enemy = world.createEntity()
		world.attachDataWith[FactionData](enemy, fd => {
			fd.playerControlled = false
			fd.color = RGBA(0.1f,0.1f,0.75f,1.0f)
		})


		val torbold = createCreature(world, player)
		world.modify(Tiles.tileAt(0,0), Tile.entities + torbold, None)
		world.modify(torbold, IdentityData.name -> Some("Torbold"), None)

		playerCharacter = torbold

		val longspearArch = WeaponLibrary.withKind(Taxonomy("longspear"))
		val longspear = longspearArch.createEntity(world)

		InventoryLogic.equip(torbold, longspear)(world)

		val slime = createCreature(world, enemy)
		world.modify(slime, CharacterInfo.species setTo Taxonomy("slime"), None)
		world.modify(slime, Physical.position -> AxialVec3(2,0,0), None)
		world.modify(slime, IdentityData.name -> Some("Slime"), None)
		world.modify(Tiles.tileAt(2,0), Tile.entities + slime, None)

		world.attachWorldData(new TurnData)
		world.modifyWorld(TurnData.activeFaction -> player, None)

		world.attachWorldData(new RandomizationWorldData)

		world
	}

	def createCreature(world : World, faction : Entity) = {
		val creature = world.createEntity()
		world.attachData(creature, new CharacterInfo)
		world.attachData(creature, new Physical)
		world.attachData(creature, new IdentityData)
		world.attachData(creature, new Equipment)
		world.attachData(creature, new Inventory)
		world.attachData(creature, new CombatData)
		world.attachDataWith(creature, (ad : AllegianceData) => {
			ad.faction = faction
		})
		creature
	}

	override def displayWorld(universe: Universe): World = {
		val world = new World
		world.registerSubtypesOf[AxGraphicsData]()

		world[PovData].pov = new PixelCamera(512, 0.1f)

		world
	}

	override def realtime(universe: Universe): Boolean = false

	override def registerGameComponents(gameEngine: GameEngine, universe: Universe): Unit = {
		gameEngine.register[TurnComponent]
	}

	override def registerGraphicsComponents(graphicsEngine: GraphicsEngine, universe: Universe): Unit = {
		graphicsEngine.register[WindowingGraphicsComponent]
		graphicsEngine.register[AnimationGraphicsComponent]
		graphicsEngine.register[TileGraphics]
		graphicsEngine.register[EntityGraphics]
		graphicsEngine.register[TacticalUIGraphics]

	}

	override def registerControlComponents(controlEngine: ControlEngine, universe: Universe): Unit = {
		controlEngine.register[WindowingControlComponent]
		controlEngine.register[QueryControlComponent]
		controlEngine.register[TacticalUIControl]
		controlEngine.register[TacticalUIActionControl]
	}

	override def serialGraphicsEngine(universe: Universe): Boolean = true
}

class QueryControlComponent extends ControlComponent {
	override protected def onUpdate(game: World, graphics: World, dt: UnitOfTime): Unit = {}
	override protected def onInitialize(game: World, display: World): Unit = {
		onControlEvent {
			case KeyPressEvent(GLFW.GLFW_KEY_F4, _, _) =>
				var breaker = false
				Executor.submitAsync[Unit](() => {
					while (!breaker) {
						print("query: ")
						val line = StdIn.readLine()
						if (line == null || line.isEmpty) {
							breaker = true
							println("ending query repl")
						} else {
							println(WorldQuery.run(line)(game.view))
						}
					}
				})
		}
	}
}
