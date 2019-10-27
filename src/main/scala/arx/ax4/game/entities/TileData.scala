package arx.ax4.game.entities

import arx.Prelude.toArxIterable
import arx.core.introspection.TEagerSingleton
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.{AxialVec, AxialVec3}
import arx.engine.data.{ConfigDataLoader, ConfigLoadable, TNestedData}
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.WorldView
import arx.resource.ResourceManager

@GenerateCompanion
class Tile extends AxAuxData {
	var entities = Set[Entity]()
	var position = AxialVec.Zero
}

object Tiles {
	def tileAt(v : AxialVec, z : Int = 0) : Entity = new Entity(tileEntityId(v.q,v.r,z))
	def tileAt(v : AxialVec3) : Entity = new Entity(tileEntityId(v.q, v.r, v.l))
	def tileAt(q : Int, r : Int, z : Int) : Entity = new Entity(tileEntityId(q,r,z))
	def tileAt(q : Int, r : Int) : Entity = new Entity(tileEntityId(q,r,0))

	// 32 z levels, 4096 y, 4096 x
	// 5 bits, 12 bits, 12 bits
	//[0 - normal entities - 31][32 - tiles - 60][61 - unused - 63]
	/** Compute the entity id to use for the tile at the given x/y/(z) coordinates */
	def tileEntityId(x : Int, y : Int, z : Int = 0) = (1 << 32L) + ((z + 16) << 17) + ((y + 2048) << 12) + (x + 2048)

	def characterOnTile(v : AxialVec3)(implicit world : WorldView) : Option[Entity] = {
		val tileEnt = tileAt(v)
		val tile = tileEnt[Tile]
		tile.entities.find(e => {
			e.hasData[CharacterInfo]
		})
	}
}

