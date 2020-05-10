package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.control.components.DamageExpression
import arx.ax4.game.action.{EntityPredicate, EntitySelector, HexPatternSelector, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.Tile
import arx.ax4.game.entities._
import arx.ax4.game.logic.{AllegianceLogic, CombatLogic, InventoryLogic, SpecialAttackLogic}
import arx.engine.entity.{Entity, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, LineBreakSection, RGBA, RichText, RichTextRenderSettings, RichTextSection, TaxonSections, TextSection}
import arx.Prelude._
import arx.ax4.game.entities.AttackConditionals.AnyAttack
import arx.ax4.game.entities.TargetPattern.Line
import arx.engine.data.Moddable

case class AttackGameEffect(key: AttackKey, attackData: AttackData) extends GameEffect {

	override def instantiate(world: WorldView, attacker: Entity, effectSource: Entity): Either[GameEffectInstance, String] = {
		Left(AttackGameEffectInstance(attacker, attackData, this)(world))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		toRichText(null, Entity.Sentinel, settings)
	}

	override def toRichText(view: WorldView, entity: Entity, settings: RichTextRenderSettings): RichText = {
		val (effAttackData, modifiers) = if (view != null) {
			CombatLogic.resolveUntargetedConditionalAttackData(view, entity, attackData)
		} else {
			(attackData, Vector())
		}
		AttackGameEffect.toRichText(attackData, effAttackData, "Attack", settings)
	}
}

object AttackGameEffect {
	def toRichText(attackData: AttackData, effAttackData: AttackData, prefix : String, settings: RichTextRenderSettings): RichText = {
		val accuracyColor = attackDataPartToColor(attackData, effAttackData, _.accuracyBonus)

		val mainLine = RichText(Vector(
			TextSection(s"$prefix "),
			TextSection(s"${effAttackData.accuracyBonus.toSignedString}", Moddable(accuracyColor)),
			HorizontalPaddingSection(4)) ++
			TaxonSections("Accuracy", settings) ++
			DamageExpression(effAttackData.damage).toRichText(settings).sections)
		var rangeShapeLine = RichText.Empty
		if (effAttackData.minRange > 1 || effAttackData.maxRange > 1) {
			val rangeExpr = s"${effAttackData.maxRange}"
			//			rangeShapeLine ++= Seq(LineBreakSection(10), TextSection(s"$rangeExpr"), HorizontalPaddingSection(4), ImageSection(s"graphics/ui/range.png", 1.0f, Color.White))
			rangeShapeLine ++= Seq(TextSection(s"Range $rangeExpr"), HorizontalPaddingSection(5))
		}
		effAttackData.targetPattern match {
			case pattern: HexTargetPattern => rangeShapeLine ++= TextSection("Target") +: HorizontalPaddingSection(4) +: pattern.toRichText(settings).sections
			case _ => // do nothing
		}
		var effLine = mainLine
		if (rangeShapeLine.nonEmpty) {
			effLine += LineBreakSection(5)
			effLine ++= rangeShapeLine
		}

		val triggeredLine = AttackData.renderTriggeredAttackEffects(attackData.triggeredEffects, settings)
		if (triggeredLine.nonEmpty) {
			effLine += LineBreakSection(5)
			effLine ++= triggeredLine
		}

		effLine
	}

	def attackDataPartToColor[T: Numeric](prev: AttackData, cur: AttackData, component: (AttackData) => T): Color = {
		val a = component(prev)
		val b = component(cur)
		implicitly[Numeric[T]].compare(a, b) match {
			case 1 => RGBA(0.8f, 0.3f, 0.2f, 1.0f)
			case 0 => Color.Black
			case -1 => RGBA(0.1f, 0.5f, 0.25f, 1.0f)
		}
	}
}

case class SpecialAttackGameEffect(specialAttack: SpecialAttack) extends GameEffect {

	override def instantiate(world: WorldView, entity: Entity, source: Entity): Either[GameEffectInstance, String] = {
		implicit val view = world
		CombatLogic.resolveSpecialAttack(entity, specialAttack) match {
			case Some(effAttack) =>
				Left(AttackGameEffectInstance(entity, effAttack, this))
			case None =>
				Right("No matching attack")
		}
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		toRichText(null, Entity.Sentinel, settings)
	}

	override def toRichText(view: WorldView, attacker: Entity, settings: RichTextRenderSettings): RichText = {
		val preamble = if (specialAttack.condition != AnyAttack) {
			val conditionSections = specialAttack.condition.toRichText(settings).sections
			RichText(TaxonSections("GameConcepts.Attack", settings) ++ Seq(LineBreakSection(0), TextSection("Requires ")) ++ conditionSections :+ LineBreakSection(0))
		} else {
			RichText.parse("[GameConcepts.Attack]\n", settings)
		}

		val modifierSections = specialAttack.attackModifier.toRichText(settings)
		if (view == null) {
			preamble ++ modifierSections
		} else {
			CombatLogic.resolveSpecialAttack(attacker, specialAttack)(view) match {
				case Some(baseAttack) =>
					val (effAttack, _) = CombatLogic.resolveUntargetedConditionalAttackData(view, attacker, baseAttack)
					AttackGameEffect.toRichText(baseAttack, effAttack, "Attack", settings)
				case None =>
					//  + TextSection("(No Matching Attack)\nModifiers\n")
					preamble ++ modifierSections
			}
		}
	}
}

case class AttackGameEffectInstance(attacker: Entity, attackData: AttackData, cardEffect: GameEffect)(implicit val view: WorldView) extends GameEffectInstance {
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