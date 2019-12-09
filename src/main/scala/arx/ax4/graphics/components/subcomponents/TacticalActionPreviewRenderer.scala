package arx.ax4.graphics.components.subcomponents

import arx.ai.search.PathStep
import arx.application.Noto
import arx.ax4.game.action.{AttackAction, BiasedAxialVec3, BiasedHexSelector, CompoundSelectable, GameAction, HexSelector, MoveAction, MoveCharacter, Selectable, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.graphics.components.{AttackPreviewData, AxCanvas, DrawLayer}
import arx.core.vec.Vec3f
import arx.core.vec.coordinates.AxialVec3
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.resource.ResourceManager
import arx.Prelude._
import arx.ax4.control.components.{ActionSelectionContext, DamageExpression}
import arx.ax4.game.entities.Tiles
import arx.ax4.game.entities.cardeffects.AttackCardEffect
import arx.ax4.game.logic.CombatLogic
import arx.ax4.graphics.data.TacticalUIData
import arx.ax4.graphics.logic.GameWidgetLogic
import arx.core.introspection.ReflectionAssistant
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.{PositionExpression, TopLeft, TopRight}
import arx.engine.entity.{Entity, Taxonomy}
import arx.graphics.GL
import arx.graphics.helpers.RGBA

trait TacticalActionPreviewRenderer {
	def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _]
}


class MoveActionPreviewRenderer extends TacticalActionPreviewRenderer {
	override def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _] = {
		case MoveAction(entity, path) =>
			if (path.steps.size > 1) {
				implicit val view = game
				val img = ResourceManager.image("third-party/wesnoth/feet.png")
				val startPos = path.steps.head.node
				var prev = startPos

				for (PathStep(hex, cost) <- path.steps; if hex != startPos) {
					val stepDir = (hex.asCartesian(1.0f) - prev.asCartesian(1.0f)).normalize
					val ortho = stepDir.cross(Vec3f.UnitZ)
					canvas.quad(hex)
						.texture(img)
						.dimensions(img.width * 2, img.height * 2)
						.layer(DrawLayer.UnderEntity)
						.forward(stepDir.xy.normalizeSafe)
						.ortho(ortho.xy.normalizeSafe)
						.draw()
					prev = hex
				}
			} else {
				Noto.warn(s"trying to preview degenerate path : $path")
			}
	}
}

class AttackActionPreviewRenderer extends TacticalActionPreviewRenderer {
	override def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _] = {
		case AttackAction(attacker, attack, attackFrom, targets, preMove, postMove) =>
			implicit val view = game
			val img = ResourceManager.image("third-party/DHGZ/sword1.png")
			val startPos = attackFrom
			val endPos = targets match {
				case Left(entities) => entities.head(Physical).position
				case Right(hexes) => hexes.head
			}

			canvas.quad((startPos.qr.asCartesian + endPos.qr.asCartesian) * 0.5f)
				.texture(img, 4)
				.layer(DrawLayer.OverEntity)
				.draw()

			targets match {
				case Left(entities) =>
					for (e <- entities) {
						canvas.quad(e(Physical).position)
   						.texture(ResourceManager.image("third-party/DHGZ/targetIcon.png"),4)
   						.layer(DrawLayer.OverEntity)
   						.draw()
					}
				case Right(hexes) =>
					for (h <- hexes) {
						canvas.quad(h)
   						.texture(ResourceManager.image("third-party/zeshioModified/ui/hex_selection.png"))
   						.color(RGBA(0.7f,0.1f,0.1f,0.8f))
   						.hexBottomOrigin()
   						.layer(DrawLayer.UnderEntity)
   						.draw()
					}
			}
	}
}


trait TacticalSelectorRenderer {
	def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any])
}

