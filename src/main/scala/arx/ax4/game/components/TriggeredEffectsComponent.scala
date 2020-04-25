package arx.ax4.game.components

import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.CardSelector.SelfSelector
import arx.ax4.game.entities.Companions.CardData
import arx.ax4.game.entities.{CardSelector, DeckData}
import arx.core.units.UnitOfTime
import arx.engine.event.GameEvent
import arx.engine.game.components.GameComponent
import arx.engine.world.World

class TriggeredEffectsComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {}

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		onGameEventEnd {
			case ge : GameEvent =>
				for (entity <- view.entitiesWithData[DeckData]) {
					val deck = entity[DeckData]
					for (card <- deck.allCards) {
						val CD = card(CardData)
						for (effGroup <- CD.cardEffectGroups; triggeredEffect <- effGroup.triggeredEffects if triggeredEffect.trigger.matches(entity, card, ge)) {
							triggeredEffect.effect.instantiate(view, entity, card) match {
								case Left(inst) =>
									var sel = SelectionResult()
									inst.nextSelector(sel) match {
										case Some(selector @ CardSelector.SelfSelector(_)) => sel = sel.addResult(selector, card, 1)
										case Some(other) => Noto.warn("Effects that require non-self selection are not yet supported")
										case None => // good
									}

									if (inst.nextSelector(sel).isEmpty) {
										inst.applyEffect(world, sel)
									} else {
										Noto.warn(s"\tUnfulfilled selections in triggered effect $triggeredEffect, not applying")
									}

								case Right(msg) => Noto.info(s"Could not instantiate triggered effect, which is probably fine: $msg")
							}
						}
					}
				}
		}
	}
}
