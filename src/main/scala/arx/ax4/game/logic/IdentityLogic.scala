package arx.ax4.game.logic

import arx.application.Noto
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
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

	def isA(entity : Entity, a : Taxon)(implicit view : WorldView) : Boolean = {
		entity.dataOpt[IdentityData] match {
			case Some(ident) => ident.kind.isA(a)
			case None =>
				Noto.info(s"asking for isA relationship with entity with no identity data ${entity.id}")
				false
		}
	}
}
