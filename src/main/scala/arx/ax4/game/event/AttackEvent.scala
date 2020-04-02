package arx.ax4.game.event

import arx.ax4.game.entities.{AttackData, DamageType, DefenseData, GatherProspect, PerkSource, ReactionType, Resource}
import arx.engine.entity.{Entity, Taxon}
import arx.engine.event.GameEvent

case class AttackEvent(attackInfo : AttackEventInfo) extends GameEvent

case class StrikeEvent(attackInfo : AttackEventInfo) extends GameEvent

case class SubStrike(target : Entity, attackInfo : AttackEventInfo, defenseData : DefenseData) extends GameEvent

case class ArmorUsedEvent(entity : Entity, damage : Int, reducedBy : Int, damageType : Taxon) extends GameEvent

case class DamageEvent(entity : Entity, damage : Int, damageType : Taxon) extends GameEvent

case class DodgeEvent(entity : Entity) extends GameEvent

case class DeflectEvent(entity : Entity, originalDamage : Int, damageType : Taxon) extends GameEvent



case class AttackEventInfo(attacker : Entity, weapon : Entity, allTargets : Seq[Entity], attackData : AttackData)


case class EquipItem(entity : Entity, item : Entity) extends GameEvent

case class UnequipItem(entity : Entity, item : Entity) extends GameEvent

case class TransferItem(item : Entity, from : Option[Entity], to : Option[Entity]) extends  GameEvent



case class GainSkillXPEvent(entity : Entity, skill : Taxon, amount : Int) extends GameEvent

case class GainSkillLevelEvent(entity : Entity, skill : Taxon, newLevel : Int) extends GameEvent

case class NewPerkPicksAvailable(entity : Entity, perks : Seq[Taxon]) extends GameEvent

case class SwitchReactionEvent(entity : Entity, from : ReactionType, to : ReactionType) extends GameEvent

case class GatherEvent(prospect : GatherProspect) extends GameEvent

case class ResourceGatheredEvent(entity : Entity, kind : Taxon, gained : Int) extends GameEvent


case class GainPerkEvent(entity : Entity, perk : Taxon, source : PerkSource) extends GameEvent

case class ChangeFlagEvent(entity : Entity, flag : Taxon, oldValue : Int, newValue : Int) extends GameEvent {
	def delta = newValue - oldValue
}