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

case class AttackCardEffect(attackRef: AttackReference) extends CardEffect {

	def targetSelector(attacker: Entity, attackData: AttackData): Either[Selector[Entity], BiasedHexSelector] = attackData.targetPattern match {
		case hexTargetPattern: HexTargetPattern => Right(BiasedHexSelector(hexTargetPattern, (_, _) => true, this))
		case entityTarget: EntityTarget => Left(
			new EntitySelector(Vector(
				EntityPredicate.Enemy(attacker),
				EntityPredicate.InRange(attacker, attackData.minRange, attackData.maxRange)),
				"Enemy Creature", this).withAmount(entityTarget.count))
	}

	override def nextSelector(world: WorldView, attacker: Entity, results: SelectionResult): Option[Selector[_]] = {
		implicit val view = world
		attackRef.resolve() match {
			case Some(attack) =>
				val tSel = targetSelector(attacker, attack)
				if (results.fullySatisfied(tSel)) {
					None
				} else {
					tSel match {
						case Left(value) => Some(value)
						case Right(value) => Some(value)
					}
				}
			case None =>
				Noto.error(s"Could not resolve attack: $attackRef")
				None
		}
	}


	override def applyEffect(world: World, attacker: Entity, selectionResult: SelectionResult): Unit = {
		implicit val view = world.view
		attackRef.resolve()(world.view) match {
			case Some(attack) =>
				val targets = selectionResult(targetSelector(attacker, attack))
				val entityTargets = targets match {
					case Left(entityTargets) => entityTargets
					case Right(hexTargets) => hexTargets.flatMap(hex => Tiles.tileAt(hex.vec)(Tile).entities)
				}
				CombatLogic.attack(world, attacker, entityTargets, attackRef)

			case None =>
				Noto.error(s"Could not resolve attack when applying: $attackRef")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		attackRef.resolve()(world).isDefined
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		RichText("Attack")
	}

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