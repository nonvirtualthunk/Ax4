package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.control.components.DamageExpression
import arx.ax4.game.action.{BiasedHexSelector, EntityPredicate, EntitySelector, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.Tile
import arx.ax4.game.entities._
import arx.ax4.game.logic.{AllegianceLogic, CombatLogic}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, RichText, RichTextRenderSettings, TextSection}
import arx.Prelude._

case class AttackCardEffect(attackRef: AttackReference) extends GameEffect {

	override def instantiate(world: WorldView, attacker: Entity): Either[GameEffectInstance, String] = attackRef.resolve()(world) match {
		case Some(attackData) =>
			Left(AttackGameEffectInstance(attacker, attackRef, attackData, this)(world))
		case None =>
			Right("Attack could not be resolved")
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		RichText("Attack")
	}

	// TODO: Move this inside the card effect instance as well?
	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = {
		implicit val v = view

		attackRef.resolve() match {
			case Some(attack) =>
				RichText(
					Seq(
						TextSection(s"Attack ${attack.accuracyBonus.toSignedString}"),
						HorizontalPaddingSection(4),
						ImageSection("graphics/ui/crosshairs.png", 2.0f, Color.White),
						HorizontalPaddingSection(10)) ++
						DamageExpression(attack.damage).toRichText(settings).sections
				)
			case None => RichText("Invalid attack")
		}
	}
}

//case class SpecialAttackByRefCardEffect(source : Entity, specialAttackKey : AnyRef) extends GameEffect {
//	override def instantiate(world: WorldView, entity: Entity): Either[GameEffectInstance, String] = {
//		implicit val view = world
//		CombatLogic.validSpecialAttacksFor(entity, source, specialAttackKey).headOption.flatMap(aref => aref.resolve().map(aref -> _)) match {
//			case Some((attackRef, attack)) => Left(AttackGameEffectInstance(entity, attackRef, attack, this))
//			case None => Right("No valid special attack")
//		}
//	}
//
//	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Special Attack")
//}

case class SpecialAttackCardEffect(specialAttack: SpecialAttack) extends GameEffect {
	override def instantiate(world: WorldView, entity: Entity): Either[GameEffectInstance, String] = {
		implicit val view = world
		CombatLogic.resolveSpecialAttack(entity, specialAttack).flatMap(aref => aref.resolve().map(aref -> _)) match {
			case Some((attackRef, attack)) => Left(AttackGameEffectInstance(entity, attackRef, attack, this))
			case None => Right("No valid special attack")
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"Special Attack")
}

case class AttackGameEffectInstance(attacker : Entity, attackRef : AttackReference, attackData : AttackData, cardEffect: GameEffect)(implicit val view : WorldView) extends GameEffectInstance {
	val targetSelector: Either[Selector[Entity], BiasedHexSelector] =
		attackData.targetPattern match {
			case hexTargetPattern: HexTargetPattern => Right(BiasedHexSelector(hexTargetPattern, (_, _) => true, cardEffect))
			case entityTarget: EntityTarget => Left(
				new EntitySelector(Vector(
					EntityPredicate.Enemy(attacker),
					EntityPredicate.InRange(attacker, attackData.minRange, attackData.maxRange)), "Enemy Creature", cardEffect).withAmount(entityTarget.count))
		}


	override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
		val targets = selectionResult(targetSelector)
		val entityTargets = targets match {
			case Left(entityTargets) => entityTargets
			case Right(hexTargets) => hexTargets.flatMap(hex => Tiles.tileAt(hex.vec)(Tile).entities)
		}
		CombatLogic.attack(world, attacker, entityTargets, attackRef)
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