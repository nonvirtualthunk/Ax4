package arx.ax4.game.logic

import arx.engine.entity.{Entity, IdentityData, Taxonomy}
import arx.engine.world.WorldView

object IdentityLogic {

	def kind(entity : Entity)(implicit view : WorldView) = {
		entity.dataOpt[IdentityData] match {
			case Some(ident) => ident.kind
			case None => Taxonomy.UnknownThing
		}
	}

	def name(entity : Entity)(implicit view : WorldView) : String = {
		entity.dataOpt[IdentityData] match {
			case Some(ident) => ident.name match {
				case Some(nomen) => nomen
				case None => ident.kind.toString
			}
			case None => "Nameless"
		}
	}
}
