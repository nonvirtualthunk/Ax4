package arx.ax4.graphics.components.subcomponents

import arx.ai.search.PathStep
import arx.application.Noto
import arx.ax4.game.action.{AttackAction, BiasedAxialVec3, BiasedHexSelector, CompoundSelectable, CompoundSelectableInstance, GameAction, HexSelector, MoveAction, MoveCharacter, MoveCharacterInstance, ResourceGatherSelector, Selectable, SelectableInstance, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, ResourceSourceData}
import arx.ax4.graphics.components.{AttackPreviewData, AxCanvas, DrawLayer}
import arx.core.vec.{Vec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.resource.ResourceManager
import arx.Prelude._
import arx.ax4.control.components.{ActionSelectionContext, DamageExpression, ResourceSelectionInfo}
import arx.ax4.control.event.SelectionMadeEvent
import arx.ax4.game.entities.{ResourceSourceData, Tiles}
import arx.ax4.game.entities.cardeffects.{AttackCardEffect, AttackCardEffectInstance, GatherCardEffect}
import arx.ax4.game.logic.{CharacterLogic, CombatLogic, GatherLogic}
import arx.ax4.graphics.data.{AxGraphicsData, CullingData, SpriteLibrary, TacticalUIData}
import arx.ax4.graphics.logic.GameWidgetLogic
import arx.core.introspection.ReflectionAssistant
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.{ListItemSelected, PositionExpression, TopLeft, TopRight}
import arx.engine.data.{Moddable, TMutableAuxData, TWorldAuxData}
import arx.engine.entity.{Entity, Taxonomy}
import arx.graphics.{GL, ScaledImage}
import arx.graphics.helpers.{Color, RGBA}

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
	def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any]): Unit
}

class HexSelectorRenderer extends TacticalSelectorRenderer {
	override def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any]): Unit = selector pmatch {
		case hs : HexSelector =>
			selected.foreach {
				case a: AxialVec3 => canvas.quad(a)
					.layer(DrawLayer.UnderEntity)
					.texture("third-party/DHGZ/frame11.png")
					.draw()
				case other => Noto.warn(s"Unexpected selected value for hex selector $other")
			}
		case BiasedHexSelector(pattern, hexPredicate, selectable) =>
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
	def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectableInst: SelectableInstance, selectionResults: SelectionResult): Unit

	def updateUI(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, consideredSelectionResult: SelectionResult, activeSelectionResults: SelectionResult, desktop: Widget): Unit = {}
	def update(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, selectionResult: SelectionResult): Unit = {}
}

object CompoundSelectableRenderer extends TacticalSelectableRenderer  {
	lazy val otherRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalSelectableRenderer].filterNot(_ == CompoundSelectableRenderer)

	override def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectableInst: SelectableInstance, selectionResults: SelectionResult): Unit = {
		selectableInst match {
			case compound : CompoundSelectableInstance =>
				for ((subSel, subSelInst) <- compound.subSelectableInstances ; renderer <- otherRenderers) {
					renderer.renderSelectable(game, display, entity, canvas, subSel, subSelInst, selectionResults)
				}
			case _ =>
		}
	}

	override def updateUI(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, consideredSelectionResult: SelectionResult, activeSelectionResults: SelectionResult, desktop: Widget): Unit = {
		selectableInst match {
			case compound : CompoundSelectableInstance =>
				for ((subSel, subSelInst) <- compound.subSelectableInstances ; renderer <- otherRenderers) {
					renderer.updateUI(game, display, entity, subSel, subSelInst, consideredSelectionResult, activeSelectionResults, desktop)
				}
			case _ =>
		}
	}

	override def update(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, selectionResult: SelectionResult): Unit = {
		selectableInst match {
			case compound : CompoundSelectableInstance =>
				for ((subSel, subSelInst) <- compound.subSelectableInstances ; renderer <- otherRenderers) {
					renderer.update(game, display, entity, subSel, subSelInst, selectionResult)
				}
			case _ =>
		}
	}
}

object AttackCardEffectRenderer extends TacticalSelectableRenderer {
	var widgets : Map[SelectableInstance,Map[Entity,Widget]] = Map()

