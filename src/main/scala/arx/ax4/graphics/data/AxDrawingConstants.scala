package arx.ax4.graphics.data

import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.graphics.data.TGraphicsData

class AxDrawingConstants extends AxGraphicsData with TWorldAuxData with TMutableAuxData {
	var HexSize = 160
	var HexHeight = (HexSize  / 1.1547005).toInt


	def HexWidth = HexSize


}
