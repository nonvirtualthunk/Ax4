package arx.ax4.game.event

import arx.ax4.game.action.GameAction
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.event.GameEvent

case class EntityMoved(entity : Entity, from : AxialVec3, to : AxialVec3) extends GameEvent
case class EntityPlaced(entity : Entity, at : AxialVec3) extends GameEvent
case class ActionTaken(action : GameAction) extends GameEvent
case class MovePointsGained(entity : Entity, mp : Int) extends GameEvent