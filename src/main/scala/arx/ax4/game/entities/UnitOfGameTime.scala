package arx.ax4.game.entities

import arx.core.units.{MeasurementUnit, UnitOfMeasure}

import scala.language.implicitConversions

class UnitOfGameTime(unit : MeasurementUnit,value: Float) extends UnitOfMeasure[UnitOfGameTime](unit,value) {
	def this () { this(Turn,0.0f) }
	def inTurns : Float = value * unit.conversion


	def baseUnitOfMeasure = Turn
	def toBaseUnitOfMeasure = inTurns
	def in(newUnit:MeasurementUnit) = this.inTurns / newUnit.conversion
	def create(u: MeasurementUnit, v: Float) = new UnitOfGameTime(u,v)
	def order : Int = 1

	def resolve() = this
	def baseValue() = this
}


object Turn extends MeasurementUnit {
	override val name: String = "turn"
	override val suffix: String = "turn"
	override var conversion: Float = 1.0f
}

object GameDay extends MeasurementUnit {
	override val name: String = "day"
	override val suffix: String = "d"
	override var conversion: Float = 12.0f
}

object GameSeason extends MeasurementUnit {
	override val name: String = "season"
	override val suffix: String = "seas"
	override var conversion: Float = 4.0f * GameDay.conversion
}

object GameYear extends MeasurementUnit {
	override val name: String = "year"
	override val suffix: String = "y"
	override var conversion: Float = 4.0f * GameSeason.conversion
}



class UnitOfGameTimeFloat(val f : Float) extends AnyVal {
	def turns = new UnitOfGameTime(Turn, f)
	def turn = new UnitOfGameTime(Turn, f)

	def gameDay = new UnitOfGameTime(GameDay, f)
	def gameDays = new UnitOfGameTime(GameDay, f)

	def gameYear = new UnitOfGameTime(GameYear, f)
	def gameYears = new UnitOfGameTime(GameYear, f)

	def gameSeason = new UnitOfGameTime(GameSeason, f)
	def gameSeasons = new UnitOfGameTime(GameSeason, f)
}

object UnitOfGameTimeFloat {
	implicit def fromFloat(f : Float) : UnitOfGameTimeFloat = new UnitOfGameTimeFloat(f)
}