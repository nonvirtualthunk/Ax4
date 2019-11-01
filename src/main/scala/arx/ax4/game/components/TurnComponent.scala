package arx.ax4.game.components

import arx.ax4.game.entities.AllegianceData
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.AttackEvent
import arx.ax4.game.event.TurnEvents.{TurnEndedEvent, TurnStartedEvent}
import arx.core.units.UnitOfTime
import arx.engine.game.components.GameComponent
import arx.engine.world.World

class TurnComponent extends GameComponent {
	import arx.core.introspection.FieldOperations._

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		onGameEvent {
			case TurnStartedEvent(faction, turnNumber) =>
				for (allegiant <- world.view.entitiesMatching[AllegianceData](ad => ad.faction == faction)) {
					world.modify(allegiant, CharacterInfo.actionPoints.recoverToFull())
				}
			case TurnEndedEvent(faction, turnNumber) =>
				for (allegiant <- world.view.entitiesMatching[AllegianceData](ad => ad.faction == faction)) {
					world.modify(allegiant, CharacterInfo.movePoints -> 0)
					world.modify(allegiant, CharacterInfo.actionPoints reduceTo 0)
				}
		}
	}

	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}
}
