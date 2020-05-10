package arx.ax4.application.scenarios

import arx.Prelude
import arx.application.Noto
import arx.ax4.control.components.{CardControl, PerkPickControl, SelectionControl, TacticalUIControl}
import arx.ax4.game.components.{AIComponent, CardCreationComponent, DeckComponent, FlagComponent, TriggeredEffectsComponent, TurnComponent}
import arx.ax4.game.entities.AttackEffectTrigger.Hit
import arx.ax4.game.entities.Companions.{CardData, CharacterInfo, DeckData, MonsterData, Physical, TagData, Tile, TurnData}
import arx.ax4.game.entities.EntityConditionals._
import arx.ax4.game.entities.GatherConditionals._
import arx.ax4.game.entities.{AffectsSelector, AllegianceData, AttackData, AttackKey, AxAuxData, CardData, CardLibrary, CardPredicate, CardSelector, CardTypes, CharacterInfo, CombatData, DamageElement, DamageKey, DamageType, DeckData, EntityConditionals, Equipment, FactionData, GatherConditionals, GatherMethod, Inventory, ItemLibrary, LockedCardSlot, LockedCardType, MonsterAttack, MonsterData, Physical, QualitiesData, ReactionData, Resource, ResourceKey, ResourceOrigin, ResourceSourceData, TargetPattern, Terrain, TerrainLibrary, Tile, Tiles, TriggeredAttackEffect, TurnData, Vegetation, VegetationLayer, VegetationLayerType, VegetationLibrary, Weapon, WeaponLibrary}
import arx.ax4.game.logic.{CardLogic, CharacterLogic, InventoryLogic, MovementLogic, SkillsLogic}
import arx.ax4.graphics.components.{AnimationGraphicsComponent, AnimationGraphicsRenderingComponent, EntityGraphics, TacticalUIGraphics, TileGraphics}
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
import arx.engine.control.event.{KeyPressEvent, Mouse}
import arx.engine.data.Reduceable
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, IdentityData, Taxonomy}
import arx.engine.event.WorldCreatedEvent
import arx.engine.game.GameEngine
import arx.engine.graphics.GraphicsEngine
import arx.engine.graphics.components.windowing.WindowingGraphicsComponent
import arx.engine.graphics.data.PovData
import arx.engine.world.{Universe, World, WorldQuery}
import arx.game.data.{DicePool, RandomizationWorldData}
import arx.graphics.helpers.RGBA
import arx.graphics.pov.{PixelCamera, TopDownCamera}
import org.lwjgl.glfw.GLFW
import arx.ax4.game.entities.UnitOfGameTimeFloat._
import arx.ax4.game.entities.cardeffects.{AddCardToDeck, AttackGameEffect, GainMovePoints, PayActionPoints, PayStamina}
import arx.ax4.game.event.EntityCreated
import arx.ax4.game.event.TurnEvents.TurnStartedEvent
import arx.ax4.game.logic.CardAdditionStyle.DrawPile
import arx.core.gen.SimplexNoise
import arx.resource.ResourceManager

import scala.io.StdIn

object SimpleMapScenario extends Scenario {
	import arx.core.introspection.FieldOperations._

	var playerCharacter : Entity = _
	var player : Entity = _

	override def gameWorld(universe: Universe): World = {
		val world = new World
		world.registerSubtypesOf[AxAuxData]()
		Metrics.checkpoint("SimpleMap world subtypes registered")

		world.attachWorldData(new RandomizationWorldData)

		world
	}

