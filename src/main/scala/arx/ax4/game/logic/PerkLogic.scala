package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.entities.{PendingPerkPicks, PerkSource, PerksLibrary}
import arx.ax4.game.event.GainPerkEvent
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.World
import arx.core.introspection.FieldOperations._

object PerkLogic {
	def takePerk(character : Entity, perk : Taxon, source : PerkSource)(implicit world : World): Unit = {
		implicit val view = world.view

		PerksLibrary.getWithKind(perk) match {
			case Some(perkInfo) =>
				var found = false
				var newPicks = Vector[PendingPerkPicks]()
				for (pick <- character(CharacterInfo).pendingPerkPicks) {
					if (found) {
						newPicks :+= pick
					} else {
						if (pick.possiblePerks.contains(perk)) {
							found = true
						} else {
							newPicks :+= pick
						}
					}
				}
				world.startEvent(GainPerkEvent(character, perk, source))
				world.modify(character, CharacterInfo.pendingPerkPicks -> newPicks)
				for (effect <- perkInfo.effects) {
					effect.instantiate(view, character, character) match {
						case Left(effectInst) =>
							// TODO: Make it possible to make selections for this effect as one would
							// a card effect
							effectInst.applyEffect(world, SelectionResult())
						case Right(msg) =>
							Noto.warn(s"Could not gain perk because : $msg")
					}
				}
				world.endEvent(GainPerkEvent(character, perk, source))
			case None =>
				Noto.warn(s"Cannot gain unknown perk $perk")
		}

	}
}
