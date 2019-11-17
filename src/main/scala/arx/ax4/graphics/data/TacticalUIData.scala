package arx.ax4.graphics.data

import arx.ax4.control.components.ActionSelectionContext
import arx.ax4.control.components.widgets.InventoryWidget
import arx.ax4.game.action.BiasedAxialVec3
import arx.ax4.graphics.data.TacticalUIMode.Neutral
import arx.core.vec.coordinates.{AxialVec3, HexDirection}
import arx.engine.control.components.windowing.Widget
import arx.engine.control.data.TControlData
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.entity.Entity

class TacticalUIData extends TControlData with TMutableAuxData with TWorldAuxData {
	var selectedCharacterInfoWidget : Widget = _
	var specialAttacksList : Widget = _
	var inventoryWidget : InventoryWidget = _
	var mainSectionWidget : Widget = _

	var selectedCharacter : Option[Entity] = None

	var actionSelectionContext : Option[ActionSelectionContext] = None
	var consideringActionSelectionContext : Option[ActionSelectionContext] = None
	var consideringSelection : Option[Entity] = None

	var mousedOverBiasedHex = BiasedAxialVec3(AxialVec3.Zero, HexDirection.Top)
	def mousedOverHex = mousedOverBiasedHex.vec
	def mousedOverHexBiasDir = mousedOverBiasedHex.biasDirection


	var activeUIMode : TacticalUIMode = TacticalUIMode.ChooseSpecialAttackMode

	def toggleUIMode(target : TacticalUIMode): Unit = {
		if (activeUIMode == target) {
			activeUIMode = Neutral
		} else {
			activeUIMode = target
		}
	}
}


trait TacticalUIMode
object TacticalUIMode {
	case object ChooseSpecialAttackMode extends TacticalUIMode
	case object InventoryMode extends TacticalUIMode
	case object Neutral extends TacticalUIMode
}