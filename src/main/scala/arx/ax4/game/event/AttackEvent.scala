package arx.ax4.game.event

import arx.ax4.game.entities.{AttackData, DamageType, DefenseData}
import arx.engine.entity.Entity
import arx.engine.event.GameEvent

case class AttackEvent(attackInfo : AttackEventInfo) extends GameEvent

case class StrikeEvent(attackInfo : AttackEventInfo) extends GameEvent

case class SubStrike(target : Entity, attackInfo : AttackEventInfo, defenseData : DefenseData) extends GameEvent

case class DamageEvent(entity : Entity, damage : Int, damageType : DamageType) extends GameEvent

case class DodgeEvent(entity : Entity) extends GameEvent

case class DeflectEvent(entity : Entity, originalDamage : Int) extends GameEvent



case class AttackEventInfo(attacker : Entity, weapon : Entity, allTargets : List[Entity], attackData : AttackData)