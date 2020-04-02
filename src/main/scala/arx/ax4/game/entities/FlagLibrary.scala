package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.components.FlagComponent
import arx.ax4.game.components.FlagComponent.{ChangeFlagBy, FlagBehavior}
import arx.ax4.game.entities.Companions.TagInfo
import arx.core.datastructures.MultiMap
import arx.core.introspection.ReflectionAssistant
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
import arx.engine.data.ConfigLoadable
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.event.GameEvent
import arx.engine.world.EventState.Ended
import arx.graphics.helpers.{RichText, RichTextRenderSettings, THasRichTextRepresentation, TaxonSections}

case class FlagInfo(descriptions : String, limitToZero : Boolean = true, hidden : Boolean = false, simpleBehaviors : Seq[FlagBehavior] = Nil)

object FlagLibrary extends Library[FlagInfo] {
	lazy val eventClassesByName : Map[String, Class[_]] = ReflectionAssistant.allSubTypesOf(classOf[GameEvent])
		.map(c => c.getSimpleName.replace("$","") -> c)
		.toMap

	override protected def topLevelField: String = "Flags"

	override protected def createBlank(): FlagInfo = FlagInfo("Blank")

	override def load(config: ConfigValue): Unit = {
		for ((k,flagConf) <- config.field(topLevelField).fields) {
			val flag = Taxonomy(k, "Flags")
			val limitToZero = flagConf.limitToZero.boolOrElse(true)
			var behaviors = Vector[FlagBehavior]()
			// "custom" here is just a marker to note that we define its tick down behavior in code
			for (tickDownEvent <- flagConf.fieldAsList("tickDownOn") ; if tickDownEvent.str.toLowerCase != "custom" ) {
				eventClassesByName.get(tickDownEvent.str) match {
					case Some(eventClass) =>
						behaviors :+= FlagBehavior(flag, ChangeFlagBy(-1, limitToZero), {
							case e : GameEvent if e.getClass == eventClass && e.state == Ended =>
								ReflectionAssistant.getFieldValue(e, "entity").asInstanceOf[Entity]
						})
					case None => Noto.warn(s"Flag $flag config to tick down on $tickDownEvent, but no event class of that type found")
				}
			}
			if (flagConf.resetAtEndOfTurn.boolOrElse(false)) {
				behaviors :+= FlagComponent.resetAtEndOfTurn(k)
			}
			for (equivConf <- flagConf.fieldAsList("countsAs")) {
				FlagEquivalency.flagEquivalences.add(Taxonomy(equivConf.str, "Flags"), flag)
			}
			val info = FlagInfo(flagConf.description.str, limitToZero, flagConf.hidden.boolOrElse(false), behaviors)
			byKind += flag -> info
		}
	}

	override def initialLoad(): Unit = {
		load("game/data/flags/Flags.sml")
	}
}

object FlagEquivalency {
	val flagEquivalences = MultiMap.empty[Taxon,Taxon]
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