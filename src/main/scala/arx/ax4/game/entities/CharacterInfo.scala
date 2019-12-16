package arx.ax4.game.entities

import arx.Prelude
import arx.application.Noto
import arx.ax4.game.action.{AttackIntent, DoNothingIntent, GameActionIntent, MoveIntent, SwitchSelectedCharacterIntent}
import arx.ax4.game.entities.Conditionals.BaseAttackConditional
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.vec.{ReadVec2f, ReadVec4f, Vec2f, Vec4f}
import arx.core.vec.coordinates.{AxialVec3, CartVec, CartVec3, HexDirection}
import arx.engine.data.Reduceable
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView
import arx.graphics.helpers.{Color, RGBA}

@GenerateCompanion
class CharacterInfo extends AxAuxData {
	var species : Taxon = Taxonomy("Specieses.Human")
	var sex : Taxon = if (Prelude.random.nextInt() % 2 == 0) { Taxonomy("female") } else { Taxonomy("male") }

	var health = Reduceable(6)
	var healthRecoveryRate = 1
	var alive = true
	var actionPoints = Reduceable(3)
	var moveSpeed = Sext.ofInt(3)
	var movePoints = Sext(0)

	var stamina = Reduceable(6)
	var staminaRecoveryRate = 1

	var bodyParts : Set[Taxon] = Set()

	var skillXP : Map[Taxon, Int] = Map()
	var skillLevels : Map[Taxon, Int] = Map()

	var strength : Sext = 0
	var dexterity : Sext = 0
	var intellect : Sext = 0
	var cunning : Sext = 0

	var activeAttack : Option[AttackReference] = None
	var activeIntent : GameActionIntent = DoNothingIntent
	var defaultIntent : GameActionIntent = DoNothingIntent
	var fallbackIntents : List[GameActionIntent] = List(MoveIntent, SwitchSelectedCharacterIntent)
}

@GenerateCompanion
class Physical extends AxAuxData {
	var position : AxialVec3 = AxialVec3.Zero
	var offset : CartVec = CartVec.Zero
	var colorTransforms : List[ColorTransform] = Nil
	var facing : HexDirection = HexDirection.Top
	var occupiesHex : Boolean = true
}

@GenerateCompanion
class CombatData extends AxAuxData {
	var attackModifier = AttackModifier()
	var conditionalAttackModifiers: List[(BaseAttackConditional, AttackModifier)] = List()
	var defenseModifier = DefenseModifier()
	var conditionalDefenseModifiers: List[(BaseAttackConditional, DefenseModifier)] = List()
	var specialAttacks = Map[AnyRef, SpecialAttack]()
}

case class AttackReference(weapon : Entity, attackKey : AttackKey, specialSource : Option[Entity], specialKey : AnyRef) {

	def resolve()(implicit view : WorldView) : Option[AttackData] = {
		val baseAttackData = view.data[Weapon](weapon).attacks.get(attackKey)
		val specialAttackModifier = specialSource.flatMap(ss => view.data[CombatData](ss).specialAttacks.get(specialKey)).map(sa => sa.attackModifier)
		specialAttackModifier match {
			case Some(mod) => baseAttackData.map(ba => {
				val bac = ba.copy();
				bac.merge(mod);
				bac
			})
			case None => baseAttackData
		}
	}
}




sealed trait ColorTransform {
	def apply(color : Color) : Color
}

case class HueShift(hue : Float, pcnt : Float) extends ColorTransform {
	override def apply(color: Color): Color = {
		color.asHSBA.hueShifted(hue, pcnt)
	}
}
case class Brightness(brightness : Float, pcnt : Float) extends ColorTransform {
	override def apply(color: Color): Color = {
		color.asHSBA.brightnessShifted(brightness, pcnt)
	}
}
case class ColorComponentMix(mult : RGBA, pcnt : Float) extends  ColorTransform {
	override def apply(color: Color): Color = {
		val start = color.asRGBA
		val end = mult
		RGBA(start + (end - start) * pcnt)
	}
}