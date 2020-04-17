package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.TurnData
import arx.ax4.game.entities.{FactionData, TurnData}
import arx.ax4.game.event.TurnEvents.{TurnEndedEvent, TurnStartedEvent}
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._
import arx.engine.entity.Entity

object TurnLogic {
  def endTurn(implicit game : World): Unit = {
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
  }

  def activeFaction(implicit view : WorldView) : Entity = {
    view.worldData[TurnData].activeFaction
  }
}
