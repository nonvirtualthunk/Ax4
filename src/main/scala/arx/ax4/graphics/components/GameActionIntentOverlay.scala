package arx.ax4.graphics.components

import arx.application.Noto
import arx.ax4.control.components.{ActionSelectionContext, DamageExpression}
import arx.ax4.game.action.{AttackAction, AttackIntent, GameActionIntent, GatherIntent}
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, ResourceSourceData}
import arx.ax4.game.entities.Tiles
import arx.ax4.game.logic.{CombatLogic, GatherLogic}
import arx.ax4.graphics.data.{CullingData, SpriteLibrary, TacticalUIData}
import arx.ax4.graphics.logic.GameWidgetLogic
import arx.core.vec.{Vec2T, Vec2f}
import arx.engine.control.components.windowing.widgets.{DimensionExpression, ImageDisplayWidget, PositionExpression, TopLeft, TopRight}
import arx.engine.control.components.windowing.{SimpleWidget, Widget}
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.graphics.{GL, TToImage}
import arx.resource.ResourceManager

trait GameActionIntentOverlay {
	def draw(game : WorldView, display : World, intent : GameActionIntent, consideredContext : Option[ActionSelectionContext], canvas : AxCanvas) {}
	def updateUI(game : WorldView, display : World, consideredContext : Option[ActionSelectionContext], desktop : Widget) {}

	def update(game : WorldView, display : World, consideredContext : Option[ActionSelectionContext]) {}
}


class GatherIntentOverlay extends GameActionIntentOverlay {
	override def draw(game: WorldView, display: World, intent: GameActionIntent, consideredContext : Option[ActionSelectionContext], canvas: AxCanvas): Unit = {
		implicit val view = game;
		intent match {
			case GatherIntent =>
				for (selC <- display[TacticalUIData].selectedCharacter) {
					for (hex <- display[CullingData].hexesByCartesianCoord) {
						val resourceTypes = GatherLogic.gatherProspectsFor(selC, Tiles.tileAt(hex))(game)
   						.filter(p => p.target(ResourceSourceData).resources(p.key).amount.currentValue > 0)
							.map(_.key.kind).toSet
						var xOffset = 0
						for (resourceType <- resourceTypes) {
							val sprite = SpriteLibrary.iconFor(resourceType).image


							canvas.quad(hex)
								.hexBottomOrigin(0.0f, 0.1f)
								.offset(Vec2f(xOffset, 0.0f))
								.texture("third-party/DHGZ/frames/wood_frame.png", 1)
   							.layer(DrawLayer.OverEntity)
								.draw()

							canvas.quad(hex)
								.hexBottomOrigin(0.0f, 0.1f)
								.offset(Vec2f(xOffset + 3,3.0f))
								.texture(sprite, 1)
								.layer(DrawLayer.OverEntity)
								.draw()

							xOffset += 68
						}
					}
				}
			case _ =>
		}

	}
}

class AttackIntentOverlay extends GameActionIntentOverlay {
	import arx.Prelude._

	var widgetsByEntity : Map[Entity, Widget] = Map()

	override def updateUI(game: WorldView, display: World, consideredContextOpt : Option[ActionSelectionContext], desktop: Widget): Unit = {
		implicit val view = game
		var keepPreviewWidget = false
		consideredContextOpt match {
			case Some(consideredContext)  =>
				for (selC <- display[TacticalUIData].selectedCharacter ; actions <- consideredContext.completeAction ; action <- actions) action pmatch {
					case AttackAction(attacker, attack, from, targets, preMove, postMove) =>
						val targetEntities : Seq[Entity] = targets match {
							case Left(value) => value
							case Right(value) => value.flatMap(h => Tiles.entitiesOnTile(h))
						}

						widgetsByEntity.values.foreach(_.destroy())
						val widgetsByTarget = for ((target, strikes) <- CombatLogic.effectiveAttackData(attacker, from, targetEntities, attack).strikesByTarget) yield {
							val w = desktop.createChild("AttackInfoWidgets.ConsideredAttackInfo")

							w.bind("attack", AttackPreviewData(strikes.head.attackData.name, strikes.head.attackData.accuracyBonus.toSignedString, DamageExpression(strikes.head.attackData.damage),
								target(CharacterInfo).health.currentValue, strikes.head.defenseData.dodgeBonus.toSignedString, Taxonomy("DefenseBonus")))

							target -> w.widget
						}
						widgetsByEntity = widgetsByTarget

						keepPreviewWidget = true
				}
			case _ =>
		}
		if (!keepPreviewWidget) {
			widgetsByEntity.values.foreach(_.destroy())
			widgetsByEntity = Map()
		}
	}

	override def update(game: WorldView, display: World, consideredContext: Option[ActionSelectionContext]): Unit = {
		implicit val view = game
		for ((entity,w) <- widgetsByEntity) {
			val pos = GameWidgetLogic.gamePositionToWidgetPosition(entity(Physical).position)(display)

			val (relativeTo, shift) = if (pos.x > (GL.viewport.width - 400) / 2) {
				TopRight -> -50
			} else {
				TopLeft -> 50
			}

			w.x = PositionExpression.Absolute(pos.x.toInt + shift, relativeTo)
			w.y = PositionExpression.Absolute(pos.y.toInt, relativeTo)
		}
	}
}

case class AttackPreviewData(name : String, accuracyBonus : String, damage : DamageExpression, defenderHP : Int, defense : String, defenseConcept : Taxon)