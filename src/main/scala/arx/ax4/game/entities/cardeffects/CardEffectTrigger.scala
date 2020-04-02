package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.EntityPredicate
import arx.ax4.game.entities.Conditionals.EntityConditional
import arx.ax4.game.entities.{CardPredicate, EntityConditionals}
import arx.ax4.game.event.CardEvents.CardPlayed
import arx.ax4.game.logic.{CardLocation, IdentityLogic}
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigDataLoader, CustomConfigDataLoader}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.event.GameEvent
import arx.engine.world.WorldView

trait CardEffectTrigger {
	def matches (entity : Entity, sourceCard : Entity, event : GameEvent)(implicit view : WorldView) : Boolean
}

object CardEffectTrigger extends CustomConfigDataLoader[CardEffectTrigger] {
	case class OnCardPlay(playedCardCondition : EntityConditional, sourceCardCondition : EntityConditional) extends CardEffectTrigger {
		override def matches (entity : Entity, sourceCard : Entity, event : GameEvent)(implicit view : WorldView) : Boolean = {
			event match {
				case CardPlayed(cardPlayer, card) if sourceCard != card && sourceCardCondition.isTrueFor(view, sourceCard) =>
					cardPlayer == entity && playedCardCondition.isTrueFor(view, card)
				case _ => false
			}
		}


	}

	case object Sentinel extends CardEffectTrigger {
		override def matches(entity: Entity, sourceCard : Entity, event: GameEvent)(implicit view: WorldView): Boolean = false
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[CardEffectTrigger]


//	private val onCardPlayPattern = "(?i)on\\s?Card\\s?Playe?d?\\(([a-zA-Z]*)\\s?,\\s?([a-zA-Z])\\)".r

	override def loadFrom(config: ConfigValue): Option[CardEffectTrigger] = {
		if (config.isStr) {
			config.str match {
				//				case onCardPlayPattern(playedConditional) =>
				//					if (typeFilterStr == "" || typeFilterStr.equalsIgnoreCase("all")) {
				//						OnCardPlay(EntityConditionals.any, EntityConditionals.any)
				//					} else {
				//						OnCardPlay(EntityConditionals.isA(Taxonomy(typeFilterStr, "CardTypes")))
				//					}
				case _ =>
					Noto.warn(s"Could not parse effect trigger str: $config")
					None
			}
		} else if (config.isObj) {
			config.fieldOpt("type") match {
				case Some(triggerType) =>
					triggerType.str.toLowerCase.replaceAll(" ","") match {
						case "oncardplay" => (ConfigDataLoader.loadFrom[EntityConditional](config.playedCardCondition), ConfigDataLoader.loadFrom[EntityConditional](config.sourceCardCondition)) match {
							case (Some(playedCardCondition), Some(sourceCardCondition)) => Some(OnCardPlay(playedCardCondition, sourceCardCondition))
							case _ => Noto.warn("Invalid subconditions for onCardPlay"); None
						}
					}
				case None =>
					Noto.warn(s"Could not parse effect trigger (no type): $config")
					None
			}
		} else {
			Noto.warn(s"Could not parse effect trigger: $config")
			None
		}
	}
}
