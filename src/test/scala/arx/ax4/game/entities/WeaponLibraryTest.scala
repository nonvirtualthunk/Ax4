package arx.ax4.game.entities

import arx.core.math.Sext
import arx.engine.entity.{Taxon, Taxonomy}
import arx.engine.world.World
import arx.resource.ResourceManager
import org.scalatest.FunSuite

class WeaponLibraryTest extends FunSuite {

	test("weapon library loading TestWeapons") {
		Taxonomy.load(ResourceManager.sml("TestTaxonomy.sml"))

		WeaponLibrary.load("TestWeapons.sml")

		val testSwordArch = WeaponLibrary.withKind(Taxonomy("TestSword"))

		val world = new World
		world.register[Weapon]
		world.registerSubtypesOf[AxAuxData]()

		val ent = testSwordArch.createEntity(world)
		implicit val view = world.view

		val WD = ent[Weapon]
		val ID = ent[Item]
		assert(WD.attacks.keys.toSeq == Seq(AttackKey.Primary))
		assert(WD.attacks(AttackKey.Primary).accuracyBonus == 1)
		assert(ID.durability.maxValue == Sext(1))
	}
}
