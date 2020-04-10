package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{EntityPredicate, EntitySelector, HexSelector, OptionSelector, ResourceGatherSelector, Selectable, SelectableInstance, SelectionResult, Selector, SelfEntityPredicate}
import arx.ax4.game.entities.Companions.{CharacterInfo, CombatData, Equipment, Physical, TagData}
import arx.ax4.game.entities.Conditionals.BaseAttackConditional
import arx.ax4.game.entities.{AttackConditionals, AttackData, AttackKey, AttackModifier, CardLibrary, CardSelector, CardTypes, CharacterInfo, DeckData, EntityArchetype, Equipment, SpecialAttack, TargetPattern, Tiles}
import arx.ax4.game.logic.CardAdditionStyle.{DrawDiscardSplit, DrawPile}
import arx.ax4.game.logic.{CardAdditionStyle, CardLocation, CardLogic, CharacterLogic, CombatLogic, GatherLogic, InventoryLogic, TagLogic}
import arx.core.introspection.Field
import arx.core.introspection.FieldOperations.{Add, Sub}
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.data.{CustomConfigDataLoader, TAuxData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{FieldOperationModifier, World, WorldView}
import arx.graphics.TToImage
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, RichText, RichTextRenderSettings, THasRichTextRepresentation, TaxonSections, TextSection}
import arx.core.introspection.FieldOperations._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import arx.Prelude._
import arx.ax4.game.entities.AttackConditionals.AnyAttack



trait GameEffect extends Selectable with THasRichTextRepresentation {
	override def instantiate(world: WorldView, entity: Entity, source: Entity): Either[GameEffectInstance, String]

	final def toRichText(view: WorldView, entity: Option[Entity], settings: RichTextRenderSettings): RichText = {
		entity match {
			case Some(value) => toRichText(view, value, settings)
			case None => toRichText(settings)
		}
	}
	def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = toRichText(settings)
}

object GameEffect {
	val Sentinel = new GameEffect {
		override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = Right("Sentinel")

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Sentinel")
	}
}

abstract class SimpleGameEffect extends GameEffect {
	private val outer = this

	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		Left(new GameEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = outer.applyEffect(world, entity)

			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	}

	def applyEffect(world : World, entity : Entity)
}

trait GameEffectInstance extends SelectableInstance {
	def applyEffect(world : World, selectionResult: SelectionResult)
}

case class GatherCardEffect(range : Int) extends GameEffect {


	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		implicit val view = world

		val center = entity(Physical).position
		val anyGatherInRange = (0 to range).exists { r => HexRingIterator(center, r).exists(v => {GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v)).nonEmpty}) }
		if (!anyGatherInRange) {
			Right("Nothing to gather in range")
		} else {
			Left(new GameEffectInstance {
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

					GatherLogic.gather(prospect)(world)
				}
			})
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Gather $range")
}

