package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.ReactionData
import arx.ax4.game.entities.ReactionType
import arx.ax4.game.event.SwitchReactionEvent
import arx.engine.entity.Entity
import arx.engine.world.World

object ReactionLogic {
	import arx.core.introspection.FieldOperations._


	def switchReactionType(entity : Entity, reactionType : ReactionType)(implicit world : World): Unit = {
		implicit val view = world.view

		val rdata = entity(ReactionData)
		val curReaction = rdata.currentReaction
		if (curReaction != reactionType) {
			world.startEvent(SwitchReactionEvent(entity, curReaction, reactionType))
			// disable all of the modifiers put in place by whatever the previous reaction was
			val previousModifiers = rdata.modifiersByType.getOrElse(curReaction, Nil)
			previousModifiers.foreach(m => world.toggleModification(m, enable = false))

			world.modify(entity, ReactionData.currentReaction -> reactionType)
			val newExistingModifiers = rdata.modifiersByType.getOrElse(reactionType, Nil)
			if (newExistingModifiers.nonEmpty) {
				newExistingModifiers.foreach(m => world.toggleModification(m, enable = true))
			} else {
				val newModifiers = reactionType.applyToEntity(world, entity)
				world.modify(entity, ReactionData.modifiersByType.put(reactionType, newModifiers))
			}

			world.endEvent(SwitchReactionEvent(entity, curReaction, reactionType))
		}
	}
}
