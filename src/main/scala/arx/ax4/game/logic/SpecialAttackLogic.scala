package arx.ax4.game.logic

import arx.Prelude.toRicherIterable
import arx.application.Noto
import arx.ax4.game.entities.cardeffects.AttackGameEffect
import arx.ax4.game.entities.{ApCostDeltaModifier, AttackData, AttackGameEffectModifier, CardData, DeckData, GameEffectModifier, SpecialAttack, StaminaCostDeltaModifier, UntargetedAttackProspect}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object SpecialAttackLogic {



	def resolveAttachedSpecialAttackCard(entity : Entity, specialAttack : SpecialAttack)(implicit view : WorldView) : Option[Entity] = {
		entity.dataOpt[DeckData].flatMap(dd => {
			dd.allAvailableCards.find(card => matchingAttackData(entity, card, specialAttack).isDefined)
		})
	}

	def resolveSpecialAttackData(entity : Entity, baseAttackCard : Entity, specialAttack: SpecialAttack)(implicit view: WorldView) : AttackData = {
		matchingAttackData(entity, baseAttackCard, specialAttack) match {
			case Some(baseAttackData) => baseAttackData.mergedWith(specialAttack.attackModifier)
			case None =>
				Noto.warn("Could not resolve special attack data, falling back to modifying a sentinel")
				AttackData(entity).mergedWith(specialAttack.attackModifier)
		}
	}

	private def matchingAttackData(attacker : Entity, card : Entity, specialAttack: SpecialAttack)(implicit view : WorldView) : Option[AttackData] = {
		card[CardData].effects.collectFirst {
			case AttackGameEffect(_, attackData) if specialAttack.condition.isTrueFor(view, UntargetedAttackProspect(attacker, attackData)) => attackData
		}
	}


	def specialAttackToEffectModifiers(specialAttack : SpecialAttack) : Vector[GameEffectModifier] = {
		var ret = Vector[GameEffectModifier]()
		val attackMod = specialAttack.attackModifier
		if (attackMod.actionCostMinimum.isDefined || attackMod.actionCostDelta != 0) {
			ret :+= ApCostDeltaModifier(attackMod.actionCostDelta, attackMod.actionCostMinimum)
		}
		if (attackMod.staminaCostMinimum.isDefined || attackMod.staminaCostDelta != 0) {
			ret :+= StaminaCostDeltaModifier(attackMod.staminaCostDelta, attackMod.staminaCostMinimum)
		}
		ret :+= AttackGameEffectModifier(attackMod)
		ret
	}
}
