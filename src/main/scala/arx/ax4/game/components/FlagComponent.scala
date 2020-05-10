package arx.ax4.game.components

import arx.application.Noto
import arx.ax4.game.components.FlagComponent.{ChangeFlagBy, FlagBehavior}
import arx.ax4.game.entities.Companions.{Physical, TagData}
import arx.ax4.game.entities.{ConfigLoadableLibrary, DeckData, FlagLibrary, Library, Tiles}
import arx.ax4.game.event.TurnEvents.{EntityTurnEndEvent, EntityTurnStartEvent}
import arx.ax4.game.event.{DamageEvent, DeflectEvent, DodgeEvent, EntityMoved}
import arx.ax4.game.logic.{AllegianceLogic, CharacterLogic, CombatLogic, MapLogic, MovementLogic, TagLogic}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.World
import arx.core.introspection.FieldOperations._
import arx.core.introspection.ReflectionAssistant
import arx.core.representation.ConfigValue
import arx.core.units.UnitOfTime
import arx.engine.event.GameEvent
import arx.engine.game.components.GameComponent
import arx.engine.world.EventState.Ended

class FlagComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(world: World): Unit = {
		implicit val impWorld = world
		implicit val view = world.view

		onGameEvent {
			case ge : GameEvent =>
				for (behavior <- FlagComponent.flagBehaviors) {
					behavior.liftedCondition(ge) match {
						case Some(baseEntity) =>
							// operate on all cards of a deck when the deck owner is processed
							for (entity <- Vector(baseEntity) ++ baseEntity.dataOpt[DeckData].map(_.allCards).getOrElse(Vector())) {
								behavior.alteration.changeFlag(entity, behavior.flag)(world)
							}
						case None => // do nothing
					}
				}
		}

		onGameEventEnd {
			case EntityMoved(entity, from, to) =>
				for (enemy <- AllegianceLogic.enemiesOf(entity)) {
					val zoc = TagLogic.flagValue(enemy, Taxonomy("Flags.ZoneOfControlRange"))
					val enemyPos = CharacterLogic.position(enemy)

					val fromDist = enemyPos.distance(from)
					val toDist = enemyPos.distance(to)

					// if it's moving from a non-zoc hex to a zoc hex
					if (toDist < zoc && fromDist >= zoc) {
						MovementLogic.forceEndOfMovement(entity)
					}

					val approachAttacks = TagLogic.flagValue(enemy, Taxonomy("Flags.OnApproachAttack"))
					if (approachAttacks > 0) {
						val (_, primAttack) = CombatLogic.defaultAttack(enemy)

						val enemyPos = CharacterLogic.position(enemy)
						val couldAttackPrev = CombatLogic.couldAttackBeMadeFromLocation(enemy, enemyPos, from, Left(entity), primAttack)
						val canAttackNow = CombatLogic.couldAttackBeMadeFromLocation(enemy, enemyPos, to, Left(entity), primAttack)
						if (!couldAttackPrev && canAttackNow) {
							CombatLogic.attack(world, enemy, Vector(entity), primAttack)
						}
					}
				}


				for (adjEntity <- to.neighbors.flatMap(adj => MapLogic.characterOnTile(adj))) {
					if (AllegianceLogic.areEnemies(entity, adjEntity)) {
						if (TagLogic.flagValue(adjEntity, Taxonomy("Flags.ZoneOfControlRange")) > 0) {

						}
					}
				}


		}
	}
}


object FlagComponent {
	case class FlagBehavior(flag : Taxon, alteration: FlagAlteration, condition : PartialFunction[GameEvent,Entity]) {
		val liftedCondition = condition.lift
	}

	def resetAtEndOfTurn(flagStr : String) = FlagBehavior(flag(flagStr), ResetToZero, {
		case EntityTurnEndEvent(entity, _) => entity
	})

	def resetAtStartOfTurn(flagStr : String) = FlagBehavior(flag(flagStr), ResetToZero, {
		case EntityTurnStartEvent(entity, _) => entity
	})

	trait FlagAlteration {
		def changeFlag(entity : Entity, flag : Taxon)(implicit world : World)
	}

	case class ChangeFlagBy(value : Int, limitToZero : Boolean) extends FlagAlteration {
		override def changeFlag(entity: Entity, flag: Taxon)(implicit world: World): Unit = {
			TagLogic.changeFlagBy(entity, flag, value, limitToZero)
		}
	}

	case object ResetToZero extends FlagAlteration {
		override def changeFlag(entity: Entity, flag: Taxon)(implicit world: World): Unit = {
			TagLogic.changeFlagTo(entity, flag, 0)
		}
	}



	def flag(str : String) = Taxonomy(str, "Flags")

	lazy val flagBehaviors = FlagLibrary.all.values.flatMap(_.simpleBehaviors)
}