class HexSelectorRenderer extends TacticalSelectorRenderer {
	override def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any]) = selector pmatch {
		case HexSelector(pattern, hexPredicate) =>
			selected.foreach {
				case a: AxialVec3 => canvas.quad(a)
					.layer(DrawLayer.UnderEntity)
					.texture("third-party/DHGZ/frame11.png")
					.draw()
				case other => Noto.warn(s"Unexpected selected value for hex selector $other")
			}
		case BiasedHexSelector(pattern, hexPredicate) =>
			selected.foreach {
				case a: BiasedAxialVec3 => canvas.quad(a.vec)
					.layer(DrawLayer.UnderEntity)
					.texture("third-party/DHGZ/frame11.png")
					.draw()
				case other => Noto.warn(s"Unexpected selected value for hex selector $other")
			}
	}
}


trait TacticalSelectableRenderer {
	def renderSelectable(game : HypotheticalWorldView, display : World, entity : Entity, canvas : AxCanvas, selectable : Selectable, selectionResults : SelectionResult)

	def updateUI(game: WorldView, display: World, entity : Entity, selectable : Selectable, selectionResult: SelectionResult, desktop: Widget): Unit = {}
	def update(game: WorldView, display: World, entity : Entity, selectable : Selectable, selectionResult: SelectionResult): Unit = {}
}

object CompoundSelectableRenderer extends TacticalSelectableRenderer  {
	lazy val otherRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalSelectableRenderer].filterNot(_ == CompoundSelectableRenderer)

	override def renderSelectable(game: HypotheticalWorldView, display: World, entity : Entity, canvas: AxCanvas, selectable: Selectable, selectionResults: SelectionResult): Unit = {
		selectable match {
			case compound : CompoundSelectable =>
				for (subSel <- compound.subSelectables(game) ; renderer <- otherRenderers) {
					renderer.renderSelectable(game, display, entity, canvas, subSel, selectionResults)
				}
			case _ =>
		}
	}

//	override def updateUI(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectionResult: SelectionResult, desktop: Widget): Unit = {
//		selectable match {
//			case compound : CompoundSelectable =>
//				for (subSel <- compound.subSelectables(game) ; renderer <- otherRenderers) {
//					renderer.updateUI(game, display, entity, subSel, selectionResult, desktop)
//				}
//			case _ =>
//		}
//	}
//
//	override def update(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectionResult: SelectionResult): Unit = {
//		selectable match {
//			case compound : CompoundSelectable =>
//				for (subSel <- compound.subSelectables(game) ; renderer <- otherRenderers) {
//					renderer.update(game, display, entity, subSel, selectionResult)
//				}
//			case _ =>
//		}
//	}
}

object AttackCardEffectRenderer extends TacticalSelectableRenderer {
	var widgets : Map[Selectable,Map[Entity,Widget]] = Map()

	override def renderSelectable(game: HypotheticalWorldView, display: World, entity : Entity, canvas: AxCanvas, selectable: Selectable, selectionResults: SelectionResult): Unit = {
		selectable match {
			case ce @ AttackCardEffect(attackRef) =>
				attackRef.resolve()(game) match {
					case Some(attack) =>
						implicit val view = game
						val targetSel = ce.targetSelector(entity, attack)
						if (selectionResults.fullySatisfied(targetSel)) {
							val attackFrom = entity(Physical).position
							val targets = selectionResults(targetSel)

							val img = ResourceManager.image("third-party/DHGZ/sword1.png")
							val startPos = attackFrom
							val endPos = targets match {
								case Left(entities) => entities.head(Physical).position
								case Right(hexes) => hexes.head.vec
							}

							canvas.quad((startPos.qr.asCartesian + endPos.qr.asCartesian) * 0.5f)
								.texture(img, 4)
								.layer(DrawLayer.OverEntity)
								.draw()

							targets match {
								case Left(entities) =>
									for (e <- entities) {
										canvas.quad(e(Physical).position)
											.texture(ResourceManager.image("third-party/DHGZ/targetIcon.png"),4)
											.layer(DrawLayer.OverEntity)
											.draw()
									}
								case Right(hexes) =>
									for (h <- hexes) {
										canvas.quad(h.vec)
											.texture(ResourceManager.image("third-party/zeshioModified/ui/hex_selection.png"))
											.color(RGBA(0.7f,0.1f,0.1f,0.8f))
											.hexBottomOrigin()
											.layer(DrawLayer.UnderEntity)
											.draw()
									}
							}
						}
					case None =>
						Noto.warn(s"Could not resolve attack reference to render card effect : $attackRef")
				}
			case _ => // do nothing
		}
	}

