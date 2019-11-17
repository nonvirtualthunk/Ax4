package arx.ax4.control.components
import arx.application.Noto
import arx.ax4.game.action.{GameActionIntent, GameActionIntentInstance, GatherSelectionProspect, MoveIntent, ResourceGatherSelector, SelectionResultBuilder, Selector}
import arx.ax4.game.entities.{AllegianceData, CharacterInfo, ResourceSourceData, Tiles, TurnData}
import arx.ax4.game.logic.{ActionLogic, GatherLogic, IdentityLogic}
import arx.ax4.graphics.data.{SpriteLibrary, TacticalUIData}
import arx.core.CachedKeyedValue
import arx.core.datastructures.Watcher
import arx.core.units.UnitOfTime
import arx.core.vec.coordinates.AxialVec3
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.ListItemSelected
import arx.engine.control.event.KeyReleaseEvent
import arx.engine.data.Moddable
import arx.engine.entity.Entity
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World}
import arx.graphics.helpers.{Color, RGBA}
import arx.graphics.{Image, ScaledImage}
import org.lwjgl.glfw.GLFW

class TacticalUIActionControl(mainControl : TacticalUIControl) extends AxControlComponent {
	import TacticalUIActionControl._

	var watcher : Watcher[(AxialVec3, Int, GameEventClock, Option[ActionSelectionContext])] = _

	var selectionContext = new CachedKeyedValue[ActionSelectionCacheKey, ActionSelectionContext]

	var lastSelectionContextKey : Option[ActionSelectionCacheKey] = None
	var lastSelector : Option[Selector[_]] = None

	var selectingResource = false

	var selectionWidgets : Set[Widget] = Set()

	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		if (watcher.hasChanged || gameView.currentTime != game.currentTime) {
			updateConsideredActions(gameView, game, display)
		}
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root

		val tuid = display[TacticalUIData]
		watcher = Watcher((tuid.mousedOverHex, tuid.mousedOverHexBiasDir, gameView.currentTime, tuid.actionSelectionContext))