	override def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectableInst: SelectableInstance, selectionResults: SelectionResult): Unit = {
		selectableInst match {
			case ce @ AttackCardEffectInstance(attacker, attackRef, attack, _) =>
				implicit val view = game

				val targetSel = ce.targetSelector
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
									.texture(ResourceManager.image("third-party/DHGZ/targetIcon.png"), 4)
									.layer(DrawLayer.OverEntity)
									.draw()
							}
						case Right(hexes) =>
							for (h <- hexes) {
								canvas.quad(h.vec)
									.texture(ResourceManager.image("third-party/zeshioModified/ui/hex_selection.png"))
									.color(RGBA(0.7f, 0.1f, 0.1f, 0.8f))
									.hexBottomOrigin()
									.layer(DrawLayer.UnderEntity)
									.draw()
							}
					}
				}
			case _ => // do nothing
		}
	}

	override def updateUI(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, consideredSelectionResult: SelectionResult, activeSelectionResults: SelectionResult, desktop: Widget): Unit = {
		implicit val view = game

		val allSelectablePairs = selectableInst match {
			case compound : CompoundSelectableInstance => compound.subSelectableInstances
			case other => List(selectable -> other)
		}

		for ((selectable, selectableInst) <- allSelectablePairs) {
			selectableInst match {
				case ace@AttackCardEffectInstance(attacker, attackRef, attack, _) =>
					val targetSel = ace.targetSelector
					val targets = consideredSelectionResult(targetSel)
					val targetEntities = CombatLogic.targetedEntities(targets)

					val existingWidgets = widgets.getOrElse(selectableInst, Map())


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

					widgets += selectableInst -> widgetsByTarget
				case _ => // do nothing
			}
		}

		val allSelectableInstances = allSelectablePairs.map(_._2).toSet
		for (outdatedMap <- widgets.filterKeys(s => ! allSelectableInstances.contains(s)).values; (_, widget) <- outdatedMap) {
			widget.destroy()
		}
		widgets = widgets.filterKeys(s => allSelectableInstances.contains(s))
	}

	override def update(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, selectionResult: SelectionResult): Unit = {
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
	override def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectableInst: SelectableInstance, selectionResults: SelectionResult): Unit = {
		selectableInst match {
			case mce @ MoveCharacterInstance(_) =>
				val selector = mce.pathSelector
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

case object GatherEffectRenderer extends TacticalSelectableRenderer {
	override def renderSelectable(game: HypotheticalWorldView, display: World, entity: Entity, canvas: AxCanvas, selectable: Selectable, selectableInst: SelectableInstance, selectionResults: SelectionResult): Unit = {
		implicit val view = game
		selectable match {
			case GatherCardEffect(range) =>
				for (selC <- display[TacticalUIData].selectedCharacter) {
					for (r <- 0 to range ; hex <- HexRingIterator(selC(Physical).position, r)) {
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
			case _ => // do nothing
		}
	}

	override def updateUI(game: WorldView, display: World, entity: Entity, selectable: Selectable, selectableInst: SelectableInstance, consideredSelectionResult: SelectionResult, activeSelectionResults: SelectionResult, desktop: Widget): Unit = {
		implicit  val view = game

		val tuid = display[TacticalUIData]
		val rsdd = display[ResourceSelectorDisplayData]
		val rsrcW = rsdd.resourceSelectorWidget match {
			case null =>
				val newW = tuid.mainSectionWidget.createChild("ResourceSelectionWidgets.ResourceSelectionWidget")
				newW.onEvent {
					case ListItemSelected(_, _, Some(data: ResourceSelectionInfo)) =>
						newW.handleEvent(SelectionMadeEvent(rsdd.activeSelector, data.prospect, 1))
				}
				newW
			case other => other
		}
		rsdd.resourceSelectorWidget = rsrcW

		var shouldShow = false
		selectableInst.nextSelector(activeSelectionResults) match {
			case Some(rgs@ResourceGatherSelector(resources, _)) =>
				rsdd.activeSelector = rgs
				rsrcW.bind("possibleResources", () => {
					resources.map(p => {
						val (textColor, iconColor) = p.toGatherProspect(game) match {
							case Some(prospect) if GatherLogic.canGather(prospect) => (Color.Black, Color.White)
							case _ => (RGBA(0.1f, 0.1f, 0.1f, 1.0f), Color.Grey)
						}
						val disabledReason = GatherLogic.cantGatherReason(p).map("[" + _.toLowerCase + "]").getOrElse("")
						val remaining = p.target[ResourceSourceData].resources(p.key).amount.currentValue
						ResourceSelectionInfo(p, p.method, p.key.kind.name, ScaledImage.scaleToPixelWidth(SpriteLibrary.iconFor(p.key.kind), 64), p.method.name, p.method.amount, remaining, textColor, iconColor, disabledReason)
					})
				})
				shouldShow = true
			case _ =>
		}

		rsrcW.showing = Moddable(shouldShow)
	}
}

//case object ResourceSelectorRenderer extends TacticalSelectorRenderer {
//	override def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any]): Unit = {
//		val tuid = display[TacticalUIData]
//		val rsdd = display[ResourceSelectorDisplayData]
//		val rsrcW = rsdd.resourceSelectorWidget match {
//			case null => tuid.mainSectionWidget.createChild("ResourceSelectionWidgets.ResourceSelectionWidget")
//			case other => other
//		}
//
//		selector match {
//
//		}
//
//		rsrcW.bind("possibleResources", () => {
//			resources.map(p => {
//				val (textColor, iconColor) = p.toGatherProspect(gameView) match {
//					case Some(prospect) if GatherLogic.canGather(prospect) => (Color.Black, Color.White)
//					case _ => (RGBA(0.1f, 0.1f, 0.1f, 1.0f), Color.Grey)
//				}
//				val disabledReason = GatherLogic.cantGatherReason(p).map("[" + _.toLowerCase + "]").getOrElse("")
//				val remaining = p.target[ResourceSourceData].resources(p.key).amount.currentValue
//				ResourceSelectionInfo(p, p.method, p.key.kind.name, ScaledImage.scaleToPixelWidth(SpriteLibrary.iconFor(p.key.kind), 64), p.method.name, p.method.amount, remaining, textColor, iconColor, disabledReason)
//			})
//		})
//		rsrcW.onEvent {
//			case ListItemSelected(_, _, Some(data: ResourceSelectionInfo)) =>
//				if (makeSelection(game, display, sel, data.prospect)) {
//					rsrcW.destroy()
//				}
//		}
//		selectionWidgets += sel -> rsrcW
//	}
//}

class ResourceSelectorDisplayData extends AxGraphicsData with TMutableAuxData with TWorldAuxData {
	var resourceSelectorWidget : Widget = _
	var activeSelector : Selector[_] = _
}