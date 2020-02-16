package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.Companions.{CombatData, Skill}
import arx.ax4.game.entities.cardeffects.{GameEffect, GameEffectConfigLoader}
import arx.core.NoAutoLoad
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigLoadable, TAuxData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{Modifier, World}
import arx.graphics.TToImage
import arx.resource.ResourceManager

import scala.reflect.ClassTag


case class Perk(kind : Taxon,
					 name : String,
					 description : String,
					 effects : Seq[GameEffect],
					 icon : Option[TToImage]) extends ConfigLoadable
object Perk {
	val Sentinel = Perk(Taxonomy("UnknownPerk"), "Sentinel", "sentinel", Nil, None)
}

class SkillLevelUpPerk extends ConfigLoadable {
	var perk : Taxon = Taxonomy.UnknownThing
	var minLevel : Int = 0
	var maxLevel : Int = 100
	@NoAutoLoad
	var requirements : List[Conditional[Entity]] = Nil

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (levelRange <- config.fieldOpt("levelRange")) {
			levelRange.str match {
				case SkillLevelUpPerk.levelRangePattern(lower, upper) =>
					minLevel = lower.toInt
					maxLevel = upper.toInt
				case s => Noto.warn(s"Invalid level range : $s")
			}
		}

		requirements = config.fieldAsList("requires").map(c => EntityConditionals.fromConfig(c))

	}
}
object SkillLevelUpPerk {
	val levelRangePattern = "([0-9]+)-([0-9]+)".r

	def apply(perk : Perk, minLevel : Int, maxLevel : Int, requirements : List[Conditional[Entity]]) : SkillLevelUpPerk = {
		val skill = new SkillLevelUpPerk
		skill.perk = perk.kind
		skill.minLevel = minLevel
		skill.maxLevel = maxLevel
		skill.requirements = requirements
		skill
	}
}

@GenerateCompanion
class Skill extends ConfigLoadable {
	var displayName : String = "Unnamed Skill"
	@NoAutoLoad
	var levelUpPerks: Vector[SkillLevelUpPerk] = Vector()

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (perksConf <- config.fieldOpt("levelUpPerks"); (k,v) <- perksConf.fields) {
			val perk = new SkillLevelUpPerk
			perk.perk = Taxonomy(k, "perks")
			perk.loadFromConfig(v)
			levelUpPerks :+= perk
		}
	}
}


object PerksLibrary extends Library[Perk] {
	override protected def topLevelField: String = "Perks"

	override protected def createBlank(): Perk = {
		Perk(Taxonomy("perk"), "default perk", "default description", Nil, None)
	}

	override def load(config: ConfigValue): Unit = {
		for (topLevel <- config.fieldOpt("Perks"); (perkName, perkConf) <- topLevel.fields) {
			val kind = Taxonomy(perkName, "perks")
			val effects = (perkConf.fieldAsList("effect") ::: perkConf.fieldAsList("effects")).map(GameEffectConfigLoader.loadFrom)
			val perk = Perk(
				kind,
				perkConf.name.strOrElse("Unknown name perk"),
				perkConf.description.strOrElse("Unknown name perk"),
				effects,
				perkConf.fieldOpt("icon").map(_.str)
			)
			byKind += kind -> perk
		}
	}

	override def initialLoad(): Unit = {
		load("game/data/skills/Skills.sml")
	}


//	override def loadTaxons(): Unit = {
//		ResourceManager.sml("game/data/skills/Skills.sml").Perks.fields.keys.foreach {
//			k => Taxonomy.createTaxon(k, "Perks", )
//		}
//	}
}



object SkillsLibrary extends ConfigLoadableLibrary[Skill](Skill) {

	override def defaultNamespace: String = "Skills"

	override protected def topLevelField: String = "Skills"

	override protected def createBlank(): Skill = new Skill

	override def initialLoad(): Unit = {
		load("game/data/skills/Skills.sml")
	}
}
