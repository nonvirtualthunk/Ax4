package arx.ax4.control.components
import arx.Prelude
import arx.application.Noto
import arx.ax4.game.action.{GameActionIntent, GameActionIntentInstance, GatherSelectionProspect, MoveIntent, ResourceGatherSelector, SelectionResult, Selector, SwitchSelectedCharacterAction}
import arx.ax4.game.entities.Companions.{CharacterInfo, DeckData}
import arx.ax4.game.entities.{AllegianceData, CardData, CharacterInfo, DeckData, GatherMethod, ResourceSourceData, Tiles, TurnData}
import arx.ax4.game.event.ActiveIntentChanged
import arx.ax4.game.logic.{ActionLogic, CharacterLogic, GatherLogic, IdentityLogic}
import arx.ax4.graphics.components.GameActionIntentOverlay
import arx.ax4.graphics.data.{SelectionConfirmationMethod, SpriteLibrary, TacticalUIData}
import arx.core.CachedKeyedValue
import arx.core.datastructures.Watcher
import arx.core.introspection.ReflectionAssistant
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2f, Vec2i}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.EngineCore
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.{BottomLeft, ListItemSelected, PositionExpression, TopLeft}
import arx.engine.control.data.WindowingControlData
import arx.engine.control.event.{KeyReleaseEvent, Mouse, MousePressEvent, MouseReleaseEvent}
import arx.engine.data.Moddable
import arx.engine.entity.Entity
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World, WorldView}
import arx.graphics.helpers.{Color, RGBA}
import arx.graphics.{GL, Image, ScaledImage}
import arx.resource.ResourceManager
import org.lwjgl.glfw.GLFW

class TacticalUIActionControl(mainControl : TacticalUIControl) extends AxControlComponent {
	import TacticalUIActionControl._
	import arx.core.introspection.FieldOperations._

	lazy val intentOverlays = ReflectionAssistant.instancesOfSubtypesOf[GameActionIntentOverlay]

	var watcher : Watcher[(AxialVec3, Int, GameEventClock, Option[ActionSelectionContext])] = _

	var selectionContext = new CachedKeyedValue[ActionSelectionCacheKey, ActionSelectionContext]

	var lastSelectionContextKey : Option[ActionSelectionCacheKey] = None
	var lastSelector : Option[Selector[_]] = None

	var selectingResource = false

	var selectionWidgets : Map[Selector[_], Widget] = Map()


	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		if (watcher.hasChanged || gameView.currentTime != game.currentTime) {
			updateActiveContext(gameView, display)
			updateConsideredActions(gameView, game, display)
		}
		val tuid = display[TacticalUIData]
		intentOverlays.foreach(_.update(gameView, display, tuid.consideringActionSelectionContext))
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root

		val tuid = display[TacticalUIData]
		watcher = Watcher((tuid.mousedOverHex, tuid.mousedOverHexBiasDir, gameView.currentTime, tuid.actionSelectionContext))

