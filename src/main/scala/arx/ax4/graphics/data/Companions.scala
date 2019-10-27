package arx.ax4.graphics.data
import arx.core.introspection.Field
import arx.core.introspection.Clazz
object Companions {
import arx.ax4.graphics.data.CharacterDrawingData
object CharacterDrawingData extends Clazz[CharacterDrawingData]("CharacterDrawingData", classOf[CharacterDrawingData]){
	val Sentinel = new CharacterDrawingData
	override def instantiate = new CharacterDrawingData
	val exactPositionOverride = Field.fromValue(Sentinel.exactPositionOverride).createField[CharacterDrawingData]("exactPositionOverride",f => f.exactPositionOverride, (f,exactPositionOverride) => f.exactPositionOverride = exactPositionOverride, CharacterDrawingData) 
	fields += "exactPositionOverride" -> exactPositionOverride
	val colorMultiplier = Field.fromValue(Sentinel.colorMultiplier).createField[CharacterDrawingData]("colorMultiplier",f => f.colorMultiplier, (f,colorMultiplier) => f.colorMultiplier = colorMultiplier, CharacterDrawingData) 
	fields += "colorMultiplier" -> colorMultiplier

	def apply(f : CharacterDrawingData => Unit) : CharacterDrawingData = { val v = new CharacterDrawingData; f(v); v }
					 
}
}