	/**
	 * Called after game engine initialized
	 */
	override def setupInitialGameState(world: World): Unit = {
		var created = 0
		for (r <- 0 until 8) {
			for (apos <- HexRingIterator(AxialVec.Zero, r)) {
				val ent = world.createEntity(Tiles.tileEntityId(apos.q, apos.r))
				world.attachDataWith[Tile](ent, t => t.position = AxialVec3(apos, 0))


				val terrainKind = if (r <= 1) {
					Taxonomy("Mountains", "Terrains")
				} else if (r <= 4) {
					Taxonomy("Hills", "Terrains")
				} else {
					Taxonomy("Flatland", "Terrains")
				}
				val terrain = TerrainLibrary.withKind(terrainKind)
				world.attachData(ent, terrain)


				val veg = new Vegetation
				if (apos.q > apos.r && r > 1) {
					veg.layers += VegetationLayerType.GroundCover -> VegetationLibrary.withKind(Taxonomy("grass", "Vegetations"))
				}
				val cpos = apos.asCartesian


				val terrainForestThreshold = if (terrainKind == Taxonomy("mountains", "Terrains")) {
					0.4f
				} else if (terrainKind == Taxonomy("hills", "Terrains")) {
					0.3f
				} else {
					0.2f
				}
				val forestThreshold = if (veg.layers.nonEmpty) {
					terrainForestThreshold
				} else {
					terrainForestThreshold + 0.35f
				}


				if (SimplexNoise.noise(cpos.x * 0.3f, cpos.y * 0.3f) > forestThreshold) {
					veg.layers += VegetationLayerType.Canopy -> VegetationLibrary.withKind(Taxonomy("forest", "Vegetations"))
				}
				world.attachData(ent, veg)



				val resourceData = new ResourceSourceData
				for ((vlt, l) <- veg.layers) {
					if (l.kind == Taxonomy("grass", "Vegetations")) {
						resourceData.resources += ResourceKey(ResourceOrigin.Vegetation(vlt), Taxonomy("hay bushel", "Items")) ->
							Resource(Taxonomy("hay bushel", "Items"), Reduceable(3), 1, 1.gameYear, canRecoverFromZero = true, structural = false,
								Vector(GatherMethod(name = "Gather Hay", amount = 1, gatherFlags = Set(Taxonomy("harvester")))))
					}
				}
				world.attachData(ent, resourceData)

				created += 1
			}
		}
		Metrics.checkpoint("SimpleMap hexes created")

		player = world.createEntity()
		world.attachDataWith[FactionData](player, fd => {
			fd.playerControlled = true
			fd.color = RGBA(0.8f,0.1f,0.2f,1.0f)
		})

		val enemy = world.createEntity()
		world.attachDataWith[FactionData](enemy, fd => {
			fd.playerControlled = false
			fd.color = RGBA(0.1f,0.1f,0.75f,1.0f)
		})




		val torbold = CharacterLogic.createCharacter(player)(world)
		MovementLogic.placeCharacterAt(torbold, AxialVec3(1,-1,0))(world)
		//		world.modify(Tiles.tileAt(0,0), Tile.entities + torbold, None)
		world.modify(torbold, IdentityData.name -> Some("Torbold"), None)

		world.modify(torbold, TagData.flags.put(Taxonomy("flags.flashingPoints"), 1))
		world.modify(torbold, TagData.flags.put(Taxonomy("flags.slow"), 1))
		world.modify(torbold, TagData.flags.put(Taxonomy("flags.parry"), 3))

		playerCharacter = torbold

		def addCardTorbold(cardName : String): Unit = {
			val card = CardLibrary.withKind(Taxonomy(cardName, "CardTypes")).createEntity(world)
			CardLogic.addCards(torbold, Vector(card), DrawPile)(world)
		}

		addCardTorbold("HedgehogStance")

		val moveCard = torbold(DeckData)(world.view).allCards.find(c => c(IdentityData)(world.view).isA(CardTypes.MoveCard)).get

		CardLogic.addLockedCardSlot(torbold, LockedCardSlot(Seq(CardPredicate.IsCard), "Any Card"))(world)
		CardLogic.addLockedCardSlot(torbold, LockedCardSlot(Seq(CardPredicate.IsCard), "Any Card"))(world)

		CardLogic.setLockedCard(torbold, 0, LockedCardType.SpecificCard(moveCard))(world)
		CardLogic.setLockedCard(torbold, 1, LockedCardType.MetaAttackCard(AttackKey.Primary, None))(world)

		val longspearArch = WeaponLibrary.withKind(Taxonomy("longspear", "Items.Weapons"))
		val longspear = longspearArch.createEntity(world)
//		InventoryLogic.equip(torbold, longspear)(world)
		InventoryLogic.transferItem(longspear, to = Some(torbold))(world)

		val scytheArch = WeaponLibrary.withKind(Taxonomy("scythe", "Items.Weapons"))
		val scythe = scytheArch.createEntity(world)
		//		InventoryLogic.equip(torbold, longspear)(world)
		InventoryLogic.transferItem(scythe, to = Some(torbold))(world)


		val slime = CharacterLogic.createCharacter(enemy)(world)
		world.modify(slime, CharacterInfo.species setTo Taxonomy("slime"), None)
		world.modify(slime, Physical.position -> AxialVec3(2,0,0), None)
		world.modify(slime, IdentityData.name -> Some("Slime"), None)
		world.modify(Tiles.tileAt(2,0), Tile.entities + slime, None)
//		world.modify(slime, CharacterInfo.innateCards append Vector(CardLogic.createCard(slime, CardLibrary.withKind(Taxonomy("SlimeSmash")))(world)))
		world.attachData(slime, new MonsterData)
		world.modify(slime, MonsterData.monsterAttacks append MonsterAttack(AttackData(
			weapon = slime,
			name = "Slime Smash",
			attackType = Taxonomy("PhysicalAttack"),
			damage = Vector(DamageElement(DamageKey.Primary, DicePool(1).d(4), 0, 1.0f, DamageType.Bludgeoning)),
			triggeredEffects = Vector(TriggeredAttackEffect(Hit ,AffectsSelector.Target, AddCardToDeck(Seq(Taxonomy("CardTypes.Slime")), DrawPile)))
		)))

		world.attachWorldData(new TurnData)
		world.modifyWorld(TurnData.activeFaction -> player, None)

		world.addEvent(TurnStartedEvent(player, 0))

		SkillsLogic.gainSkillXP(playerCharacter, Taxonomy("spearSkill"),20)(world)
//
		Mouse.setImage(ResourceManager.image("third-party/shikashiModified/staff1.png"), Vec2i(4,4))
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
		gameEngine.register[DeckComponent]
		gameEngine.register[CardCreationComponent]
		gameEngine.register[AIComponent]
		gameEngine.register[FlagComponent]
		gameEngine.register[TriggeredEffectsComponent]
	}

	override def registerGraphicsComponents(graphicsEngine: GraphicsEngine, universe: Universe): Unit = {
		graphicsEngine.register[WindowingGraphicsComponent]
		graphicsEngine.register[AnimationGraphicsComponent]
		graphicsEngine.register[AnimationGraphicsRenderingComponent]
		graphicsEngine.register[TileGraphics]
		graphicsEngine.register[EntityGraphics]
		graphicsEngine.register[TacticalUIGraphics]

	}

	override def registerControlComponents(controlEngine: ControlEngine, universe: Universe): Unit = {
		controlEngine.register[WindowingControlComponent]
		controlEngine.register[QueryControlComponent]
		controlEngine.register[TacticalUIControl]
//		controlEngine.register[TacticalUIActionControl]
		controlEngine.register[CardControl]
		controlEngine.register[SelectionControl]
		controlEngine.register[PerkPickControl]
	}

	override def serialGraphicsEngine(universe: Universe): Boolean = true
	override def serialControlEngine(universe: Universe): Boolean = true
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
