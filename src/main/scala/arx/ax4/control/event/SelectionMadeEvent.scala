package arx.ax4.control.event

import arx.ax4.game.action.Selector
import arx.engine.control.event.ControlEvent

case class SelectionMadeEvent(selector : Selector[_], value : Any, amount : Int) extends ControlEvent {

}
