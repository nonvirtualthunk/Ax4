package arx.ax4.game.action

import arx.engine.entity.Entity
import arx.engine.world.WorldView

trait Selectable {
	def instantiate(world : WorldView, entity : Entity) : Either[SelectableInstance, String]
}

trait SelectableInstance {
	def nextSelector(results : SelectionResult) : Option[Selector[_]]
}



trait CompoundSelectable extends Selectable {
	def subSelectables(world : WorldView) : Traversable[Selectable]

	override def instantiate(world: WorldView, entity: Entity): Either[SelectableInstance, String] = {
		val subSel = subSelectables(world)
		val subSelectableInstRaw = subSel.map(s => s -> s.instantiate(world, entity))

		var subSelectableInst = Vector[(Selectable, SelectableInstance)]()
		subSelectableInstRaw.foreach {
			case (k, Left(s)) => subSelectableInst :+= (k -> s)
			case (_, Right(reason)) => return Right(reason)
		}

		Left(new CompoundSelectableInstance(subSelectableInst))
	}
}

class CompoundSelectableInstance(val subSelectableInstances : Vector[(Selectable, SelectableInstance)]) extends SelectableInstance {
	override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
		for ((_, selInst) <- subSelectableInstances) {
			selInst.nextSelector(results) match {
				case s @ Some(_) => return s
				case None => // continue
			}
		}
		None
	}
}

abstract class Selector[MatchedType](val origin : Selectable) {
	var amount = 1

	def satisfiedBy(view: WorldView, a: Any): Option[(MatchedType, Int)]

	def description: String

	def withAmount(n: Int) = {
		amount = n
		this
	}
}