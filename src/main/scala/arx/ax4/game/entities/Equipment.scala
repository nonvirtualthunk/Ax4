package arx.ax4.game.entities

import arx.Prelude.none
import arx.ax4.game.entities.Companions.{Consumable, Equipment, Inventory, Item, Weapon}
import arx.core.introspection.Clazz
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.engine.data.{Reduceable, TAuxData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}

@GenerateCompanion
class Item extends AxAuxData {
	var durability : Reduceable[Sext] = Reduceable(Sext(25))

	var equipable : Boolean = false
	var equippedTo : Option[Entity] = None
	var heldIn : Option[Entity] = None
	var wornOn : Map[Taxon, Int] = Map()
	var usesBodyParts : Map[Taxon, Int] = Map()

	var inventoryCards : Vector[Entity] = Vector()
	var equippedCards : Vector[Entity] = Vector()
}

@GenerateCompanion
class Consumable extends AxAuxData {
	var uses : Reduceable[Int] = Reduceable(1)
}

@GenerateCompanion
class Inventory extends AxAuxData {
	var heldItems = Set[Entity]()
	var heldItemCountLimit = none[Int]
}

@GenerateCompanion
class Equipment extends AxAuxData {
	var equipped : Set[Entity] = Set()
}


@GenerateCompanion
class Weapon extends AxAuxData {
	var attacks : Map[AnyRef, AttackData] = Map()
	var primaryAttack : AnyRef = "primary"
	var weaponSkills : List[Taxon] = Nil

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (attField <- config.fieldOpt("attacks")) {
			for ((attName, attDataConfig) <- attField.fields) {
				val attData = new AttackData
				attData.loadFromConfig(attDataConfig)
				attacks += attName -> attData
			}
		}
		for (weaponSkillsConf <- config.fieldOpt("weaponSkills")) {
			weaponSkills = weaponSkillsConf.arr.map(cv => Taxonomy(cv.str)).toList
		}
	}
}


object WeaponLibrary extends EntityArchetypeLibrary {
	override protected def topLevelField: String = "Weapons"

	override def defaultNamespace: String = "Items.Weapons"

	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(Item, Equipment, Weapon)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map("heldItemCountLimit" -> Inventory)

	override def initialLoad(): Unit = {
		load("game/data/items/Weapons.sml")
	}
}

object ItemLibrary extends EntityArchetypeLibrary {
	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(Item)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map("uses" -> Consumable)

	override protected def topLevelField: String = "Items"

	override def defaultNamespace: String = "Items"

	override def initialLoad(): Unit = {
		load("game/data/items/Items.sml")
	}
}