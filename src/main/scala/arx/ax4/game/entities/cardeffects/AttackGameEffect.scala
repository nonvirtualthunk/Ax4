package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.control.components.DamageExpression
import arx.ax4.game.action.{HexPatternSelector, EntityPredicate, EntitySelector, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.Tile
import arx.ax4.game.entities._
import arx.ax4.game.logic.{AllegianceLogic, CombatLogic, SpecialAttackLogic}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, RichText, RichTextRenderSettings, TextSection}
import arx.Prelude._

case class AttackGameEffect(attackKey : AttackKey, attackData: AttackData) extends GameEffect {

	override def instantiate(world: WorldView, attacker: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		Left(AttackGameEffectInstance(attacker,attackData, this)(world))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		RichText("Attack")
	}

	// TODO: Move this inside the card effect instance as well?
	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = {
		implicit val v = view
		RichText(
			Seq(
				TextSection(s"Attack ${attackData.accuracyBonus.toSignedString}"),
				HorizontalPaddingSection(4),
				ImageSection("graphics/ui/crosshairs.png", 2.0f, Color.White),
				HorizontalPaddingSection(10)) ++
				DamageExpression(attackData.damage).toRichText(settings).sections
		)
	}
}

case class AttackGameEffectInstance(attacker : Entity, attackData : AttackData, cardEffect: GameEffect)(implicit val view : WorldView) extends GameEffectInstance {
	val targetSelector: Either[Selector[Entity], HexPatternSelector] =
		attackData.targetPattern match {
			case hexTargetPattern: HexTargetPattern => Right(HexPatternSelector(attacker[Physical].position, hexTargetPattern, (_, _) => true, cardEffect))
			case entityTarget: EntityTarget => Left(
				new EntitySelector(Vector(
					EntityPredicate.Enemy(attacker),
					EntityPredicate.InRange(attacker, attackData.minRange, attackData.maxRange)), "Enemy Creature", cardEffect).withAmount(entityTarget.count))
		}


	override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
		val targets = selectionResult(targetSelector)
		val entityTargets = targets match {
			case Left(entityTargets) => entityTargets
			case Right(hexTargets) => hexTargets.flatten.flatMap(hex => Tiles.tileAt(hex)(Tile).entities)
		}
		CombatLogic.attack(world, attacker, entityTargets, attackData)
	}

	override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(targetSelector)) {
		None
	} else {
		targetSelector match {
			case Left(value) => Some(value)
			case Right(value) => Some(value)
		}
	}
}