package arx.ax4.game.logic

import arx.ai.search.{Path, Pathfinder}
import arx.core.vec.coordinates.AxialVec3
import arx.Prelude._
import arx.ax4.game.entities.{Physical, Terrain, Tile, Vegetation}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object AxPathfinder {
	protected val pathfinder = memoize((view: WorldView) => {
		val terrainData = view.dataStore[Terrain]
		val tileData = view.dataStore[Tile]
		val physicalData = view.dataStore[Physical]
		val vegData = view.dataStore[Vegetation]

		Pathfinder[AxialVec3](
			"AxMainPathfinder",
			v => v.neighbors,
			(e, from, to) => MovementLogic.moveCostTo(e, to, tileData, terrainData, vegData, physicalData).map(_.asFloat),
			(from, to) => from.distance(to)
		)
	})

	protected val noHeuristicPathfinder = memoize((view: WorldView) => {
		val terrainData = view.dataStore[Terrain]
		val tileData = view.dataStore[Tile]
		val physicalData = view.dataStore[Physical]
		val vegData = view.dataStore[Vegetation]

		Pathfinder[AxialVec3](
			"AxMainPathfinder",
			v => v.neighbors,
			(e, from, to) => MovementLogic.moveCostTo(e, to, tileData, terrainData, vegData, physicalData).map(_.asFloat),
			(from, to) => 0.0f
		)
	})


	def findPath(entity : Entity, from : AxialVec3, to : AxialVec3)(implicit view : WorldView) : Option[Path[AxialVec3]] = {
		findPath(entity, from, to :: Nil)
	}

	def findPath(entity : Entity, from : AxialVec3, to : List[AxialVec3])(implicit view : WorldView) : Option[Path[AxialVec3]] = {
		pathfinder(view).findPathToAny(entity, from, to, Some(1000.0f))
	}

	def findPathToMatching(entity : Entity, from : AxialVec3, to : AxialVec3 => Boolean)(implicit view : WorldView) : Option[Path[AxialVec3]] = {
		noHeuristicPathfinder(view).findPathToMatching(entity, from, from, to)
	}


}