		onControlEvent {
			case HexMouseReleaseEvent(button, hex, pos, modifiers) =>
				val tuid = display[TacticalUIData]
				for (sel <- tuid.consideringCharacterSwitch) {
					mainControl.selectCharacter(game, display, sel)
				}

				for (asc <- tuid.consideringActionSelectionContext) {
					updateActionSelectionContext(game, display, asc)
				}
			case KeyReleaseEvent(GLFW.GLFW_KEY_ESCAPE, _) =>
				tuid.actionSelectionContext match {
					case Some(ActionSelectionContext(intent, selectionResults)) =>
						(selectionResults.results.keys ++ intent.nextSelection(selectionResults).toList).foreach(sel => onPendingSelectionPopped(gameView, game, display, sel))

						tuid.actionSelectionContext = Some(ActionSelectionContext(intent, SelectionResult()))
						tuid.consideringActionSelectionContext = None

						for (selC <- tuid.selectedCharacter) {
							CharacterLogic.setActiveIntent(selC, selC(CharacterInfo).defaultIntent)(game)
						}
						true
					case _ => false
				}
		}
	}

	def updateActionSelectionContext(game : World, display : World, asc : ActionSelectionContext): Unit = {
		val tuid = display[TacticalUIData]

		val ActionSelectionContext(intent, selectionResults) = asc
		intent.nextSelection(selectionResults) match {
			case None =>
				for (action <- intent.createAction(selectionResults)) {
					action match {
						case SwitchSelectedCharacterAction(from, to) => tuid.selectedCharacter = Some(from)
						case _ => ActionLogic.performAction(action)(game)
					}
				}
				tuid.actionSelectionContext = None
				for (selC <- tuid.selectedCharacter) {
					implicit val view = game.view
					if (selC(CharacterInfo).activeIntent != selC(CharacterInfo).defaultIntent) {
						CharacterLogic.setActiveIntent(selC, selC(CharacterInfo).defaultIntent)(game)
					}
				}
			case Some(nextSel) =>
				// lock in the new value as our current context, then work on the next selector
				tuid.actionSelectionContext = Some(asc)
				onPendingSelectionPushed(game.view, game, display, nextSel)
		}
	}

	def makeSelection(game : World, display : World, sel : Selector[_], value : Any): Boolean = {
		display[TacticalUIData].actionSelectionContext match {
			case Some(ActionSelectionContext(intent, selectionResults)) =>
				sel.satisfiedBy(game.view, value) match {
					case Some((value, amount)) =>
						updateActionSelectionContext(game, display, ActionSelectionContext(intent, selectionResults.addResult(sel, value, amount)))
						true
					case None =>
						Noto.warn("Attempted to make a selection, but the selected value did not actually match the selection")
						false
				}
			case None =>
				Noto.error(s"Attempted to make a selection, but no context is active at this time: $sel, $value")
				false
		}
	}

	def onPendingSelectionPushed(implicit gameView: WorldView, game : World, display: World, sel: Selector[_]): Unit = {
		val tuid = display[TacticalUIData]

		sel match {
			case rgs@ResourceGatherSelector(resources) if !lastSelector.contains(rgs) =>
				val rsrcW = tuid.mainSectionWidget.createChild("ResourceSelectionWidgets.ResourceSelectionWidget")

				rsrcW.bind("possibleResources", () => {
					resources.map(p => {
						val (textColor, iconColor) = p.toGatherProspect(gameView) match {
							case Some(prospect) if GatherLogic.canGather(prospect) => (Color.Black, Color.White)
							case _ => (RGBA(0.1f, 0.1f, 0.1f, 1.0f), Color.Grey)
						}
						val disabledReason = GatherLogic.cantGatherReason(p).map("[" + _.toLowerCase + "]").getOrElse("")
						val remaining = p.target[ResourceSourceData].resources(p.key).amount.currentValue
						ResourceSelectionInfo(p, p.method, p.key.kind.name, ScaledImage.scaleToPixelWidth(SpriteLibrary.iconFor(p.key.kind), 64), p.method.name, p.method.amount, remaining, textColor, iconColor, disabledReason)
					})
				})
				rsrcW.onEvent {
					case ListItemSelected(_, _, Some(data: ResourceSelectionInfo)) =>
						if (makeSelection(game, display, sel, data.prospect)) {
							rsrcW.destroy()
						}
				}
				selectionWidgets += sel -> rsrcW
			case _ =>
		}
	}

	def onPendingSelectionPopped(implicit gameView: WorldView, game : World, display: World, sel: Selector[_]): Unit = {
		for (selW <- selectionWidgets.get(sel)) { selW.destroy() }
		selectionWidgets -= sel
	}

	/**
	 * Updates the action selection context based on the selected character, their active intent and the like
	 */
	def updateActiveContext(implicit gameView : WorldView, display : World): Unit = {
		val tuid = display[TacticalUIData]
		tuid.selectedCharacter match {
			case Some(selC) =>
				val activeIntent = selC[CharacterInfo].activeIntent
				val selKey = ActionSelectionCacheKey(selC, activeIntent, gameView.currentTime)

				// set up the fixed selection context. That is, what has actually been selected as opposed to what is being considered as a next possible
				// selection
				if (!lastSelectionContextKey.contains(selKey) || tuid.actionSelectionContext.isEmpty) {
					tuid.actionSelectionContext = activeIntent.instantiate(gameView, selC) match {
						case Left(intentInstance) => Some(ActionSelectionContext(intentInstance, SelectionResult()))
						case Right(error) =>
							Noto.error(s"Could not instantiate selection context: $error")
							None
					}
					lastSelectionContextKey = Some(selKey)
					tuid.consideringActionSelectionContext = None
				}
			case None =>
				tuid.actionSelectionContext = None
				tuid.consideringActionSelectionContext = None
		}
	}

	/**
	 * Updates considered actions to preview based on the current position of the mouse, etc
	 */
	def updateConsideredActions(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root // take the non-hypothetical, but still slowly advancing world view
		val tuid = display[TacticalUIData]
		import tuid._

		var newPossibleSelection : Option[Entity] = None

		val activeFaction = gameView.worldData[TurnData].activeFaction
		selectedCharacter match {
			case Some(selC) =>
				actionSelectionContext match {
					case Some(ActionSelectionContext(mainIntent, selectionResults)) =>

						var selectionIdentified = false
						for (intent <- Stream(mainIntent) ++ selC(CharacterInfo).fallbackIntents.toStream.map(_.instantiate(view, selC)).collect { case Left(value) => value }) {
							for (sel <- intent.nextSelection(selectionResults)) {
								val toConsiderForSelection: List[Any] = Tiles.entitiesOnTile(mousedOverHex).toList ::: mousedOverBiasedHex :: mousedOverHex :: Nil
								for (thing <- toConsiderForSelection; if !selectionIdentified) yield {
									for ((satisfyingThing, amount) <- sel.satisfiedBy(view, thing)) yield {
										selectionIdentified = true
										tuid.consideringActionSelectionContext = Some(ActionSelectionContext(intent, selectionResults.addResult(sel, satisfyingThing, amount)))
									}
								}
							}
						}
					case None => // if we can't set up a context for whatever reason, fall back on allowing switching active character
						for (mousedEnt <- Tiles.entitiesOnTile(mousedOverHex)) {
							if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction)) {
								newPossibleSelection = Some(mousedEnt)
							}
						}
				}

				intentOverlays.foreach(_.updateUI(gameView, display, consideringActionSelectionContext, display[WindowingControlData].desktop))
			case None =>
				tuid.actionSelectionContext = None
		}


		tuid.consideringCharacterSwitch = newPossibleSelection
	}
}

object TacticalUIActionControl {
	case class ActionSelectionCacheKey(entity : Entity, intent : GameActionIntent, time : GameEventClock)

}

case class ActionSelectionContext(intent : GameActionIntentInstance, selectionResults: SelectionResult) {

	def fullySatisfied = intent.nextSelection(selectionResults).isEmpty
	def completeAction = if (fullySatisfied) {
		Some(intent.createAction(selectionResults))
	} else {
		None
	}
}


case class ResourceSelectionInfo(prospect : GatherSelectionProspect, method : GatherMethod, resourceName : String, resourceIcon : ScaledImage, methodName : String, amount : Int, remainingAmount : Int, fontColor : Color, iconColor : Color, disabledReason : String)