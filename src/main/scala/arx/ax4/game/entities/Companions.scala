package arx.ax4.game.entities
import arx.core.introspection.Field
import arx.core.introspection.Clazz
object Companions {
import arx.ax4.game.entities.Physical
object Physical extends Clazz[Physical]("Physical", classOf[Physical]){
	val Sentinel = new Physical
	override def instantiate = new Physical
	val position = Field.fromValue(Sentinel.position).createField[Physical]("position",f => f.position, (f,position) => f.position = position, Physical) 
	fields += "position" -> position
	val offset = Field.fromValue(Sentinel.offset).createField[Physical]("offset",f => f.offset, (f,offset) => f.offset = offset, Physical) 
	fields += "offset" -> offset
	val facing = Field.fromValue(Sentinel.facing).createField[Physical]("facing",f => f.facing, (f,facing) => f.facing = facing, Physical) 
	fields += "facing" -> facing
	val occupiesHex = Field.fromValue(Sentinel.occupiesHex).createField[Physical]("occupiesHex",f => f.occupiesHex, (f,occupiesHex) => f.occupiesHex = occupiesHex, Physical) 
	fields += "occupiesHex" -> occupiesHex

	def apply(f : Physical => Unit) : Physical = { val v = new Physical; f(v); v }
					 
}
import arx.ax4.game.entities.Equipment
object Equipment extends Clazz[Equipment]("Equipment", classOf[Equipment]){
	val Sentinel = new Equipment
	override def instantiate = new Equipment
	val equipped = Field.fromValue(Sentinel.equipped).createField[Equipment]("equipped",f => f.equipped, (f,equipped) => f.equipped = equipped, Equipment) 
	fields += "equipped" -> equipped
	val wornOn = Field.fromValue(Sentinel.wornOn).createField[Equipment]("wornOn",f => f.wornOn, (f,wornOn) => f.wornOn = wornOn, Equipment) 
	fields += "wornOn" -> wornOn
	val usesBodyParts = Field.fromValue(Sentinel.usesBodyParts).createField[Equipment]("usesBodyParts",f => f.usesBodyParts, (f,usesBodyParts) => f.usesBodyParts = usesBodyParts, Equipment) 
	fields += "usesBodyParts" -> usesBodyParts

	def apply(f : Equipment => Unit) : Equipment = { val v = new Equipment; f(v); v }
					 
}
import arx.ax4.game.entities.Terrain
object Terrain extends Clazz[Terrain]("Terrain", classOf[Terrain]){
	val Sentinel = new Terrain
	override def instantiate = new Terrain
	val descriptors = Field.fromValue(Sentinel.descriptors).createField[Terrain]("descriptors",f => f.descriptors, (f,descriptors) => f.descriptors = descriptors, Terrain) 
	fields += "descriptors" -> descriptors
	val fertility = Field.fromValue(Sentinel.fertility).createField[Terrain]("fertility",f => f.fertility, (f,fertility) => f.fertility = fertility, Terrain) 
	fields += "fertility" -> fertility
	val cover = Field.fromValue(Sentinel.cover).createField[Terrain]("cover",f => f.cover, (f,cover) => f.cover = cover, Terrain) 
	fields += "cover" -> cover
	val elevation = Field.fromValue(Sentinel.elevation).createField[Terrain]("elevation",f => f.elevation, (f,elevation) => f.elevation = elevation, Terrain) 
	fields += "elevation" -> elevation
	val moveCost = Field.fromValue(Sentinel.moveCost).createField[Terrain]("moveCost",f => f.moveCost, (f,moveCost) => f.moveCost = moveCost, Terrain) 
	fields += "moveCost" -> moveCost
	val kind = Field.fromValue(Sentinel.kind).createField[Terrain]("kind",f => f.kind, (f,kind) => f.kind = kind, Terrain) 
	fields += "kind" -> kind