case class GainMovePoints(bonusMP : Sext) extends SimpleGameEffect {
	override def applyEffect(world: World, entity: Entity): Unit = {
		CharacterLogic.gainMovePoints(entity, entity(CharacterInfo)(world.view).moveSpeed + bonusMP)(world)
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Move [Speed] + $bonusMP")

	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = RichText(s"Move ${entity(CharacterInfo)(view).moveSpeed + bonusMP}")
}

case class PayActionPoints(ap : Int) extends GameEffect {


	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = if (CharacterLogic.curActionPoints(entity)(world) >= ap) {
		Left(new GameEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useActionPoints(entity, ap)(world)
			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Insufficient action points")
	}
	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $ap ") :: TaxonSections("GameConcepts.ActionPoint", settings))
}

case class PayStamina(stamina : Int) extends GameEffect {


	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = if (CharacterLogic.curStamina(entity)(world) >= stamina) {
		Left(new GameEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = CharacterLogic.useStamina(entity, stamina)(world)
			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Insufficient stamina")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $stamina ")  :: TaxonSections("GameConcepts.StaminaPoint", settings))
}


case class DiscardCards(numCards : Int, requirePresent : Boolean, random : Boolean) extends GameEffect {


	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = world.dataOpt[DeckData](entity) match {
		case Some(deck) =>
			if (!requirePresent || deck.hand.size >= numCards) {
				Left(new GameEffectInstance {
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

case class DrawCards(numCards : Int) extends GameEffect {
	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		implicit val view = world
		entity.dataOpt[DeckData] match {
			case Some(_) => Left(new GameEffectInstance {
				override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
					for (_ <- 0 until numCards) {
						CardLogic.drawCard(entity)(world)
					}
				}

				override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
			})
			case None => Right("Entity does not have a deck to draw from")
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Draw $numCards Cards")
}

case class AddCardToDeck(cardArchetypes : Seq[Taxon], cardAdditionStyle: CardAdditionStyle) extends GameEffect {
	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		implicit val view = world

		val cardSelector = OptionSelector(cardArchetypes, AddCardToDeck.this)

		if (cardArchetypes.isEmpty) {
			Right("Add card to deck attempted with no cards to choose from")
		} else {
			entity.dataOpt[DeckData] match {
				case Some(_) => Left(new GameEffectInstance {
					override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
						val chosenKind = if(cardArchetypes.size == 1) {
							cardArchetypes.head
						} else {
							selectionResult.single(cardSelector)
						}
						val cardArch = CardLibrary.withKind(chosenKind)

						val card = CardLogic.createCard(entity, cardArch)(world)
						CardLogic.addCard(entity, card, cardAdditionStyle)(world)
					}

					override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (cardArchetypes.size > 1) {
						if (results.fullySatisfied(cardSelector)) {
							None
						} else {
							Some(cardSelector)
						}
					} else {
						None
					}
				})
				case _ => Right("Could not add card to entity without a deck")
			}
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Add Card")
}

case class MoveCardTo(to : CardLocation) extends GameEffect {
	override def instantiate(world: WorldView, entity: Entity, source: Entity): Either[GameEffectInstance, String] = {
		Left(new GameEffectInstance {
			val cardSelfSelector = CardSelector.SelfSelector(MoveCardTo.this)

			override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
				results.firstUnsatisfiedSelector(cardSelfSelector)
			}

			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
				val card = selectionResult.single(cardSelfSelector)
				CardLogic.moveCardTo(entity, card, to)(world)
			}
		})
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		to match {
			case CardLocation.Hand => "return to hand"
			case CardLocation.DrawPile => "return to draw pile"
			case CardLocation.DiscardPile => "return to discard pile"
			case CardLocation.ExhaustPile => "exhaust"
			case CardLocation.NotInDeck => "invalid target location for move"
		}
	}
}

case class ChangeMaxHP(delta : Int) extends SimpleGameEffect {
	override def applyEffect(world: World, entity: Entity): Unit = {
		world.modify(entity, CharacterInfo.health changeMaxBy(delta, true))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		RichText(s"Max HP ${delta.toSignedString}")
	}
}

case class EquipItemEffect(item : Entity) extends GameEffect {


	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = if (entity.hasData(Equipment)(world)) {
		// TODO: inventory slots
		Left(new GameEffectInstance {
			override def applyEffect(world: World, selectionResult: SelectionResult): Unit = InventoryLogic.equip(entity, item)(world)

			override def nextSelector(results: SelectionResult): Option[Selector[_]] = None
		})
	} else {
		Right("Entity cannot equip items")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Equip")
}


case class AddAttackModifierEffect(condition : BaseAttackConditional, modifier : AttackModifier) extends GameEffect {
	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[GameEffectInstance, String] = Left(new GameEffectInstance {

		override def nextSelector(results: SelectionResult): Option[Selector[_]] = None

		override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
			world.modify(entity, CombatData.conditionalAttackModifiers append (condition -> modifier))
		}
	})

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Attack Modifier : $modifier")
}

case class ChangeFlag(flag : Taxon, delta : Int, limitToZero : Boolean) extends SimpleGameEffect {
	override def applyEffect(world: World, entity: Entity): Unit = {
		TagLogic.changeFlagBy(entity, flag, delta, limitToZero)(world)
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"${flag.displayName} $delta")
}

object GameEffectConfigLoader extends CustomConfigDataLoader[GameEffect] {
	val APPattern = "(?i)AP\\(([+-]?[0-9]+)\\)".r
	val MovePattern = "(?i)Move\\(([+-]?[0-9]*)\\)".r
	val StaminaPattern = "(?i)Stamina\\(([+-]?[0-9]*)\\)".r
	val GatherPattern = "(?i)Gather\\(([0-9]*)\\)".r
	val AddCardPattern = "(?i)AddCard\\(([a-zA-Z0-9]+)\\)".r
//	val SpecialAttackPattern = "SpecialAttackCard\\((.+?)\\)".r
//	val SpecialAttackPattern = "(?i)SpecialAttack\\(([a-zA-Z0-9]+)\\)".r
	val MaxHPPattern = "(?i)MaxHP\\(([+-]?[0-9]+)\\)".r
	val KeyNumberPattern = "([a-zA-Z]+)\\s*\\(([+-]?[0-9]+)\\s*\\)".r
	val MoveCardToPattern = "(?i)move\\s?card\\s?to\\s?\\(([a-zA-Z]+)\\)".r


	val allFlags = {
		Taxonomy.descendantsOf("Flag").map(f => f.name.toLowerCase -> f).toMap
	}

	override def loadedType = typeOf[GameEffect]

	override def loadFrom(config: ConfigValue): Option[GameEffect] = {
		if (config.isStr) {
			config.str.trim match {
				case APPattern(ap) => Some(PayActionPoints(ap.toInt))
				case StaminaPattern(stam) => Some(PayStamina(stam.toInt))
				case GatherPattern(range) => Some(GatherCardEffect(range.toInt))
				case MovePattern(mp) => Some(GainMovePoints(mp.toInt))
				//				case SpecialAttackPattern(attName) if SpecialAttack.withNameExists(attName) => AddSpecialAttackCardEffect(attName, SpecialAttack.withName(attName))
				case AddCardPattern(cardName) => Some(AddCardToDeck(List(Taxonomy(cardName, "CardTypes")), DrawPile))
				case MaxHPPattern(maxHP) => Some(ChangeMaxHP(maxHP.toInt))
				//				case SpecialAttackPattern(attackName) => SpecialAttackCardEffect(SpecialAttack.withName(attackName))
				case KeyNumberPattern(key, numberStr) =>
					val number = numberStr.toInt
					key.toLowerCase match {
						case "draw" | "drawCards" => Some(DrawCards(number))
						case f if allFlags.contains(f) => Some(ChangeFlag(allFlags(f), number, limitToZero = true))
						case _ =>
							Noto.warn(s"Unparseable card effect : ${config.str}")
							None
					}
				case MoveCardToPattern(to) => CardLocation.parse(to).flatMap(to => Some(MoveCardTo(to)))
				case _ =>
					Noto.warn(s"Unparseable card effect : ${config.str}")
					None
			}
		} else if (config.isObj) {
			config.fieldOpt("type") match {
				case Some(effectType) =>
					effectType.str.toLowerCase match {
						case "conditionalattackmodifier" =>
							val modifier = AttackModifier.loadFromConfig(config.modifier)
							AttackConditionals.loadFrom(config.condition).map(cond => AddAttackModifierEffect(cond, modifier))
						case "attack" =>
							val attack = AttackData(Entity.Sentinel).loadFromConfig(config)
							Some(AttackGameEffect(AttackKey.Technique, attack))
						case "specialattack" =>
							val attackModifier = AttackModifier().loadFromConfig(config)
							val condition = AttackConditionals.loadFrom(config) match {
								case Some(cond) => cond
								case None =>
									Noto.error(s"could not load attack conditional from : $config")
									AnyAttack
							}
							Some(SpecialAttackGameEffect(SpecialAttack(condition, attackModifier)))
						case _ =>
							Noto.warn(s"Game effect config with unsupported type ${effectType.render}")
							None
					}
				case None =>
					Noto.warn(s"Game effect config as object without type field ${config.render}")
					None
			}
		} else {
			Noto.warn(s"Unparseable card effect : ${config.render}")
			None
		}
	}
}


//case class DecreaseFlag(flag : Taxon, n : Int) extends CardEffect