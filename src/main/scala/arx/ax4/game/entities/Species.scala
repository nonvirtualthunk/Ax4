package arx.ax4.game.entities

import arx.engine.entity.Taxon


class Species(name_ : String, parentTaxons : List[Taxon]) extends Taxon(name_, parentTaxons) {
	def this(name_ : String, parentTaxon : Taxon) { this(name_, parentTaxon :: Nil) }
}

object Species {
	import arx.engine.entity.Taxonomy.taxon

	case object Human extends Species("human", taxon("Humanoid"))
	case object Elf extends Species("elf", taxon("Elf"))
	case object MudMonster extends Species("mud monster", taxon("Monstrous"))
}