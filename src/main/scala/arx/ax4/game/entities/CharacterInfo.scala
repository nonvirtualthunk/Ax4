package arx.ax4.game.entities

import arx.Prelude
import arx.ax4.game.action.{GameActionIntent, MoveIntent}
import arx.ax4.game.entities.Conditionals.AttackConditional
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.vec.{ReadVec2f, Vec2f}
import arx.core.vec.coordinates.{AxialVec3, CartVec3, HexDirection}
import arx.engine.data.Reduceable
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView

@GenerateCompanion
class CharacterInfo extends AxAuxData {
	var species : Taxon = Species.Human
	var sex : Taxon = if (Prelude.random.nextInt() % 2 == 0) { Taxonomy("female") } else { Taxonomy("male") }

	var health = Reduceable(6)
	var healthRecoveryRate = 1
	var alive = true
	var actionPoints = Reduceable(3)
	var moveSpeed = Sext.ofInt(3)
	var movePoints = Sext(0)

	var bodyParts : Set[BodyPart] = Set()

	var strength : Sext = 0
	var dexterity : Sext = 0
	var intellect : Sext = 0
	var cunning : Sext = 0

	var activeAttack : Option[AttackReference] = None
	var activeIntent : GameActionIntent = MoveIntent

	def maxPossibleMovePoints = actionPoints.maxValue * moveSpeed
	def curPossibleMovePoints = movePoints + actionPoints.currentValue * moveSpeed
}

@GenerateCompanion
class Physical extends AxAuxData {
	var position : AxialVec3 = AxialVec3.Zero
	var offset : ReadVec2f = Vec2f.Zero
	var facing : HexDirection = HexDirection.Top
	var occupiesHex : Boolean = true
}

@GenerateCompanion
class CombatData extends AxAuxData {
	var attackModifier = AttackModifier()
	var conditionalAttackModifiers: List[(AttackConditional, AttackModifier)] = List()
	var defenseModifier = DefenseModifier()
	var conditionalDefenseModifiers: List[(AttackConditional, DefenseModifier)] = List()
	var specialAttacks = Map[AnyRef, SpecialAttack]()
}

class BodyPart(nomen_ : String, parents_ : Taxon*) extends Taxon(nomen_, parents_.toList)
object BodyPart {
	case object BaseBodyPart extends BodyPart("body part")
	case object Gripping extends BodyPart("gripping body part", BaseBodyPart)
	case object Thinking extends BodyPart("thinking body part", BaseBodyPart)
	case object Appendage extends BodyPart("appendage", BaseBodyPart)
	case object DextrousAppendage extends BodyPart("dextrous appendage", Appendage)

	case object Hand extends BodyPart("hand", Gripping)
	case object Pseudopod extends BodyPart("pseudopod", Gripping, DextrousAppendage)
	case object Arm extends BodyPart("arm", DextrousAppendage)
	case object Leg extends BodyPart("leg", Appendage)
	case object Head extends BodyPart("head", Thinking)
}

case class AttackReference(weapon : Entity, attackKey : AnyRef, specialSource : Option[Entity], specialKey : AnyRef) {

	def resolve()(implicit view : WorldView) : Option[AttackData] = {
		val baseAttackData = view.data[Weapon](weapon).attacks.get(attackKey)
		val specialAttackModifier = specialSource.flatMap(ss => view.data[CombatData](ss).specialAttacks.get(specialKey)).map(sa => sa.attackModifier)
		specialAttackModifier match {
			case Some(mod) => baseAttackData.map(ba => { val bac = ba.copy(); bac.merge(mod); bac })
			case None => baseAttackData
		}
	}
}




