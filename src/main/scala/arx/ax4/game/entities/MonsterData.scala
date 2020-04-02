package arx.ax4.game.entities

import arx.core.NoAutoLoad
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
import arx.engine.entity.Entity

@GenerateCompanion
class MonsterData extends AxAuxData {
	@NoAutoLoad var monsterAttacks : Vector[MonsterAttack] = Vector()

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (attackConf <- config.fieldAsList("monsterAttacks")) {
			val ad = AttackData(Entity.Sentinel)
			ad.loadFromConfig(attackConf.attack)
			monsterAttacks :+= MonsterAttack(ad)
		}
	}
}

case class MonsterAttack (attack : AttackData)