	def apply(f : Terrain => Unit) : Terrain = { val v = new Terrain; f(v); v }
					 
}
import arx.ax4.game.entities.Item
object Item extends Clazz[Item]("Item", classOf[Item]){
	val Sentinel = new Item
	override def instantiate = new Item
	val durability = Field.fromValue(Sentinel.durability).createField[Item]("durability",f => f.durability, (f,durability) => f.durability = durability, Item) 
	fields += "durability" -> durability

	def apply(f : Item => Unit) : Item = { val v = new Item; f(v); v }
					 
}
import arx.ax4.game.entities.Inventory
object Inventory extends Clazz[Inventory]("Inventory", classOf[Inventory]){
	val Sentinel = new Inventory
	override def instantiate = new Inventory
	val heldItems = Field.fromValue(Sentinel.heldItems).createField[Inventory]("heldItems",f => f.heldItems, (f,heldItems) => f.heldItems = heldItems, Inventory) 
	fields += "heldItems" -> heldItems
	val heldItemCountLimit = Field.fromValue(Sentinel.heldItemCountLimit).createField[Inventory]("heldItemCountLimit",f => f.heldItemCountLimit, (f,heldItemCountLimit) => f.heldItemCountLimit = heldItemCountLimit, Inventory) 
	fields += "heldItemCountLimit" -> heldItemCountLimit

	def apply(f : Inventory => Unit) : Inventory = { val v = new Inventory; f(v); v }
					 
}
import arx.ax4.game.entities.Vegetation
object Vegetation extends Clazz[Vegetation]("Vegetation", classOf[Vegetation]){
	val Sentinel = new Vegetation
	override def instantiate = new Vegetation
	val layers = Field.fromValue(Sentinel.layers).createField[Vegetation]("layers",f => f.layers, (f,layers) => f.layers = layers, Vegetation) 
	fields += "layers" -> layers

	def apply(f : Vegetation => Unit) : Vegetation = { val v = new Vegetation; f(v); v }
					 
}
import arx.ax4.game.entities.AllegianceData
object AllegianceData extends Clazz[AllegianceData]("AllegianceData", classOf[AllegianceData]){
	val Sentinel = new AllegianceData
	override def instantiate = new AllegianceData
	val faction = Field.fromValue(Sentinel.faction).createField[AllegianceData]("faction",f => f.faction, (f,faction) => f.faction = faction, AllegianceData) 
	fields += "faction" -> faction

