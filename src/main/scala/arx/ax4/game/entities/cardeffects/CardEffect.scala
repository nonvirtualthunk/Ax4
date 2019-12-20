package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{HexSelector, ResourceGatherSelector, Selectable, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.{AttackReference, CardSelector, CharacterInfo, DeckData, TargetPattern, Tiles}
import arx.ax4.game.logic.{CardLogic, CharacterLogic, GatherLogic, InventoryLogic}
import arx.core.introspection.Field
import arx.core.introspection.FieldOperations.{Add, Sub}
import arx.core.math.Sext
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.data.TAuxData
import arx.engine.entity.Entity
import arx.engine.world.{FieldOperationModifier, World, WorldView}
import arx.graphics.TToImage
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, RichText, RichTextRenderSettings, THasRichTextRepresentation, TextSection}

import scala.reflect.ClassTag

trait CardEffect extends Selectable with THasRichTextRepresentation {
	def nextSelector(world : WorldView, entity : Entity, results : SelectionResult) : Option[Selector[_]]
	def applyEffect(world : World, entity : Entity, selectionResult: SelectionResult)
	def canApplyEffect(world : WorldView, entity: Entity) : Boolean

	def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = toRichText(settings)
}

case class GatherHexSelector(entity : Entity, range : Int, selectable : Selectable) extends HexSelector(TargetPattern.Point, selectable) {
	override def hexPredicate(view: WorldView, hex: AxialVec3): Boolean = {
		hex.distance(entity(Physical)(view).position) <= range && GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(hex))(view).nonEmpty
	}
}

case class GatherCardEffect(range : Int) extends CardEffect {
	def prospectSelector(view : WorldView, entity : Entity, target : AxialVec3) = ResourceGatherSelector(GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(target))(view), this)

	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = {
		val hexSel = GatherHexSelector(entity, range, this)
		if (!results.fullySatisfied(hexSel)) {
			Some(hexSel)
		} else {
			val hex = results.single(hexSel)
			val rsrcSel = prospectSelector(world, entity, hex)
			if (results.fullySatisfied(rsrcSel)) {
				None
			} else {
				Some(rsrcSel)
			}
		}
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		val hex = selectionResult.single(GatherHexSelector(entity, range, this))
		val prospect = selectionResult.single(prospectSelector(world.view, entity, hex))

		prospect.toGatherProspect(world.view) match {
			case Some(p) => GatherLogic.gather(p)(world)
			case None => Noto.error(s"Gather failed after being selected: $prospect")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		implicit val view = world
		val center = entity(Physical).position
		(0 to range).exists { r => HexRingIterator(center, r).exists(v => {GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v)).nonEmpty}) }
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Gather $range")
}

case class GainMovePoints(bonusMP : Sext) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		CharacterLogic.gainMovePoints(entity, entity(CharacterInfo)(world.view).moveSpeed + bonusMP)(world)
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = true

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Move [Speed] + $bonusMP")

	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = RichText(s"Move ${entity(CharacterInfo)(view).moveSpeed + bonusMP}")
}

case class PayActionPoints(ap : Int) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		CharacterLogic.useActionPoints(entity, ap)(world)
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = CharacterLogic.curActionPoints(entity)(world) >= ap

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $ap ") :: HorizontalPaddingSection(10) :: ImageSection("graphics/ui/action_point.png", 2.0f, Color.White) :: Nil)
}

case class PayAttackActionPoints(attackReference : AttackReference) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		for (attack <- attackReference.resolve()(world.view)) {
			CharacterLogic.useActionPoints(entity, attack.actionCost)(world)
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = attackReference.resolve()(world) match {
		case Some(attack) => CharacterLogic.curActionPoints(entity)(world) >= attack.actionCost
		case None => false
	}


	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Pay Attack AP")
}

case class PayAttackStaminaPoints(attackReference : AttackReference) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		for (attack <- attackReference.resolve()(world.view)) {
			CharacterLogic.useStamina(entity, attack.staminaCost)(world)
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = attackReference.resolve()(world) match {
		case Some(attack) => CharacterLogic.curStamina(entity)(world) >= attack.staminaCost
		case None => false
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Pay Attack Stamina")
}


case class PayStamina(stamina : Int) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		CharacterLogic.useStamina(entity, stamina)(world)
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = CharacterLogic.curStamina(entity)(world) >= stamina

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"Pay $stamina ") :: HorizontalPaddingSection(10) :: ImageSection("graphics/ui/stamina_point_large.png", 2.0f, Color.White) :: Nil)
}


case class DiscardCards(numCards : Int, random : Boolean) extends CardEffect {
	val cardSelector = CardSelector.AnyCard("Card to discard", this).withAmount(numCards)

	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) {
		None
	} else {
		Some(cardSelector)
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		world.view.dataOpt[DeckData](entity) match {
			case Some(_) =>
				val cards = selectionResult(cardSelector)
				CardLogic.discardCards(entity, cards, explicit = true)(world)
			case None =>
				Noto.error("No deck to discard from")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		world.dataOpt[DeckData](entity) match {
			case Some(deck) => deck.hand.size >= numCards
			case None => false
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = if (random) {
		RichText(s"Discard $numCards Cards at random")
	} else {
		RichText(s"Discard $numCards Cards")
	}
}

case class EquipItemEffect(item : Entity) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = InventoryLogic.equip(entity, item)(world)

	// TODO: inventory slots
	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = true

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Equip")
}

//case class IncreaseFlag(flag : Taxon, n : Int) extends CardEffect
//case class DecreaseFlag(flag : Taxon, n : Int) extends CardEffect