		onControlEvent {
			case HexMousePressEvent(button, hex, pos, modifiers) =>
				val tuid = display[TacticalUIData]
				for (sel <- tuid.consideringSelection) {
					mainControl.selectCharcter(game, display, sel)
				}

				for (asc <- tuid.consideringActionSelectionContext) {
					updateActionSelectionContext(game, display, asc)
				}
			case KeyReleaseEvent(GLFW.GLFW_KEY_ESCAPE, _) =>
				tuid.actionSelectionContext match {
					case Some(ActionSelectionContext(intent, selectionResults)) if !selectionResults.isEmpty =>
						tuid.actionSelectionContext = Some(ActionSelectionContext(intent, SelectionResultBuilder()))
						tuid.consideringActionSelectionContext = None

						selectionWidgets.foreach(_.destroy())
						selectionWidgets = Set()
						true
					case _ => false
				}
		}
//		val rsrcSelW =

	}

	def updateActionSelectionContext(game : World, display : World, asc : ActionSelectionContext): Unit = {
		val tuid = display[TacticalUIData]

		val ActionSelectionContext(intent, selectionResults) = asc
		if (! intent.hasRemainingSelections(selectionResults)) {
			for (action <- intent.createAction(selectionResults.build())) {
				ActionLogic.performAction(action)(game)
			}
		} else {
			// lock in the considered value here when we press
			tuid.actionSelectionContext = Some(asc)
		}
	}

	def updateConsideredActions(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root // take the non-hypothetical, but still slowly advancing world view
		val tuid = display[TacticalUIData]
		import tuid._

		var newConsideredActionSelectionContext : Option[ActionSelectionContext] = None
		var newPossibleSelection : Option[Entity] = None

		val activeFaction = gameView.worldData[TurnData].activeFaction
		selectedCharacter match {
			case Some(selC) =>
				val activeIntent = selC[CharacterInfo].activeIntent
				val selKey = ActionSelectionCacheKey(selC, activeIntent, gameView.currentTime)

				// set up the fixed selection context. That is, what has actually been selected as opposed to what is being considered as a next possible
				// selection
				if (!lastSelectionContextKey.contains(selKey) || tuid.actionSelectionContext.isEmpty) {
					tuid.actionSelectionContext = activeIntent.instantiate(gameView, selC) match {
						case Left(intentInstance) =>
							Some(ActionSelectionContext(intentInstance, SelectionResultBuilder()))
						case Right(error) =>
							Noto.error(s"Could not instantiate selection context: $error")
							None
					}
					lastSelectionContextKey = Some(selKey)
				}

				actionSelectionContext match {
					case Some(ActionSelectionContext(intent, selectionResults)) =>
						var selectionIdentified = false
						val nextSel = intent.nextSelection(selectionResults)
						nextSel match {
							case Some(sel) =>
								sel match {
									case rgs @ ResourceGatherSelector(resources) if ! lastSelector.contains(rgs) =>
										val rsrcW = tuid.mainSectionWidget.createChild("ResourceSelectionWidgets.ResourceSelectionWidget")

										rsrcW.bind("possibleResources", () => {
											resources.map(p => {
												val (textColor, iconColor) = p.toGatherProspect(view) match {
													case Some(prospect) if GatherLogic.canGather(prospect) => (Color.Black, Color.White)
													case _ => (RGBA(0.55f,0.1f,0.1f,1.0f), Color.Grey)
												}
												val disabledReason = GatherLogic.cantGatherReason(p).map("[" + _.toLowerCase + "]").getOrElse("")
												val remaining = p.target[ResourceSourceData].resources(p.key).amount.currentValue
												ResourceSelectionInfo(p, p.key.kind.name, ScaledImage.scaleToPixelWidth(SpriteLibrary.iconFor(p.key.kind), 64), p.method.name, p.method.amount, remaining, textColor, iconColor, disabledReason)
											})
										})
										rsrcW.onEvent {
											case ListItemSelected(_,_,Some(data : ResourceSelectionInfo)) =>
												rgs.satisfiedBy(view, data.prospect) match {
													case Some((value, amount)) =>
														val newAsc = ActionSelectionContext(intent, selectionResults.addResult(rgs, value, amount))
														updateActionSelectionContext(game, display, newAsc)
														rsrcW.destroy()
													case None => Noto.info("picked unpickable resource selection")
												}
										}
										selectionWidgets += rsrcW
									case _ =>
										val toConsiderForSelection : List[Any] = Tiles.entitiesOnTile(mousedOverHex).toList ::: mousedOverBiasedHex :: mousedOverHex :: Nil
										for (thing <- toConsiderForSelection; if ! selectionIdentified) {
											val consideredSelectionResults = sel.satisfiedBy(view, thing) match {
												case Some((satisfyingThing, amount)) => selectionResults.addResult(sel, satisfyingThing, amount)
												case _ => selectionResults
											}
											if (consideredSelectionResults != selectionResults) {
												selectionIdentified = true
												updateConsideringActionSelectionContext(tuid, ActionSelectionContext(intent, consideredSelectionResults))
											}
										}
								}
							case None => // do nothing, fully selected already
						}
						lastSelector = nextSel



						// if we haven't identified anything, see if this can be a character switch
						if (! selectionIdentified) {
							for (mousedEnt <- Tiles.entitiesOnTile(mousedOverHex)) {
								if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction)) {
									newPossibleSelection = Some(mousedEnt)
									selectionIdentified = true
								}
							}
						}
						// if we still haven't identified anything, see if this can be a simple move. Only consider this if we're not in the middle
						// of selecting something more complicated
						if (!selectionIdentified && (actionSelectionContext.isEmpty || actionSelectionContext.exists(asc => asc.selectionResults.isEmpty))) {
							MoveIntent.instantiate(view, selC) match {
								case Left(moveIntent) =>
									var selectionResultBuilder = SelectionResultBuilder()
									for (thing <- List(mousedOverBiasedHex, mousedOverHex); sel <- moveIntent.nextSelection(selectionResultBuilder)) {
										sel.satisfiedBy(view, thing) match {
											case Some((value, amount)) => selectionResultBuilder = selectionResultBuilder.addResult(sel, value, amount)
											case None => // do nothing
										}
									}
									if (moveIntent.nextSelection(selectionResultBuilder).isEmpty) {
										updateConsideringActionSelectionContext(tuid, ActionSelectionContext(moveIntent, selectionResultBuilder))
									}
								case Right(_) => // do nothing
							}
						}
					case None => // if we can't set up a context for whatever reason, fall back on allowing switching active character
						for (mousedEnt <- Tiles.entitiesOnTile(mousedOverHex)) {
							if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction)) {
								newPossibleSelection = Some(mousedEnt)
							}
						}
				}
			case None =>
				tuid.actionSelectionContext = None
		}

		tuid.consideringSelection = newPossibleSelection
	}


	def updateConsideringActionSelectionContext(tuid : TacticalUIData, asc : ActionSelectionContext): Unit = {
		tuid.consideringActionSelectionContext = Some(asc)
	}
}

object TacticalUIActionControl {
	case class ActionSelectionCacheKey(entity : Entity, intent : GameActionIntent, time : GameEventClock)

}

case class ActionSelectionContext(intent : GameActionIntentInstance, selectionResults: SelectionResultBuilder)


case class ResourceSelectionInfo(prospect : GatherSelectionProspect, resourceName : String, resourceIcon : ScaledImage, methodName : String, amount : Int, remainingAmount : Int, fontColor : Color, iconColor : Color, disabledReason : String)