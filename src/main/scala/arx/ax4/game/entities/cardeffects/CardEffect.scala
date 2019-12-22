package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{HexSelector, ResourceGatherSelector, Selectable, SelectableInstance, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{CharacterInfo, Equipment, Physical}
import arx.ax4.game.entities.{AttackReference, CardSelector, CharacterInfo, DeckData, Equipment, TargetPattern, Tiles}
import arx.ax4.game.logic.{CardLogic, CharacterLogic, GatherLogic, InventoryLogic}
import arx.core.introspection.Field
import arx.core.introspection.FieldOperations.{Add, Sub}
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.data.{CustomConfigDataLoader, TAuxData}
import arx.engine.entity.Entity
import arx.engine.world.{FieldOperationModifier, World, WorldView}
import arx.graphics.TToImage
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, RichText, RichTextRenderSettings, THasRichTextRepresentation, TextSection}

import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

trait CardEffect extends Selectable with THasRichTextRepresentation {
	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String]

	def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = toRichText(settings)
}

object CardEffect {
	val Sentinel = new CardEffect {
		override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = Right("Sentinel")

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Sentinel")
	}
}

abstract class SimpleCardEffect extends CardEffect {
	private val outer = this

	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = {
		Left(new CardEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = outer.applyEffect(world, entity)

			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	}

	def applyEffect(world : World, entity : Entity)
}

trait CardEffectInstance extends SelectableInstance {
	def applyEffect(world : World, selectionResult: SelectionResult)
}

case class GatherCardEffect(range : Int) extends CardEffect {


	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = {
		implicit val view = world

		val center = entity(Physical).position
		val anyGatherInRange = (0 to range).exists { r => HexRingIterator(center, r).exists(v => {GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v)).nonEmpty}) }
		if (!anyGatherInRange) {
			Right("Nothing to gather in range")
		} else {
			Left(new CardEffectInstance {
				val hexSelector = HexSelector(TargetPattern.Point, GatherCardEffect.this, h => h.distance(center) <= range && GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(h)).nonEmpty)

				def prospectSelector(target : AxialVec3) = ResourceGatherSelector(GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(target))(view), GatherCardEffect.this)

				override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
					if (!results.fullySatisfied(hexSelector)) {
						Some(hexSelector)
					} else {
						val hex = results.single(hexSelector)
						val rsrcSel = prospectSelector(hex)
						if (results.fullySatisfied(rsrcSel)) {
							None
						} else {
							Some(rsrcSel)
						}
					}
				}

				override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
					val hex = selectionResult.single(hexSelector)
					val prospect = selectionResult.single(prospectSelector(hex))

					prospect.toGatherProspect(world.view) match {
						case Some(p) => GatherLogic.gather(p)(world)
						case None => Noto.error(s"Gather failed after being selected: $prospect")
					}
				}
			})
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Gather $range")
}

case class GainMovePoints(bonusMP : Sext) extends SimpleCardEffect {
	override def applyEffect(world: World, entity: Entity): Unit = {
		CharacterLogic.gainMovePoints(entity, entity(CharacterInfo)(world.view).moveSpeed + bonusMP)(world)
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Move [Speed] + $bonusMP")

	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = RichText(s"Move ${entity(CharacterInfo)(view).moveSpeed + bonusMP}")
}

case class PayActionPoints(ap : Int) extends CardEffect {


	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = if (CharacterLogic.curActionPoints(entity)(world) >= ap) {
		Left(new CardEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useActionPoints(entity, ap)(world)
			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Insufficient action points")
	}
	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $ap ") :: HorizontalPaddingSection(10) :: ImageSection("graphics/ui/action_point.png", 2.0f, Color.White) :: Nil)
}

case class PayAttackActionPoints(attackReference : AttackReference) extends CardEffect {

	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = attackReference.resolve()(world) match {
		case Some(attackData) =>
			if (CharacterLogic.curActionPoints(entity)(world) >= attackData.actionCost) {
				Left(new CardEffectInstance {
					override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useActionPoints(entity, attackData.actionCost)(world)
					override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
				})
			} else {
				Right("Insufficient action points")
			}
		case None => Right("Could not resolve attack")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Pay Attack AP")
}

case class PayAttackStaminaPoints(attackReference : AttackReference) extends CardEffect {

	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = attackReference.resolve()(world) match {
		case Some(attackData) =>
			if (CharacterLogic.curActionPoints(entity)(world) >= attackData.actionCost) {
				Left(new CardEffectInstance {
					override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useStamina(entity, attackData.actionCost)(world)
					override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
				})
			} else {
				Right("Insufficient stamina")
			}
		case None => Right("Could not resolve attack")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Pay Attack Stamina")
}


case class PayStamina(stamina : Int) extends CardEffect {


	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = if (CharacterLogic.curStamina(entity)(world) >= stamina) {
		Left(new CardEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useStamina(entity, stamina)(world)
			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Insufficient stamina")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $stamina ") :: HorizontalPaddingSection(10) :: ImageSection("graphics/ui/stamina_point_large.png", 2.0f, Color.White) :: Nil)
}


case class DiscardCards(numCards : Int, requirePresent : Boolean, random : Boolean) extends CardEffect {


	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = world.dataOpt[DeckData](entity) match {
		case Some(deck) =>
			if (!requirePresent || deck.hand.size >= numCards) {
				Left(new CardEffectInstance {
					val cardSelector = CardSelector.AnyCard("Card to discard", DiscardCards.this).withAmount(numCards)

					override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
						val cards = selectionResult(cardSelector)
						CardLogic.discardCards(entity, cards, explicit = true)(world)
					}

					override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) { None } else { Some(cardSelector) }
				})
			} else {
				Right("Insufficient cards to discard")
			}
		case None => Right("Entity did not have a hand")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = if (random) {
		RichText(s"Discard $numCards Cards at random")
	} else {
		RichText(s"Discard $numCards Cards")
	}
}

case class EquipItemEffect(item : Entity) extends CardEffect {


	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = if (entity.hasData(Equipment)(world)) {
		// TODO: inventory slots
		Left(new CardEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = InventoryLogic.equip(entity, item)(world)

			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Entity cannot equip items")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Equip")
}


object CardEffectConfigLoader extends CustomConfigDataLoader[CardEffect] {
	val APPattern = "AP\\(([0-9]*)\\)".r
	val MovePattern = "Move\\(([0-9]*)\\)".r
	val StaminaPattern = "Stamina\\(([0-9]*)\\)".r
	val GatherPattern = "Gather\\(([0-9]*)\\)".r

	override def loadedType = typeOf[CardEffect]

	override def loadFrom(config: ConfigValue): CardEffect = {
		if (config.isStr) {
			config.str match {
				case APPattern(ap) => PayActionPoints(ap.toInt)
				case StaminaPattern(stam) => PayStamina(stam.toInt)
				case GatherPattern(range) => GatherCardEffect(range.toInt)
				case MovePattern(mp) => GainMovePoints(mp.toInt)
				case _ =>
					Noto.warn(s"Unparseable card effect : ${config.str}")
					CardEffect.Sentinel
			}
		} else {
			Noto.warn(s"Unparseable card effect : $config")
			CardEffect.Sentinel
		}
	}
}

//case class IncreaseFlag(flag : Taxon, n : Int) extends CardEffect
//case class DecreaseFlag(flag : Taxon, n : Int) extends CardEffect