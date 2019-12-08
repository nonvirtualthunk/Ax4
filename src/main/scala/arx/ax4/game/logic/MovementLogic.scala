package arx.ax4.game.logic

import arx.ai.search.{Path, PathStep}
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile}
import arx.ax4.game.entities.{CharacterInfo, Physical, Terrain, Tile, Tiles, Vegetation}
import arx.core.math.Sext
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.world.{EntityDataStore, TEntityDataStore, World, WorldView}
import arx.Prelude.toArxList
import arx.ax4.game.event.{EntityMoved, EntityPlaced}

object MovementLogic {
	import arx.core.introspection.FieldOperations._

	def moveCostTo(character : Entity, position : AxialVec3, tileStore : TEntityDataStore[Tile], terrainStore : TEntityDataStore[Terrain], vegetationStore : TEntityDataStore[Vegetation], physicalStore : TEntityDataStore[Physical]) : Option[Sext] = {
		val tileEnt = Tiles.tileAt(position)
		tileStore.getOpt(tileEnt) match {
			case Some(tileData) =>
				if (!tileData.entities.exists(e => physicalStore.getOpt(e).exists(p => p.occupiesHex))) {
					val terrain = terrainStore.get(tileEnt)
					val vegetation = vegetationStore.get(tileEnt)

					Some(terrain.moveCost + vegetation.moveCost)
				} else {
					None
				}
			case None => None
		}
	}

	def moveCostTo(character : Entity, position : AxialVec3)(implicit view : WorldView) : Option[Sext] = {
		moveCostTo(character,position, view.dataStore[Tile], view.dataStore[Terrain], view.dataStore[Vegetation], view.dataStore[Physical])
	}


	private def setCharacterPosition(character : Entity, position : AxialVec3)(implicit world : World) : Boolean = {
		implicit val view = world.view
		val tile = Tiles.tileAt(position)
		if (tile[Tile].entities.isEmpty) {
			val prevTile = Tiles.tileAt(character[Physical].position)
			if (prevTile[Tile].entities.contains(character)) {
				world.modify(prevTile, Tile.entities - character, None)
			}

			world.modify(character, Physical.position -> position, None)
			world.modify(tile, Tile.entities -> Set(character), None)
			true
		} else {
			false
		}
	}

	def placeCharacterAt(character : Entity, position : AxialVec3)(implicit world : World): Boolean = {
		implicit val view = world.view

		if (setCharacterPosition(character, position)) {
			world.addEvent(EntityPlaced(character, position))
			true
		} else {
			false
		}
	}

	def removeCharacterFromTile(character : Entity)(implicit world : World) : Boolean = {
		implicit val view = world.view

		val tile = Tiles.tileAt(character[Physical].position)
		if (tile[Tile].entities.contains(character)) {
			world.modify(tile, Tile.entities - character, None)
			true
		} else {
			Noto.warn("Tried to remove character from tile, but it was not present")
			false
		}
	}

	def moveCharacterOnPath(character : Entity, path : Path[AxialVec3])(implicit world : World) : Boolean = {
		implicit val view = world.view

		val physical = character[Physical]

		if (path.steps.headOption.exists(h => h.node == physical.position)) {
			for ((from,to) <- path.steps.map(p => p.node).sliding2) {
				moveCostTo(character, to) match {
					case Some(moveCost) if CharacterLogic.curMovePoints(character) >= moveCost =>
						world.eventStmt(EntityMoved(character, from, to))
						val positionChangedSuccessfully = setCharacterPosition(character, to)
						if (!positionChangedSuccessfully) {
							return false
						}

						world.modify(character, CharacterInfo.movePoints - moveCost, "movement")
						world.endEvent(EntityMoved(character, from, to))
					case _ => return false
				}
			}
			true
		} else {
			Noto.error("Path started at hex other than the character's current hex")
			false
		}
	}

	def subPath(character : Entity, path : Path[AxialVec3], costLimit : Sext)(implicit view : WorldView) : Path[AxialVec3] = {
		path.steps match {
			case head :: _ =>
				var cost = Sext(0)

				var resultPath = Vector[PathStep[AxialVec3]](head)
				path.steps.sliding2.foreach{ case (from, to) => {
					moveCostTo(character, to.node) match {
						case Some(singleStepCost) =>
							if (cost + singleStepCost > costLimit) {
								return Path(resultPath.toList)
							} else {
								resultPath :+= to
								cost += singleStepCost
							}
						case None =>
							Noto.error("Path encountered with non-moveable path step")
							return Path(resultPath.toList)
					}
				}}
				Path(resultPath.toList)
			case _ =>
				path
		}
	}

	def movePointsRequiredForPath(character : Entity, path : Path[AxialVec3])(implicit view : WorldView) : Sext = {
		val charInf = character(CharacterInfo)
		var mp = Sext(0)
		for ((from,to) <- path.steps.sliding2) {
			moveCostTo(character, to.node) match {
				case Some(stepCost) => mp += stepCost
				case None => Noto.warn("Checking action cost of untravellable path")
			}
		}
		mp
	}
}
