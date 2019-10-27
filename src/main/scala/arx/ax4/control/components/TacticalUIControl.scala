package arx.ax4.control.components

import arx.ai.search.Pathfinder
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile}
import arx.ax4.game.entities.{CharacterInfo, Physical, Tile, Tiles}
import arx.ax4.game.event.MoveEvent
import arx.ax4.graphics.components.TacticalUIData
import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3}
import arx.engine.control.components.ControlComponent
import arx.engine.control.event.{KeyModifiers, MouseButton, MousePressEvent, UIEvent}
import arx.engine.graphics.data.PovData
import arx.engine.world.World
import arx.graphics.GL
import arx.Prelude.toArxList

class TacticalUIControl extends ControlComponent {
	import arx.core.introspection.FieldOperations._

	override protected def onUpdate(game: World, graphics: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(game: World, display: World): Unit = {
		implicit val view = game.view

		onControlEvent {
			case MousePressEvent(button, pos, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val pressedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				fireEvent(HexPressEvent(button,pressedHex,pos,modifiers))
			case HexPressEvent(button, hex, pos, modifiers) =>
				val tileEnt = Tiles.tileAt(hex)
				val tuid = display[TacticalUIData]
				for (tile <- game.view.dataOpt[Tile](tileEnt)) {
					tile.entities.find(e => e.hasData[CharacterInfo]) match {
						case Some(ent) => tuid.selectedCharacter = Some(ent)
						case None => {
							for (selC <- tuid.selectedCharacter) {
								val finalTo = AxialVec3(hex,0)
								val startingFrom = selC[Physical].position

								val pathfinder = Pathfinder[AxialVec3]("tactical move pathfinder",
									v => v.neighbors,
									(e, from, to) => Some(to.distance(from)),
									(from, to) => from.distance(to))

								pathfinder.findPathTo(selC, startingFrom, finalTo) match {
									case Some(path) =>
										for ((from, to) <- path.steps.sliding2) {
											game.startEvent(MoveEvent(selC, from.node, to.node))
											game.modify(selC, Physical.position -> to.node, None)
											game.modify(Tiles.tileAt(from.node), Tile.entities - selC, None)
											game.modify(Tiles.tileAt(to.node), Tile.entities + selC, None)
											game.endEvent(MoveEvent(selC, from.node, to.node))
										}
									case None => Noto.warn("no path")
								}
							}
						}
					}
				}
		}
	}
}


case class HexPressEvent(button : MouseButton, hex : AxialVec, pos : ReadVec2f, modifiers : KeyModifiers) extends UIEvent