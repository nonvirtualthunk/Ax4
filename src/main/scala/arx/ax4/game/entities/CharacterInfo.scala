package arx.ax4.game.entities

import arx.Prelude
import arx.application.Noto
import arx.ax4.game.entities.Conditionals.BaseAttackConditional
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.vec.{ReadVec2f, ReadVec4f, Vec2f, Vec4f}
import arx.core.vec.coordinates.{AxialVec3, CartVec, CartVec3, HexDirection}
import arx.engine.data.Reduceable
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView
import arx.graphics.helpers.{Color, RGBA, RichText, RichTextRenderSettings, THasRichTextRepresentation, TaxonSections, TextSection}

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

	var innateCards : Vector[Entity] = Vector()

	var perks : Vector[Taxon] = Vector()
	var pendingPerkPicks : Vector[PendingPerkPicks] = Vector()
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


case class PendingPerkPicks(possiblePerks : Vector[Perk], source : PerkSource)

sealed trait PerkSource extends THasRichTextRepresentation
object PerkSource {
	import arx.Prelude._

	case class SkillLevelUp(skill : Taxon, level : Int) extends PerkSource {
		override def toString: String = skill.name.fromCamelCase.capitalizeAll + " Up"

		override def toRichText(settings: RichTextRenderSettings): RichText = {
			RichText(
				TextSection(skill.displayName) :: TaxonSections(Taxonomy("SkillLevelUp"), settings)
			)
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