package arx.ax4.game.entities

import arx.Prelude.none
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CardData, Consumable, Equipment, Inventory, Item, TagData, Weapon}
import arx.ax4.game.logic.{CardLogic, TagLogic}
import arx.core.NoAutoLoad
import arx.core.introspection.Clazz
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.engine.data.{Reduceable, TAuxData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.World
import arx.core.introspection.FieldOperations._

@GenerateCompanion
class Item extends AxAuxData {
	var durability: Reduceable[Sext] = Reduceable(Sext(25))

	var equipable: Boolean = false
	var equippedTo: Option[Entity] = None
	var heldIn: Option[Entity] = None
	var wornOn: Map[Taxon, Int] = Map()
	var usesBodyParts: Map[Taxon, Int] = Map()

	@NoAutoLoad var inventoryCardArchetypes: Vector[EntityArchetype] = Vector()
	@NoAutoLoad var inventoryCards: Vector[Entity] = Vector()
	@NoAutoLoad var equippedCardArchetypes: Vector[EntityArchetype] = Vector()
	@NoAutoLoad var equippedCards: Vector[Entity] = Vector()

	var equippedFlags: Map[Taxon, Int] = Map()

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (invCards <- config.fieldOpt("inventoryCards"); (cardName, cardCount) <- invCards.fields; _ <- 0 until cardCount.int) {
			inventoryCardArchetypes :+= CardLibrary.withKind(Taxonomy(cardName))
		}
		for (invCards <- config.fieldOpt("equippedCards"); (cardName, cardCount) <- invCards.fields; _ <- 0 until cardCount.int) {
			equippedCardArchetypes :+= CardLibrary.withKind(Taxonomy(cardName))
		}
	}

	override def onCopy(world: World): Unit = {
		inventoryCards = inventoryCards.map(_.copy(world))
		equippedCards = equippedCards.map(_.copy(world))
	}
}

@GenerateCompanion
class Consumable extends AxAuxData {
	var uses: Reduceable[Int] = Reduceable(1)
}

@GenerateCompanion
class Inventory extends AxAuxData {
	var heldItems = Set[Entity]()
	var heldItemCountLimit = none[Int]
}

@GenerateCompanion
class Equipment extends AxAuxData {
	@NoAutoLoad
	var equipped: Set[Entity] = Set()
}

@GenerateCompanion
class TagData extends AxAuxData {
	var tags: Set[Taxon] = Set()
	var flags: Map[Taxon, Int] = Map()
}

@GenerateCompanion
class Weapon extends AxAuxData {
	@NoAutoLoad var attacks: Map[AttackKey, AttackData] = Map()
	var primaryAttack: AttackKey = AttackKey.Primary
	var weaponSkills: List[Taxon] = Nil
var naturalWeapon: Boolean = false

	@NoAutoLoad var attackCards: Vector[Entity] = Vector()

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (attField <- config.fieldOpt("attacks")) {
			for ((attName, attDataConfig) <- attField.fields) {
				val attData = AttackData(Entity.Sentinel).loadFromConfig(attDataConfig)
				attacks += AttackKey.parse(attName) -> attData
			}
		}
		for (weaponSkillsConf <- config.fieldOpt("weaponSkills")) {
			weaponSkills = weaponSkillsConf.arr.map(cv => Taxonomy(cv.str)).toList
		}
	}
}


trait AttackKey

object AttackKey {

	case object Primary extends AttackKey

	case object Secondary extends AttackKey

	case object Tertiary extends AttackKey

	case object Technique extends AttackKey

	case object Unknown extends AttackKey

	def parse(str: String) = str.toLowerCase() match {
		case "primary" => Primary
		case "secondary" => Secondary
		case "tertiary" => Tertiary
		case other =>
			Noto.error(s"Invalid attack key $other")
			Unknown
	}
}

//class WeaponArchetype extends EntityArchetype {
//	override def createEntity(world: World): Entity = {
//		implicit val w = world
//
//		val ent = super.createEntity(world)
//
//
//
//		ent
//	}
//}

object WeaponLibrary extends EntityArchetypeLibrary {
	override protected def topLevelField: String = "Weapons"

	override def defaultNamespace: String = "Items.Weapons"

	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(Item, Equipment, Weapon, TagData)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map("heldItemCountLimit" -> Inventory)


	//	override protected def createBlank(): EntityArchetype = new WeaponArchetype

	override def initialLoad(): Unit = {
		load("game/data/items/Weapons.sml")
	}
}

object ItemLibrary extends EntityArchetypeLibrary {
	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(Item, TagData)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map("uses" -> Consumable)

	override protected def topLevelField: String = "Items"

	override def defaultNamespace: String = "Items"

	override def initialLoad(): Unit = {
		load("game/data/items/Items.sml")
	}
}

object CardLibrary extends EntityArchetypeLibrary {
	override protected def fixedDataTypes: Seq[_ <: Clazz[_ <: TAuxData]] = List(CardData, TagData)

	override protected def conditionalDataTypes: Map[String, _ <: Clazz[_ <: TAuxData]] = Map()

	override protected def topLevelField: String = "Cards"

	override def defaultNamespace: String = "CardTypes"

	override def initialLoad(): Unit = {
		load("game/data/cards/Cards.sml")
		load("game/data/cards/MonsterCards.sml")
		load("game/data/cards/StatusCards.sml")
	}
}