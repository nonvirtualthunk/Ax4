package arx.ax4.game.entities

import arx.Prelude.none
import arx.ax4.game.entities.Companions.{Equipment, Inventory, Item, Weapon}
import arx.core.introspection.Clazz
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.engine.data.{Reduceable, TAuxData}
import arx.engine.entity.{Entity, Taxon}

@GenerateCompanion
class Item extends AxAuxData {
	var durability : Reduceable[Sext] = Reduceable(Sext(25))
}

@GenerateCompanion
class Inventory extends AxAuxData {
	var heldItems = List[Entity]()
	var heldItemCountLimit = none[Int]
}

@GenerateCompanion
class Equipment extends AxAuxData {
	var equipped : Set[Entity] = Set()

	var wornOn : Map[Taxon, Int] = Map()
	var usesBodyParts : Map[Taxon, Int] = Map()
}


@GenerateCompanion
class Weapon extends AxAuxData {
	var attacks : Map[AnyRef, AttackData] = Map()
	var primaryAttack : AnyRef = "primary"

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (attField <- config.fieldOpt("attacks")) {
			for ((attName, attDataConfig) <- attField.fields) {
				val attData = new AttackData
				attData.loadFromConfig(attDataConfig)
				attacks += attName -> attData
			}
		}
	}
}


object WeaponLibrary extends EntityArchetypeLibrary {
	override protected def topLevelField: String = "Weapons"

	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(Item, Equipment, Weapon)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map("heldItemCountLimit" -> Inventory)

	load("game/data/items/Weapons.sml")
}