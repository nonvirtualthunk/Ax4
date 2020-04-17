package arx.ax4.game.components

import arx.application.Noto
import arx.ax4.game.components.FlagComponent.{ChangeFlagBy, FlagBehavior}
import arx.ax4.game.entities.Companions.TagData
import arx.ax4.game.entities.{ConfigLoadableLibrary, DeckData, FlagLibrary, Library}
import arx.ax4.game.event.TurnEvents.EntityTurnEndEvent
import arx.ax4.game.event.{DamageEvent, DeflectEvent, DodgeEvent}
import arx.ax4.game.logic.TagLogic
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
	}
}


object FlagComponent {
	case class FlagBehavior(flag : Taxon, alteration: FlagAlteration, condition : PartialFunction[GameEvent,Entity]) {
		val liftedCondition = condition.lift
	}

	def resetAtEndOfTurn(flagStr : String) = FlagBehavior(flag(flagStr), ResetToZero, {
		case EntityTurnEndEvent(entity, _) => entity
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