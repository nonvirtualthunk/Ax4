package arx.ax4.control.components

import arx.ai.search.Pathfinder
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile, TurnData}
import arx.ax4.game.entities.{CharacterInfo, FactionData, Physical, Tile, Tiles, TurnData}
import arx.ax4.game.event.EntityMoved
import arx.ax4.graphics.components.{EntityGraphics, TacticalUIData}
import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3}
import arx.engine.control.components.ControlComponent
import arx.engine.control.event.{KeyModifiers, KeyPressEvent, KeyReleaseEvent, MouseButton, MouseMoveEvent, MousePressEvent, UIEvent}
import arx.engine.graphics.data.PovData
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World, WorldView}
import arx.graphics.GL
import arx.Prelude.toArxList
import arx.ax4.game.action.GameAction
import arx.ax4.game.event.TurnEvents.{TurnEndedEvent, TurnStartedEvent}
import arx.ax4.game.logic.Action
import arx.core.datastructures.{Watcher, Watcher2}
import arx.core.math.Sext
import arx.engine.control.components.windowing.WindowingControlComponent
import arx.engine.control.data.WindowingControlData
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, IdentityData}
import arx.resource.ResourceManager
import org.lwjgl.glfw.GLFW

class TacticalUIControl(windowing : WindowingControlComponent) extends AxControlComponent {
	import arx.core.introspection.FieldOperations._

	var lastSelected : (Option[Entity], GameEventClock) = (None, GameEventClock(0))


	override def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val view = gameView
		val tuid = display[TacticalUIData]
		val selectedState = (tuid.selectedCharacter, view.currentTime)
		if (selectedState != lastSelected) {
			updateBindings(view, display, tuid.selectedCharacter)
			lastSelected = selectedState
		}
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = game.view

		val desktop = display[WindowingControlData].desktop
		desktop.drawing.drawAsForegroundBorder = true
		desktop.drawing.backgroundImage = Some(ResourceManager.image("ui/woodBorder2.png"))
		val selCInfo = desktop.createChild("widgets/CharacterInfoWidgets.sml", "SelectedCharacterInfo")
		selCInfo.showing = Moddable(() => display[TacticalUIData].selectedCharacter.isDefined)


		onControlEvent {
			case MousePressEvent(button, pos, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val pressedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				fireEvent(HexMousePressEvent(button,pressedHex,pos,modifiers))
			case MouseMoveEvent(pos, delta, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val mousedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				val tuid = display[TacticalUIData]
				tuid.mousedOverHex = AxialVec3(mousedHex,0)
				tuid.mousedOverHexBiasDir = (0 until 6).minBy(q => (mousedHex.neighbor(q).asCartesian(const.HexSize) - unprojected.xy).lengthSafe)

				fireEvent(HexMouseMoveEvent(mousedHex,pos,modifiers))
			case KeyReleaseEvent(GLFW.GLFW_KEY_ENTER, _) =>
				println("Ending turn")
				val TD = game.view.worldData[TurnData]
				val factions = game.view.entitiesWithData[FactionData].toList.sortBy(e => e.id)
				val activeFaction = TD.activeFaction
				val activeIndex = factions.indexOf(activeFaction)
				val nextFaction = factions((activeIndex + 1) % factions.size)
				game.addEvent(TurnEndedEvent(activeFaction, TD.turn))

				if (activeIndex == factions.size - 1) {
					game.modifyWorld(TurnData.turn + 1)
				}

				game.modifyWorld(TurnData.activeFaction -> nextFaction)
				game.addEvent(TurnStartedEvent(nextFaction, TD.turn))
			case HexMousePressEvent(button, hex, pos, modifiers) =>
				val tuid = display[TacticalUIData]
				for (sel <- tuid.consideringSelection) {
					tuid.selectedCharacter = Some(sel)
				}
				for (action <- tuid.consideringActions) {
					Action.performAction(action)(game)
				}
		}
	}

	def updateBindings(implicit game : WorldView, display : World, character : Option[Entity]): Unit = {
		for (selC <- display[TacticalUIData].selectedCharacter) {
			val desktop = display[WindowingControlData].desktop
			desktop.bind("selectedCharacter.name", selC[IdentityData].name.getOrElse("nameless"))
			desktop.bind("selectedCharacter.portrait", () => EntityGraphics.characterImageset.drawInfoFor(game, selC, display.view).head.image)

			desktop.bind("selectedCharacter.health.cur", () => selC[CharacterInfo].health.currentValue)
			desktop.bind("selectedCharacter.health.max", () => selC[CharacterInfo].health.maxValue)

			desktop.bind("selectedCharacter.actions.cur", () => selC[CharacterInfo].actionPoints.currentValue)
			desktop.bind("selectedCharacter.actions.max", () => selC[CharacterInfo].actionPoints.maxValue)

			desktop.bind("selectedCharacter.move.cur", () => selC[CharacterInfo].curPossibleMovePoints)
			desktop.bind("selectedCharacter.move.max", () => selC[CharacterInfo].maxPossibleMovePoints)

			desktop.bind("selectedCharacter.speed", () => selC[CharacterInfo].moveSpeed)
		}
	}
}


case class HexMousePressEvent(button : MouseButton, hex : AxialVec, pos : ReadVec2f, modifiers : KeyModifiers) extends UIEvent
case class HexMouseMoveEvent(hex : AxialVec, pos : ReadVec2f, modifiers: KeyModifiers) extends UIEvent