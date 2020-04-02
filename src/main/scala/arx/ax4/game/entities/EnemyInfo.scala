package arx.ax4.game.entities

import arx.core.macros.GenerateCompanion
import arx.engine.entity.{Entity, Taxon}

@GenerateCompanion
class EnemyInfo extends AxAuxData {
	var intent : Intent = Intent.Move

	var nextCardPlays : Vector[Entity] = Vector()

}


trait Intent
object Intent {
	case class AttackIntent(amount : Int, damageType: Taxon) extends Intent
	case object Debuff extends Intent
	case object Buff extends Intent
	case object Move extends Intent
}