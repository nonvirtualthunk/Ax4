package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.components.FlagComponent
import arx.ax4.game.components.FlagComponent.{ChangeFlagBy, FlagBehavior}
import arx.ax4.game.entities.Companions.TagInfo
import arx.core.datastructures.MultiMap
import arx.core.introspection.ReflectionAssistant
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigDataLoader, ConfigLoadable}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.event.GameEvent
import arx.engine.world.EventState.Ended
import arx.graphics.helpers.{RichText, RichTextRenderSettings, THasRichTextRepresentation, TaxonSections}
import arx.Prelude._
import arx.ax4.game.event.{AttackEvent, EntityMoved}
import arx.ax4.game.event.TurnEvents.EntityTurnEndEvent
import arx.core.NoAutoLoad

class FlagInfo extends ConfigLoadable {
	var description : Option[String] = None
	var vagueDescription : Option[String] = None
	var limitToZero : Boolean = true
	var binary : Boolean = true
	var hidden : Boolean = false
	@NoAutoLoad
	var simpleBehaviors : Vector[FlagBehavior] = Vector()
	var attackModifiers : Vector[AttackModifier] = Vector()
}

object FlagLibrary extends Library[FlagInfo] {
	lazy val eventClassesByName : Map[String, Class[_]] = ReflectionAssistant.allSubTypesOf(classOf[GameEvent])
		.map(c => c.getSimpleName.replace("$","") -> c)
		.toMap

	val attackPattern = "(?i)attack".r
	val movePattern = "(?i)move".r
	val endOfTurnPattern = "(?i)end\\s?(Of)?\\s?Turn".r
	val flagNameAmountExpression = "(?i)([a-zA-Z.]+)\\s?(?:\\(([0-9]+)\\))?".r

	override protected def topLevelField: String = "Flags"

	override protected def createBlank(): FlagInfo = new FlagInfo()

	override def load(config: ConfigValue): Unit = {

		for ((k,flagConf) <- config.field(topLevelField).fields; info <- ConfigDataLoader.loadFrom[FlagInfo](flagConf)) {

			val flag = Taxonomy(k, "Flags")
			var behaviors = Vector[FlagBehavior]()
			// "custom" here is just a marker to note that we define its tick down behavior in code
			for (tickDownEvent <- flagConf.fieldAsList("tickDownOn") ; if tickDownEvent.str.toLowerCase != "custom" ) {
				tickDownEvent.str match {
					case endOfTurnPattern(_) =>
						behaviors :+= FlagBehavior(flag, ChangeFlagBy(-1, info.limitToZero), {
							case e: EntityTurnEndEvent if e.state == Ended => e.entity
						})
					case attackPattern() =>
						behaviors :+= FlagBehavior(flag, ChangeFlagBy(-1, info.limitToZero), {
							case AttackEvent(attInfo) => attInfo.attacker
						})
					case movePattern() =>
						behaviors :+= FlagBehavior(flag, ChangeFlagBy(-1, info.limitToZero), {
							case EntityMoved(entity, from, to) => entity
						})
					case _ =>
						eventClassesByName.get(tickDownEvent.str) match {
							case Some(eventClass) =>
								behaviors :+= FlagBehavior(flag, ChangeFlagBy(-1, info.limitToZero), {
									case e : GameEvent if e.getClass == eventClass && e.state == Ended =>
										ReflectionAssistant.getFieldValue(e, "entity").asInstanceOf[Entity]
								})
							case None => Noto.warn(s"Flag $flag config to tick down on $tickDownEvent, but no event class of that type found")
						}
				}
			}

			if (flagConf.resetAtEndOfTurn.boolOrElse(false)) {
				behaviors :+= FlagComponent.resetAtEndOfTurn(k)
			}
			if (flagConf.resetAtStartOfTurn.boolOrElse(false)) {
				behaviors :+= FlagComponent.resetAtStartOfTurn(k)
			}

			val equivalencyConfigs = flagConf.fieldAsList("countsAs").map(_ -> (1,0)) :::
				flagConf.fieldAsList("countsAsNegative").map(_ -> (-1,0)) :::
				flagConf.fieldAsList("countsAsNegativeOne").map(_ -> (0,-1)) :::
				flagConf.fieldAsList("countsAsOne").map(_ -> (0,1))
			for ((equivConf, (baseMultiplier, baseAdder)) <- equivalencyConfigs) {
				equivConf.str match {
					case flagNameAmountExpression(flagName, value) =>
						val multiplier = value match {
							case null => 1
							case other => other.toInt
						}
						FlagEquivalency.flagEquivalences.add(Taxonomy(flagName, "Flags"), FlagEquivalence(flag,multiplier * baseMultiplier, baseAdder))
					case str => Noto.error(s"Unparseable countsAs equivalency: $str")
				}
			}


			info.simpleBehaviors = behaviors
//			val info = FlagInfo(flagConf.description.str, limitToZero, flagConf.hidden.boolOrElse(false), behaviors, attackModifiers.toVector)
			byKind += flag -> info
		}
	}

	override def initialLoad(): Unit = {
		load("game/data/flags/Flags.sml")
	}
}

case class FlagEquivalence(taxon: Taxon, multiplier : Int, adder : Int)

object FlagEquivalency {
	val flagEquivalences = MultiMap.empty[Taxon,FlagEquivalence]
}


@GenerateCompanion
class TagInfo extends ConfigLoadable with THasRichTextRepresentation {
	var kind : Taxon = Taxonomy.UnknownThing
	def tag = kind
	var hidden = false

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		RichText(TaxonSections(kind, settings))
	}
}

object TagLibrary extends ConfigLoadableLibrary[TagInfo](TagInfo) {
	override def defaultNamespace: String = "Tags"

	override protected def topLevelField: String = "Tags"

	override protected def createBlank(): TagInfo = new TagInfo

	override def initialLoad(): Unit = {
		load("game/data/tags/Tags.sml")
	}
}