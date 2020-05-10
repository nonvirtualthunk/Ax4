package arx.ax4.graphics.data

import arx.application.Noto
import arx.ax4.control.components.widgets.InventoryWidget
import arx.core.vec.coordinates.{AxialVec3, BiasedAxialVec3, HexDirection}
import arx.engine.control.components.windowing.Widget
import arx.engine.control.data.TControlData
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

class TacticalUIData extends TControlData with TMutableAuxData with TWorldAuxData {
	var selectedCharacterInfoWidget : Widget = _
	var inventoryWidget : InventoryWidget = _
	var mainSectionWidget : Widget = _
	var fullGameAreaWidget : Widget = _

	var perkSelectionWidget : Widget = _

	var selectedCharacter : Option[Entity] = None
	var consideringCharacterSwitch : Option[Entity] = None

	var mousedOverBiasedHex = BiasedAxialVec3(AxialVec3.Zero, HexDirection.Top)
	def mousedOverHex = mousedOverBiasedHex.vec
	def mousedOverHexBiasDir = mousedOverBiasedHex.biasDirection


	var activeUIMode : TacticalUIMode = TacticalUIMode.ChooseSpecialAttackMode

	def toggleUIMode(target : TacticalUIMode): Unit = {
		if (activeUIMode == target) {
			activeUIMode = TacticalUIMode.Neutral
		} else {
			activeUIMode = target
		}
	}
}

//case class ConsideredSelection(confirmation : SelectionConfirmationMethod, selector : Selector[_], value : Any, amount : Int)

trait SelectionConfirmationMethod
object SelectionConfirmationMethod {
	case object MousePress extends SelectionConfirmationMethod
}


trait TacticalUIMode
object TacticalUIMode {
	case object ChooseSpecialAttackMode extends TacticalUIMode
	case object InventoryMode extends TacticalUIMode
	case object Neutral extends TacticalUIMode
}