	def apply(f : AllegianceData => Unit) : AllegianceData = { val v = new AllegianceData; f(v); v }
					 
}
import arx.ax4.game.entities.CharacterInfo
object CharacterInfo extends Clazz[CharacterInfo]("CharacterInfo", classOf[CharacterInfo]){
	val Sentinel = new CharacterInfo
	override def instantiate = new CharacterInfo
	val species = Field.fromValue(Sentinel.species).createField[CharacterInfo]("species",f => f.species, (f,species) => f.species = species, CharacterInfo) 
	fields += "species" -> species
	val faction = Field.fromValue(Sentinel.faction).createField[CharacterInfo]("faction",f => f.faction, (f,faction) => f.faction = faction, CharacterInfo) 
	fields += "faction" -> faction
	val sex = Field.fromValue(Sentinel.sex).createField[CharacterInfo]("sex",f => f.sex, (f,sex) => f.sex = sex, CharacterInfo) 
	fields += "sex" -> sex
	val health = Field.fromValue(Sentinel.health).createField[CharacterInfo]("health",f => f.health, (f,health) => f.health = health, CharacterInfo) 
	fields += "health" -> health
	val healthRecoveryRate = Field.fromValue(Sentinel.healthRecoveryRate).createField[CharacterInfo]("healthRecoveryRate",f => f.healthRecoveryRate, (f,healthRecoveryRate) => f.healthRecoveryRate = healthRecoveryRate, CharacterInfo) 
	fields += "healthRecoveryRate" -> healthRecoveryRate
	val alive = Field.fromValue(Sentinel.alive).createField[CharacterInfo]("alive",f => f.alive, (f,alive) => f.alive = alive, CharacterInfo) 
	fields += "alive" -> alive
	val actionPoints = Field.fromValue(Sentinel.actionPoints).createField[CharacterInfo]("actionPoints",f => f.actionPoints, (f,actionPoints) => f.actionPoints = actionPoints, CharacterInfo) 
	fields += "actionPoints" -> actionPoints
	val moveSpeed = Field.fromValue(Sentinel.moveSpeed).createField[CharacterInfo]("moveSpeed",f => f.moveSpeed, (f,moveSpeed) => f.moveSpeed = moveSpeed, CharacterInfo) 
	fields += "moveSpeed" -> moveSpeed
	val movePoints = Field.fromValue(Sentinel.movePoints).createField[CharacterInfo]("movePoints",f => f.movePoints, (f,movePoints) => f.movePoints = movePoints, CharacterInfo) 
	fields += "movePoints" -> movePoints
	val bodyParts = Field.fromValue(Sentinel.bodyParts).createField[CharacterInfo]("bodyParts",f => f.bodyParts, (f,bodyParts) => f.bodyParts = bodyParts, CharacterInfo) 
	fields += "bodyParts" -> bodyParts
	val strength = Field.fromValue(Sentinel.strength).createField[CharacterInfo]("strength",f => f.strength, (f,strength) => f.strength = strength, CharacterInfo) 
	fields += "strength" -> strength
	val dexterity = Field.fromValue(Sentinel.dexterity).createField[CharacterInfo]("dexterity",f => f.dexterity, (f,dexterity) => f.dexterity = dexterity, CharacterInfo) 
	fields += "dexterity" -> dexterity
	val intellect = Field.fromValue(Sentinel.intellect).createField[CharacterInfo]("intellect",f => f.intellect, (f,intellect) => f.intellect = intellect, CharacterInfo) 
	fields += "intellect" -> intellect
	val cunning = Field.fromValue(Sentinel.cunning).createField[CharacterInfo]("cunning",f => f.cunning, (f,cunning) => f.cunning = cunning, CharacterInfo) 
	fields += "cunning" -> cunning
	val activeAttack = Field.fromValue(Sentinel.activeAttack).createField[CharacterInfo]("activeAttack",f => f.activeAttack, (f,activeAttack) => f.activeAttack = activeAttack, CharacterInfo) 
	fields += "activeAttack" -> activeAttack

	def apply(f : CharacterInfo => Unit) : CharacterInfo = { val v = new CharacterInfo; f(v); v }
					 
}
import arx.ax4.game.entities.CombatData
object CombatData extends Clazz[CombatData]("CombatData", classOf[CombatData]){
	val Sentinel = new CombatData
	override def instantiate = new CombatData
	val attackModifier = Field.fromValue(Sentinel.attackModifier).createField[CombatData]("attackModifier",f => f.attackModifier, (f,attackModifier) => f.attackModifier = attackModifier, CombatData) 
	fields += "attackModifier" -> attackModifier
	val conditionalAttackModifiers = Field.fromValue(Sentinel.conditionalAttackModifiers).createField[CombatData]("conditionalAttackModifiers",f => f.conditionalAttackModifiers, (f,conditionalAttackModifiers) => f.conditionalAttackModifiers = conditionalAttackModifiers, CombatData) 
	fields += "conditionalAttackModifiers" -> conditionalAttackModifiers
	val defenseModifier = Field.fromValue(Sentinel.defenseModifier).createField[CombatData]("defenseModifier",f => f.defenseModifier, (f,defenseModifier) => f.defenseModifier = defenseModifier, CombatData) 
	fields += "defenseModifier" -> defenseModifier
	val conditionalDefenseModifiers = Field.fromValue(Sentinel.conditionalDefenseModifiers).createField[CombatData]("conditionalDefenseModifiers",f => f.conditionalDefenseModifiers, (f,conditionalDefenseModifiers) => f.conditionalDefenseModifiers = conditionalDefenseModifiers, CombatData) 
	fields += "conditionalDefenseModifiers" -> conditionalDefenseModifiers
	val specialAttacks = Field.fromValue(Sentinel.specialAttacks).createField[CombatData]("specialAttacks",f => f.specialAttacks, (f,specialAttacks) => f.specialAttacks = specialAttacks, CombatData) 
	fields += "specialAttacks" -> specialAttacks