	override def updateUI(game: WorldView, display: World, entity : Entity, selectable : Selectable, selectionResult: SelectionResult, desktop: Widget): Unit = {
		implicit val view = game

		val allSelectables = (selectable match {
			case compound : CompoundSelectable => compound.subSelectables(game)
			case other => List(other)
		}).toSeq

		for (selectable <- allSelectables) {
			selectable match {
				case ace@AttackCardEffect(attackRef) =>
					for (attack <- attackRef.resolve()) {
						val targetSel = ace.targetSelector(entity, attack)
						val targets = selectionResult(targetSel)
						val targetEntities = CombatLogic.targetedEntities(targets)

						val existingWidgets = widgets.getOrElse(selectable, Map())


						val widgetsByTarget = for ((target, strikes) <- CombatLogic.effectiveAttackData(entity, entity(Physical).position, targetEntities, attackRef).strikesByTarget) yield {
							existingWidgets.get(target) match {
								case Some(w) => target -> w
								case None =>
									val w = desktop.createChild("AttackInfoWidgets.ConsideredAttackInfo")

									w.bind("attack", AttackPreviewData(strikes.head.attackData.name, strikes.head.attackData.accuracyBonus.toSignedString, DamageExpression(strikes.head.attackData.damage),
										target(CharacterInfo).health.currentValue, strikes.head.defenseData.dodgeBonus.toSignedString, Taxonomy("DefenseBonus")))

									target -> w.widget
							}
						}

						// destroy all existing widgets that correspond to entities that are no longer targets
						existingWidgets.filterKeys(e => !targetEntities.contains(e)).foreach(_._2.destroy())

						widgets += selectable -> widgetsByTarget
					}
				case _ => // do nothing
			}
		}

		for (outdatedMap <- widgets.filterKeys(s => ! allSelectables.contains(s)).values; (_, widget) <- outdatedMap) {
			widget.destroy()
		}
		widgets = widgets.filterKeys(s => allSelectables.contains(s))
	}

	override def update(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectionResult: SelectionResult): Unit = {
		implicit val view = game
		for ((_, widgetsBySelectable) <- widgets; (entity,w) <- widgetsBySelectable) {
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

case object MoveCharacterRenderer extends TacticalSelectableRenderer {
	override def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectionResults: SelectionResult): Unit = {
		selectable match {
			case MoveCharacter =>
				val selector = MoveCharacter.pathSelector(entity)
				if (selectionResults.fullySatisfied(selector)) {
					val path = selectionResults.single(selector)
					if (path.steps.size > 1) {
						implicit val view = game
						val img = ResourceManager.image("third-party/wesnoth/feet.png")
						val startPos = path.steps.head.node
						var prev = startPos

						for (PathStep(hex, cost) <- path.steps; if hex != startPos) {
							val stepDir = (hex.asCartesian(1.0f) - prev.asCartesian(1.0f)).normalize
							val ortho = stepDir.cross(Vec3f.UnitZ)
							canvas.quad(hex)
								.texture(img)
								.dimensions(img.width * 2, img.height * 2)
								.layer(DrawLayer.UnderEntity)
								.forward(stepDir.xy.normalizeSafe)
								.ortho(ortho.xy.normalizeSafe)
								.draw()
							prev = hex
						}
					} else {
						Noto.warn(s"trying to preview degenerate path : $path")
					}
				}
			case _ => // do nothing
		}
	}
}