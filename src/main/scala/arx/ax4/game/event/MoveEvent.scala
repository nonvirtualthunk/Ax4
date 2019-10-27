package arx.ax4.game.event

import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.event.GameEvent

case class MoveEvent(entity : Entity, from : AxialVec3, to : AxialVec3) extends GameEvent {

}