	def apply(f : CombatData => Unit) : CombatData = { val v = new CombatData; f(v); v }
					 
}
import arx.ax4.game.entities.FactionData
object FactionData extends Clazz[FactionData]("FactionData", classOf[FactionData]){
	val Sentinel = new FactionData
	override def instantiate = new FactionData
	val color = Field.fromValue(Sentinel.color).createField[FactionData]("color",f => f.color, (f,color) => f.color = color, FactionData) 
	fields += "color" -> color
	val playerControlled = Field.fromValue(Sentinel.playerControlled).createField[FactionData]("playerControlled",f => f.playerControlled, (f,playerControlled) => f.playerControlled = playerControlled, FactionData) 
	fields += "playerControlled" -> playerControlled

	def apply(f : FactionData => Unit) : FactionData = { val v = new FactionData; f(v); v }
					 
}
import arx.ax4.game.entities.Weapon
object Weapon extends Clazz[Weapon]("Weapon", classOf[Weapon]){
	val Sentinel = new Weapon
	override def instantiate = new Weapon
	val attacks = Field.fromValue(Sentinel.attacks).createField[Weapon]("attacks",f => f.attacks, (f,attacks) => f.attacks = attacks, Weapon) 
	fields += "attacks" -> attacks
	val primaryAttack = Field.fromValue(Sentinel.primaryAttack).createField[Weapon]("primaryAttack",f => f.primaryAttack, (f,primaryAttack) => f.primaryAttack = primaryAttack, Weapon) 
	fields += "primaryAttack" -> primaryAttack

	def apply(f : Weapon => Unit) : Weapon = { val v = new Weapon; f(v); v }
					 
}
import arx.ax4.game.entities.Tile
object Tile extends Clazz[Tile]("Tile", classOf[Tile]){
	val Sentinel = new Tile
	override def instantiate = new Tile
	val entities = Field.fromValue(Sentinel.entities).createField[Tile]("entities",f => f.entities, (f,entities) => f.entities = entities, Tile) 
	fields += "entities" -> entities
	val position = Field.fromValue(Sentinel.position).createField[Tile]("position",f => f.position, (f,position) => f.position = position, Tile) 
	fields += "position" -> position

	def apply(f : Tile => Unit) : Tile = { val v = new Tile; f(v); v }
					 
}
import arx.ax4.game.entities.VegetationLayer
object VegetationLayer extends Clazz[VegetationLayer]("VegetationLayer", classOf[VegetationLayer]){
	val Sentinel = new VegetationLayer
	override def instantiate = new VegetationLayer
	val layer = Field.fromValue(Sentinel.layer).createField[VegetationLayer]("layer",f => f.layer, (f,layer) => f.layer = layer, VegetationLayer) 
	fields += "layer" -> layer
	val cover = Field.fromValue(Sentinel.cover).createField[VegetationLayer]("cover",f => f.cover, (f,cover) => f.cover = cover, VegetationLayer) 
	fields += "cover" -> cover
	val moveCost = Field.fromValue(Sentinel.moveCost).createField[VegetationLayer]("moveCost",f => f.moveCost, (f,moveCost) => f.moveCost = moveCost, VegetationLayer) 
	fields += "moveCost" -> moveCost
	val kind = Field.fromValue(Sentinel.kind).createField[VegetationLayer]("kind",f => f.kind, (f,kind) => f.kind = kind, VegetationLayer) 
	fields += "kind" -> kind

	def apply(f : VegetationLayer => Unit) : VegetationLayer = { val v = new VegetationLayer; f(v); v }
					 
}
}
