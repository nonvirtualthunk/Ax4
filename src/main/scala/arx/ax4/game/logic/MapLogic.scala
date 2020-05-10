package arx.ax4.game.logic

import arx.ax4.game.entities.Tiles
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object MapLogic {

  def characterOnTile(hex : AxialVec3)(implicit view : WorldView) : Option[Entity] = {
    Tiles.characterOnTile(hex)
  }


}
