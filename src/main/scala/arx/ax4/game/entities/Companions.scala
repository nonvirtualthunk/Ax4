package arx.ax4.game.entities
import arx.core.introspection.Field
import arx.core.introspection.Clazz
object Companions {
import arx.ax4.game.entities.DeckData
object DeckData extends Clazz[DeckData]("DeckData", classOf[DeckData]){
	val Sentinel = new DeckData
	override def instantiate = new DeckData
	val drawPile = Field.fromValue(Sentinel.drawPile).createField[DeckData]("drawPile",f => f.drawPile, (f,drawPile) => f.drawPile = drawPile, DeckData) 
	fields += "drawPile" -> drawPile
	val discardPile = Field.fromValue(Sentinel.discardPile).createField[DeckData]("discardPile",f => f.discardPile, (f,discardPile) => f.discardPile = discardPile, DeckData) 
	fields += "discardPile" -> discardPile
	val hand = Field.fromValue(Sentinel.hand).createField[DeckData]("hand",f => f.hand, (f,hand) => f.hand = hand, DeckData) 
	fields += "hand" -> hand
	val exhaustPile = Field.fromValue(Sentinel.exhaustPile).createField[DeckData]("exhaustPile",f => f.exhaustPile, (f,exhaustPile) => f.exhaustPile = exhaustPile, DeckData) 
	fields += "exhaustPile" -> exhaustPile
	val attachedCards = Field.fromValue(Sentinel.attachedCards).createField[DeckData]("attachedCards",f => f.attachedCards, (f,attachedCards) => f.attachedCards = attachedCards, DeckData) 
	fields += "attachedCards" -> attachedCards
	val lockedCards = Field.fromValue(Sentinel.lockedCards).createField[DeckData]("lockedCards",f => f.lockedCards, (f,lockedCards) => f.lockedCards = lockedCards, DeckData) 
	fields += "lockedCards" -> lockedCards
	val lockedCardSlots = Field.fromValue(Sentinel.lockedCardSlots).createField[DeckData]("lockedCardSlots",f => f.lockedCardSlots, (f,lockedCardSlots) => f.lockedCardSlots = lockedCardSlots, DeckData) 
	fields += "lockedCardSlots" -> lockedCardSlots
	val drawCount = Field.fromValue(Sentinel.drawCount).createField[DeckData]("drawCount",f => f.drawCount, (f,drawCount) => f.drawCount = drawCount, DeckData) 
	fields += "drawCount" -> drawCount

	def apply(f : DeckData => Unit) : DeckData = { val v = new DeckData; f(v); v }
					 
	def copyInto(from : DeckData, to : DeckData) {
		to.drawPile = from.drawPile
		to.discardPile = from.discardPile
		to.hand = from.hand
		to.exhaustPile = from.exhaustPile
		to.attachedCards = from.attachedCards
		to.lockedCards = from.lockedCards
		to.lockedCardSlots = from.lockedCardSlots
		to.drawCount = from.drawCount
	}
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
					 
	def copyInto(from : Inventory, to : Inventory) {
		to.heldItems = from.heldItems
		to.heldItemCountLimit = from.heldItemCountLimit
	}
}
import arx.ax4.game.entities.MonsterData
object MonsterData extends Clazz[MonsterData]("MonsterData", classOf[MonsterData]){
	val Sentinel = new MonsterData
	override def instantiate = new MonsterData
	val monsterAttacks = Field.fromValue(Sentinel.monsterAttacks).createField[MonsterData]("monsterAttacks",f => f.monsterAttacks, (f,monsterAttacks) => f.monsterAttacks = monsterAttacks, MonsterData) 
	fields += "monsterAttacks" -> monsterAttacks

	def apply(f : MonsterData => Unit) : MonsterData = { val v = new MonsterData; f(v); v }
					 
	def copyInto(from : MonsterData, to : MonsterData) {
		to.monsterAttacks = from.monsterAttacks
	}
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
					 
	def copyInto(from : Terrain, to : Terrain) {
		to.descriptors = from.descriptors
		to.fertility = from.fertility
		to.cover = from.cover
		to.elevation = from.elevation
		to.moveCost = from.moveCost
		to.kind = from.kind
	}
}
import arx.ax4.game.entities.EnemyInfo
object EnemyInfo extends Clazz[EnemyInfo]("EnemyInfo", classOf[EnemyInfo]){
	val Sentinel = new EnemyInfo
	override def instantiate = new EnemyInfo
	val intent = Field.fromValue(Sentinel.intent).createField[EnemyInfo]("intent",f => f.intent, (f,intent) => f.intent = intent, EnemyInfo) 
	fields += "intent" -> intent
	val nextCardPlays = Field.fromValue(Sentinel.nextCardPlays).createField[EnemyInfo]("nextCardPlays",f => f.nextCardPlays, (f,nextCardPlays) => f.nextCardPlays = nextCardPlays, EnemyInfo) 
	fields += "nextCardPlays" -> nextCardPlays

	def apply(f : EnemyInfo => Unit) : EnemyInfo = { val v = new EnemyInfo; f(v); v }
					 
	def copyInto(from : EnemyInfo, to : EnemyInfo) {
		to.intent = from.intent
		to.nextCardPlays = from.nextCardPlays
	}
}
import arx.ax4.game.entities.Item
object Item extends Clazz[Item]("Item", classOf[Item]){
	val Sentinel = new Item
	override def instantiate = new Item
	val durability = Field.fromValue(Sentinel.durability).createField[Item]("durability",f => f.durability, (f,durability) => f.durability = durability, Item) 
	fields += "durability" -> durability
	val equipable = Field.fromValue(Sentinel.equipable).createField[Item]("equipable",f => f.equipable, (f,equipable) => f.equipable = equipable, Item) 
	fields += "equipable" -> equipable
	val equippedTo = Field.fromValue(Sentinel.equippedTo).createField[Item]("equippedTo",f => f.equippedTo, (f,equippedTo) => f.equippedTo = equippedTo, Item) 
	fields += "equippedTo" -> equippedTo
	val heldIn = Field.fromValue(Sentinel.heldIn).createField[Item]("heldIn",f => f.heldIn, (f,heldIn) => f.heldIn = heldIn, Item) 
	fields += "heldIn" -> heldIn
	val wornOn = Field.fromValue(Sentinel.wornOn).createField[Item]("wornOn",f => f.wornOn, (f,wornOn) => f.wornOn = wornOn, Item) 
	fields += "wornOn" -> wornOn
	val usesBodyParts = Field.fromValue(Sentinel.usesBodyParts).createField[Item]("usesBodyParts",f => f.usesBodyParts, (f,usesBodyParts) => f.usesBodyParts = usesBodyParts, Item) 
	fields += "usesBodyParts" -> usesBodyParts
	val equippedFlags = Field.fromValue(Sentinel.equippedFlags).createField[Item]("equippedFlags",f => f.equippedFlags, (f,equippedFlags) => f.equippedFlags = equippedFlags, Item) 
	fields += "equippedFlags" -> equippedFlags
	val inventoryCardArchetypes = Field.fromValue(Sentinel.inventoryCardArchetypes).createField[Item]("inventoryCardArchetypes",f => f.inventoryCardArchetypes, (f,inventoryCardArchetypes) => f.inventoryCardArchetypes = inventoryCardArchetypes, Item) 
	fields += "inventoryCardArchetypes" -> inventoryCardArchetypes
	val inventoryCards = Field.fromValue(Sentinel.inventoryCards).createField[Item]("inventoryCards",f => f.inventoryCards, (f,inventoryCards) => f.inventoryCards = inventoryCards, Item) 
	fields += "inventoryCards" -> inventoryCards
	val equippedCardArchetypes = Field.fromValue(Sentinel.equippedCardArchetypes).createField[Item]("equippedCardArchetypes",f => f.equippedCardArchetypes, (f,equippedCardArchetypes) => f.equippedCardArchetypes = equippedCardArchetypes, Item) 
	fields += "equippedCardArchetypes" -> equippedCardArchetypes
	val equippedCards = Field.fromValue(Sentinel.equippedCards).createField[Item]("equippedCards",f => f.equippedCards, (f,equippedCards) => f.equippedCards = equippedCards, Item) 
	fields += "equippedCards" -> equippedCards

	def apply(f : Item => Unit) : Item = { val v = new Item; f(v); v }
					 
	def copyInto(from : Item, to : Item) {
		to.durability = from.durability
		to.equipable = from.equipable
		to.equippedTo = from.equippedTo
		to.heldIn = from.heldIn
		to.wornOn = from.wornOn
		to.usesBodyParts = from.usesBodyParts
		to.equippedFlags = from.equippedFlags
		to.inventoryCardArchetypes = from.inventoryCardArchetypes
		to.inventoryCards = from.inventoryCards
		to.equippedCardArchetypes = from.equippedCardArchetypes
		to.equippedCards = from.equippedCards
	}
}
import arx.ax4.game.entities.CardData
object CardData extends Clazz[CardData]("CardData", classOf[CardData]){
	val Sentinel = new CardData
	override def instantiate = new CardData
	val costs = Field.fromValue(Sentinel.costs).createField[CardData]("costs",f => f.costs, (f,costs) => f.costs = costs, CardData) 
	fields += "costs" -> costs
	val effects = Field.fromValue(Sentinel.effects).createField[CardData]("effects",f => f.effects, (f,effects) => f.effects = effects, CardData) 
	fields += "effects" -> effects
	val triggeredEffects = Field.fromValue(Sentinel.triggeredEffects).createField[CardData]("triggeredEffects",f => f.triggeredEffects, (f,triggeredEffects) => f.triggeredEffects = triggeredEffects, CardData) 
	fields += "triggeredEffects" -> triggeredEffects
	val cardType = Field.fromValue(Sentinel.cardType).createField[CardData]("cardType",f => f.cardType, (f,cardType) => f.cardType = cardType, CardData) 
	fields += "cardType" -> cardType
	val name = Field.fromValue(Sentinel.name).createField[CardData]("name",f => f.name, (f,name) => f.name = name, CardData) 
	fields += "name" -> name
	val source = Field.fromValue(Sentinel.source).createField[CardData]("source",f => f.source, (f,source) => f.source = source, CardData) 
	fields += "source" -> source
	val exhausted = Field.fromValue(Sentinel.exhausted).createField[CardData]("exhausted",f => f.exhausted, (f,exhausted) => f.exhausted = exhausted, CardData) 
	fields += "exhausted" -> exhausted
	val inDeck = Field.fromValue(Sentinel.inDeck).createField[CardData]("inDeck",f => f.inDeck, (f,inDeck) => f.inDeck = inDeck, CardData) 
	fields += "inDeck" -> inDeck
	val attachments = Field.fromValue(Sentinel.attachments).createField[CardData]("attachments",f => f.attachments, (f,attachments) => f.attachments = attachments, CardData) 
	fields += "attachments" -> attachments
	val attachedCards = Field.fromValue(Sentinel.attachedCards).createField[CardData]("attachedCards",f => f.attachedCards, (f,attachedCards) => f.attachedCards = attachedCards, CardData) 
	fields += "attachedCards" -> attachedCards
	val attachedTo = Field.fromValue(Sentinel.attachedTo).createField[CardData]("attachedTo",f => f.attachedTo, (f,attachedTo) => f.attachedTo = attachedTo, CardData) 
	fields += "attachedTo" -> attachedTo
	val xp = Field.fromValue(Sentinel.xp).createField[CardData]("xp",f => f.xp, (f,xp) => f.xp = xp, CardData) 
	fields += "xp" -> xp

	def apply(f : CardData => Unit) : CardData = { val v = new CardData; f(v); v }
					 
	def copyInto(from : CardData, to : CardData) {
		to.costs = from.costs
		to.effects = from.effects
		to.triggeredEffects = from.triggeredEffects
		to.cardType = from.cardType
		to.name = from.name
		to.source = from.source
		to.exhausted = from.exhausted
		to.inDeck = from.inDeck
		to.attachments = from.attachments
		to.attachedCards = from.attachedCards
		to.attachedTo = from.attachedTo
		to.xp = from.xp
	}
}
import arx.ax4.game.entities.Physical
object Physical extends Clazz[Physical]("Physical", classOf[Physical]){
	val Sentinel = new Physical
	override def instantiate = new Physical
	val position = Field.fromValue(Sentinel.position).createField[Physical]("position",f => f.position, (f,position) => f.position = position, Physical) 
	fields += "position" -> position
	val offset = Field.fromValue(Sentinel.offset).createField[Physical]("offset",f => f.offset, (f,offset) => f.offset = offset, Physical) 
	fields += "offset" -> offset
	val colorTransforms = Field.fromValue(Sentinel.colorTransforms).createField[Physical]("colorTransforms",f => f.colorTransforms, (f,colorTransforms) => f.colorTransforms = colorTransforms, Physical) 
	fields += "colorTransforms" -> colorTransforms
	val facing = Field.fromValue(Sentinel.facing).createField[Physical]("facing",f => f.facing, (f,facing) => f.facing = facing, Physical) 
	fields += "facing" -> facing
	val occupiesHex = Field.fromValue(Sentinel.occupiesHex).createField[Physical]("occupiesHex",f => f.occupiesHex, (f,occupiesHex) => f.occupiesHex = occupiesHex, Physical) 
	fields += "occupiesHex" -> occupiesHex

	def apply(f : Physical => Unit) : Physical = { val v = new Physical; f(v); v }
					 
	def copyInto(from : Physical, to : Physical) {
		to.position = from.position
		to.offset = from.offset
		to.colorTransforms = from.colorTransforms
		to.facing = from.facing
		to.occupiesHex = from.occupiesHex
	}
}
import arx.ax4.game.entities.TurnData
object TurnData extends Clazz[TurnData]("TurnData", classOf[TurnData]){
	val Sentinel = new TurnData
	override def instantiate = new TurnData
	val turn = Field.fromValue(Sentinel.turn).createField[TurnData]("turn",f => f.turn, (f,turn) => f.turn = turn, TurnData) 
	fields += "turn" -> turn
	val activeFaction = Field.fromValue(Sentinel.activeFaction).createField[TurnData]("activeFaction",f => f.activeFaction, (f,activeFaction) => f.activeFaction = activeFaction, TurnData) 
	fields += "activeFaction" -> activeFaction

	def apply(f : TurnData => Unit) : TurnData = { val v = new TurnData; f(v); v }
					 
	def copyInto(from : TurnData, to : TurnData) {
		to.turn = from.turn
		to.activeFaction = from.activeFaction
	}
}
import arx.ax4.game.entities.Weapon
object Weapon extends Clazz[Weapon]("Weapon", classOf[Weapon]){
	val Sentinel = new Weapon
	override def instantiate = new Weapon
	val primaryAttack = Field.fromValue(Sentinel.primaryAttack).createField[Weapon]("primaryAttack",f => f.primaryAttack, (f,primaryAttack) => f.primaryAttack = primaryAttack, Weapon) 
	fields += "primaryAttack" -> primaryAttack
	val weaponSkills = Field.fromValue(Sentinel.weaponSkills).createField[Weapon]("weaponSkills",f => f.weaponSkills, (f,weaponSkills) => f.weaponSkills = weaponSkills, Weapon) 
	fields += "weaponSkills" -> weaponSkills
	val naturalWeapon = Field.fromValue(Sentinel.naturalWeapon).createField[Weapon]("naturalWeapon",f => f.naturalWeapon, (f,naturalWeapon) => f.naturalWeapon = naturalWeapon, Weapon) 
	fields += "naturalWeapon" -> naturalWeapon
	val attacks = Field.fromValue(Sentinel.attacks).createField[Weapon]("attacks",f => f.attacks, (f,attacks) => f.attacks = attacks, Weapon) 
	fields += "attacks" -> attacks
	val attackCards = Field.fromValue(Sentinel.attackCards).createField[Weapon]("attackCards",f => f.attackCards, (f,attackCards) => f.attackCards = attackCards, Weapon) 
	fields += "attackCards" -> attackCards

	def apply(f : Weapon => Unit) : Weapon = { val v = new Weapon; f(v); v }
					 
	def copyInto(from : Weapon, to : Weapon) {
		to.primaryAttack = from.primaryAttack
		to.weaponSkills = from.weaponSkills
		to.naturalWeapon = from.naturalWeapon
		to.attacks = from.attacks
		to.attackCards = from.attackCards
	}
}
import arx.ax4.game.entities.TagData
object TagData extends Clazz[TagData]("TagData", classOf[TagData]){
	val Sentinel = new TagData
	override def instantiate = new TagData
	val tags = Field.fromValue(Sentinel.tags).createField[TagData]("tags",f => f.tags, (f,tags) => f.tags = tags, TagData) 
	fields += "tags" -> tags
	val flags = Field.fromValue(Sentinel.flags).createField[TagData]("flags",f => f.flags, (f,flags) => f.flags = flags, TagData) 
	fields += "flags" -> flags

	def apply(f : TagData => Unit) : TagData = { val v = new TagData; f(v); v }
					 
	def copyInto(from : TagData, to : TagData) {
		to.tags = from.tags
		to.flags = from.flags
	}
}
import arx.ax4.game.entities.ResourceSourceData
object ResourceSourceData extends Clazz[ResourceSourceData]("ResourceSourceData", classOf[ResourceSourceData]){
	val Sentinel = new ResourceSourceData
	override def instantiate = new ResourceSourceData
	val resources = Field.fromValue(Sentinel.resources).createField[ResourceSourceData]("resources",f => f.resources, (f,resources) => f.resources = resources, ResourceSourceData) 
	fields += "resources" -> resources

	def apply(f : ResourceSourceData => Unit) : ResourceSourceData = { val v = new ResourceSourceData; f(v); v }
					 
	def copyInto(from : ResourceSourceData, to : ResourceSourceData) {
		to.resources = from.resources
	}
}
import arx.ax4.game.entities.Consumable
object Consumable extends Clazz[Consumable]("Consumable", classOf[Consumable]){
	val Sentinel = new Consumable
	override def instantiate = new Consumable
	val uses = Field.fromValue(Sentinel.uses).createField[Consumable]("uses",f => f.uses, (f,uses) => f.uses = uses, Consumable) 
	fields += "uses" -> uses

	def apply(f : Consumable => Unit) : Consumable = { val v = new Consumable; f(v); v }
					 
	def copyInto(from : Consumable, to : Consumable) {
		to.uses = from.uses
	}
}
import arx.ax4.game.entities.Equipment
object Equipment extends Clazz[Equipment]("Equipment", classOf[Equipment]){
	val Sentinel = new Equipment
	override def instantiate = new Equipment
	val equipped = Field.fromValue(Sentinel.equipped).createField[Equipment]("equipped",f => f.equipped, (f,equipped) => f.equipped = equipped, Equipment) 
	fields += "equipped" -> equipped

	def apply(f : Equipment => Unit) : Equipment = { val v = new Equipment; f(v); v }
					 
	def copyInto(from : Equipment, to : Equipment) {
		to.equipped = from.equipped
	}
}
import arx.ax4.game.entities.AllegianceData
object AllegianceData extends Clazz[AllegianceData]("AllegianceData", classOf[AllegianceData]){
	val Sentinel = new AllegianceData
	override def instantiate = new AllegianceData
	val faction = Field.fromValue(Sentinel.faction).createField[AllegianceData]("faction",f => f.faction, (f,faction) => f.faction = faction, AllegianceData) 
	fields += "faction" -> faction

	def apply(f : AllegianceData => Unit) : AllegianceData = { val v = new AllegianceData; f(v); v }
					 
	def copyInto(from : AllegianceData, to : AllegianceData) {
		to.faction = from.faction
	}
}
import arx.ax4.game.entities.ReactionData
object ReactionData extends Clazz[ReactionData]("ReactionData", classOf[ReactionData]){
	val Sentinel = new ReactionData
	override def instantiate = new ReactionData
	val currentReaction = Field.fromValue(Sentinel.currentReaction).createField[ReactionData]("currentReaction",f => f.currentReaction, (f,currentReaction) => f.currentReaction = currentReaction, ReactionData) 
	fields += "currentReaction" -> currentReaction
	val modifiersByType = Field.fromValue(Sentinel.modifiersByType).createField[ReactionData]("modifiersByType",f => f.modifiersByType, (f,modifiersByType) => f.modifiersByType = modifiersByType, ReactionData) 
	fields += "modifiersByType" -> modifiersByType

	def apply(f : ReactionData => Unit) : ReactionData = { val v = new ReactionData; f(v); v }
					 
	def copyInto(from : ReactionData, to : ReactionData) {
		to.currentReaction = from.currentReaction
		to.modifiersByType = from.modifiersByType
	}
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
					 
	def copyInto(from : FactionData, to : FactionData) {
		to.color = from.color
		to.playerControlled = from.playerControlled
	}
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
					 
	def copyInto(from : Tile, to : Tile) {
		to.entities = from.entities
		to.position = from.position
	}
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
					 
	def copyInto(from : CombatData, to : CombatData) {
		to.attackModifier = from.attackModifier
		to.conditionalAttackModifiers = from.conditionalAttackModifiers
		to.defenseModifier = from.defenseModifier
		to.conditionalDefenseModifiers = from.conditionalDefenseModifiers
		to.specialAttacks = from.specialAttacks
	}
}
import arx.ax4.game.entities.Vegetation
object Vegetation extends Clazz[Vegetation]("Vegetation", classOf[Vegetation]){
	val Sentinel = new Vegetation
	override def instantiate = new Vegetation
	val layers = Field.fromValue(Sentinel.layers).createField[Vegetation]("layers",f => f.layers, (f,layers) => f.layers = layers, Vegetation) 
	fields += "layers" -> layers

	def apply(f : Vegetation => Unit) : Vegetation = { val v = new Vegetation; f(v); v }
					 
	def copyInto(from : Vegetation, to : Vegetation) {
		to.layers = from.layers
	}
}
import arx.ax4.game.entities.QualitiesData
object QualitiesData extends Clazz[QualitiesData]("QualitiesData", classOf[QualitiesData]){
	val Sentinel = new QualitiesData
	override def instantiate = new QualitiesData
	val qualities = Field.fromValue(Sentinel.qualities).createField[QualitiesData]("qualities",f => f.qualities, (f,qualities) => f.qualities = qualities, QualitiesData) 
	fields += "qualities" -> qualities

	def apply(f : QualitiesData => Unit) : QualitiesData = { val v = new QualitiesData; f(v); v }
					 
	def copyInto(from : QualitiesData, to : QualitiesData) {
		to.qualities = from.qualities
	}
}
import arx.ax4.game.entities.CharacterInfo
object CharacterInfo extends Clazz[CharacterInfo]("CharacterInfo", classOf[CharacterInfo]){
	val Sentinel = new CharacterInfo
	override def instantiate = new CharacterInfo
	val species = Field.fromValue(Sentinel.species).createField[CharacterInfo]("species",f => f.species, (f,species) => f.species = species, CharacterInfo) 
	fields += "species" -> species
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
	val stamina = Field.fromValue(Sentinel.stamina).createField[CharacterInfo]("stamina",f => f.stamina, (f,stamina) => f.stamina = stamina, CharacterInfo) 
	fields += "stamina" -> stamina
	val staminaRecoveryRate = Field.fromValue(Sentinel.staminaRecoveryRate).createField[CharacterInfo]("staminaRecoveryRate",f => f.staminaRecoveryRate, (f,staminaRecoveryRate) => f.staminaRecoveryRate = staminaRecoveryRate, CharacterInfo) 
	fields += "staminaRecoveryRate" -> staminaRecoveryRate
	val bodyParts = Field.fromValue(Sentinel.bodyParts).createField[CharacterInfo]("bodyParts",f => f.bodyParts, (f,bodyParts) => f.bodyParts = bodyParts, CharacterInfo) 
	fields += "bodyParts" -> bodyParts
	val skillXP = Field.fromValue(Sentinel.skillXP).createField[CharacterInfo]("skillXP",f => f.skillXP, (f,skillXP) => f.skillXP = skillXP, CharacterInfo) 
	fields += "skillXP" -> skillXP
	val skillLevels = Field.fromValue(Sentinel.skillLevels).createField[CharacterInfo]("skillLevels",f => f.skillLevels, (f,skillLevels) => f.skillLevels = skillLevels, CharacterInfo) 
	fields += "skillLevels" -> skillLevels
	val strength = Field.fromValue(Sentinel.strength).createField[CharacterInfo]("strength",f => f.strength, (f,strength) => f.strength = strength, CharacterInfo) 
	fields += "strength" -> strength
	val dexterity = Field.fromValue(Sentinel.dexterity).createField[CharacterInfo]("dexterity",f => f.dexterity, (f,dexterity) => f.dexterity = dexterity, CharacterInfo) 
	fields += "dexterity" -> dexterity
	val intellect = Field.fromValue(Sentinel.intellect).createField[CharacterInfo]("intellect",f => f.intellect, (f,intellect) => f.intellect = intellect, CharacterInfo) 
	fields += "intellect" -> intellect
	val cunning = Field.fromValue(Sentinel.cunning).createField[CharacterInfo]("cunning",f => f.cunning, (f,cunning) => f.cunning = cunning, CharacterInfo) 
	fields += "cunning" -> cunning
	val innateCards = Field.fromValue(Sentinel.innateCards).createField[CharacterInfo]("innateCards",f => f.innateCards, (f,innateCards) => f.innateCards = innateCards, CharacterInfo) 
	fields += "innateCards" -> innateCards
	val perks = Field.fromValue(Sentinel.perks).createField[CharacterInfo]("perks",f => f.perks, (f,perks) => f.perks = perks, CharacterInfo) 
	fields += "perks" -> perks
	val pendingPerkPicks = Field.fromValue(Sentinel.pendingPerkPicks).createField[CharacterInfo]("pendingPerkPicks",f => f.pendingPerkPicks, (f,pendingPerkPicks) => f.pendingPerkPicks = pendingPerkPicks, CharacterInfo) 
	fields += "pendingPerkPicks" -> pendingPerkPicks

	def apply(f : CharacterInfo => Unit) : CharacterInfo = { val v = new CharacterInfo; f(v); v }
					 
	def copyInto(from : CharacterInfo, to : CharacterInfo) {
		to.species = from.species
		to.sex = from.sex
		to.health = from.health
		to.healthRecoveryRate = from.healthRecoveryRate
		to.alive = from.alive
		to.actionPoints = from.actionPoints
		to.moveSpeed = from.moveSpeed
		to.movePoints = from.movePoints
		to.stamina = from.stamina
		to.staminaRecoveryRate = from.staminaRecoveryRate
		to.bodyParts = from.bodyParts
		to.skillXP = from.skillXP
		to.skillLevels = from.skillLevels
		to.strength = from.strength
		to.dexterity = from.dexterity
		to.intellect = from.intellect
		to.cunning = from.cunning
		to.innateCards = from.innateCards
		to.perks = from.perks
		to.pendingPerkPicks = from.pendingPerkPicks
	}
}
import arx.ax4.game.entities.AttackModifier
object AttackModifier extends Clazz[AttackModifier]("AttackModifier", classOf[AttackModifier]){
	val Sentinel = new AttackModifier
	override def instantiate = new AttackModifier
	val nameOverride = Field.fromValue(Sentinel.nameOverride).createField[AttackModifier]("nameOverride",f => f.nameOverride, (f,nameOverride) => f.nameOverride = nameOverride, AttackModifier) 
	fields += "nameOverride" -> nameOverride
	val namePrefix = Field.fromValue(Sentinel.namePrefix).createField[AttackModifier]("namePrefix",f => f.namePrefix, (f,namePrefix) => f.namePrefix = namePrefix, AttackModifier) 
	fields += "namePrefix" -> namePrefix
	val accuracyBonus = Field.fromValue(Sentinel.accuracyBonus).createField[AttackModifier]("accuracyBonus",f => f.accuracyBonus, (f,accuracyBonus) => f.accuracyBonus = accuracyBonus, AttackModifier) 
	fields += "accuracyBonus" -> accuracyBonus
	val strikeCountBonus = Field.fromValue(Sentinel.strikeCountBonus).createField[AttackModifier]("strikeCountBonus",f => f.strikeCountBonus, (f,strikeCountBonus) => f.strikeCountBonus = strikeCountBonus, AttackModifier) 
	fields += "strikeCountBonus" -> strikeCountBonus
	val actionCostDelta = Field.fromValue(Sentinel.actionCostDelta).createField[AttackModifier]("actionCostDelta",f => f.actionCostDelta, (f,actionCostDelta) => f.actionCostDelta = actionCostDelta, AttackModifier) 
	fields += "actionCostDelta" -> actionCostDelta
	val actionCostMinimum = Field.fromValue(Sentinel.actionCostMinimum).createField[AttackModifier]("actionCostMinimum",f => f.actionCostMinimum, (f,actionCostMinimum) => f.actionCostMinimum = actionCostMinimum, AttackModifier) 
	fields += "actionCostMinimum" -> actionCostMinimum
	val staminaCostDelta = Field.fromValue(Sentinel.staminaCostDelta).createField[AttackModifier]("staminaCostDelta",f => f.staminaCostDelta, (f,staminaCostDelta) => f.staminaCostDelta = staminaCostDelta, AttackModifier) 
	fields += "staminaCostDelta" -> staminaCostDelta
	val staminaCostMinimum = Field.fromValue(Sentinel.staminaCostMinimum).createField[AttackModifier]("staminaCostMinimum",f => f.staminaCostMinimum, (f,staminaCostMinimum) => f.staminaCostMinimum = staminaCostMinimum, AttackModifier) 
	fields += "staminaCostMinimum" -> staminaCostMinimum
	val minRangeDelta = Field.fromValue(Sentinel.minRangeDelta).createField[AttackModifier]("minRangeDelta",f => f.minRangeDelta, (f,minRangeDelta) => f.minRangeDelta = minRangeDelta, AttackModifier) 
	fields += "minRangeDelta" -> minRangeDelta
	val minRangeOverride = Field.fromValue(Sentinel.minRangeOverride).createField[AttackModifier]("minRangeOverride",f => f.minRangeOverride, (f,minRangeOverride) => f.minRangeOverride = minRangeOverride, AttackModifier) 
	fields += "minRangeOverride" -> minRangeOverride
	val maxRangeDelta = Field.fromValue(Sentinel.maxRangeDelta).createField[AttackModifier]("maxRangeDelta",f => f.maxRangeDelta, (f,maxRangeDelta) => f.maxRangeDelta = maxRangeDelta, AttackModifier) 
	fields += "maxRangeDelta" -> maxRangeDelta
	val maxRangeOverride = Field.fromValue(Sentinel.maxRangeOverride).createField[AttackModifier]("maxRangeOverride",f => f.maxRangeOverride, (f,maxRangeOverride) => f.maxRangeOverride = maxRangeOverride, AttackModifier) 
	fields += "maxRangeOverride" -> maxRangeOverride
	val damageModifiers = Field.fromValue(Sentinel.damageModifiers).createField[AttackModifier]("damageModifiers",f => f.damageModifiers, (f,damageModifiers) => f.damageModifiers = damageModifiers, AttackModifier)
	fields += "damageModifiers" -> damageModifiers
	val targetPatternOverride = Field.fromValue(Sentinel.targetPatternOverride).createField[AttackModifier]("targetPatternOverride",f => f.targetPatternOverride, (f,targetPatternOverride) => f.targetPatternOverride = targetPatternOverride, AttackModifier) 
	fields += "targetPatternOverride" -> targetPatternOverride

	def apply(f : AttackModifier => Unit) : AttackModifier = { val v = new AttackModifier; f(v); v }
					 
	def copyInto(from : AttackModifier, to : AttackModifier) {
		to.nameOverride = from.nameOverride
		to.namePrefix = from.namePrefix
		to.accuracyBonus = from.accuracyBonus
		to.strikeCountBonus = from.strikeCountBonus
		to.actionCostDelta = from.actionCostDelta
		to.actionCostMinimum = from.actionCostMinimum
		to.staminaCostDelta = from.staminaCostDelta
		to.staminaCostMinimum = from.staminaCostMinimum
		to.minRangeDelta = from.minRangeDelta
		to.minRangeOverride = from.minRangeOverride
		to.maxRangeDelta = from.maxRangeDelta
		to.maxRangeOverride = from.maxRangeOverride
		to.damageModifiers = from.damageModifiers
		to.targetPatternOverride = from.targetPatternOverride
	}
}
import arx.ax4.game.entities.DefenseModifier
object DefenseModifier extends Clazz[DefenseModifier]("DefenseModifier", classOf[DefenseModifier]){
	val Sentinel = new DefenseModifier
	override def instantiate = new DefenseModifier
	val dodgeBonus = Field.fromValue(Sentinel.dodgeBonus).createField[DefenseModifier]("dodgeBonus",f => f.dodgeBonus, (f,dodgeBonus) => f.dodgeBonus = dodgeBonus, DefenseModifier) 
	fields += "dodgeBonus" -> dodgeBonus
	val armorBonus = Field.fromValue(Sentinel.armorBonus).createField[DefenseModifier]("armorBonus",f => f.armorBonus, (f,armorBonus) => f.armorBonus = armorBonus, DefenseModifier) 
	fields += "armorBonus" -> armorBonus

	def apply(f : DefenseModifier => Unit) : DefenseModifier = { val v = new DefenseModifier; f(v); v }
					 
	def copyInto(from : DefenseModifier, to : DefenseModifier) {
		to.dodgeBonus = from.dodgeBonus
		to.armorBonus = from.armorBonus
	}
}
import arx.ax4.game.entities.GatherMethod
object GatherMethod extends Clazz[GatherMethod]("GatherMethod", classOf[GatherMethod]){
	val Sentinel = new GatherMethod
	override def instantiate = new GatherMethod
	val name = Field.fromValue(Sentinel.name).createField[GatherMethod]("name",f => f.name, (f,name) => f.name = name, GatherMethod) 
	fields += "name" -> name
	val requirements = Field.fromValue(Sentinel.requirements).createField[GatherMethod]("requirements",f => f.requirements, (f,requirements) => f.requirements = requirements, GatherMethod) 
	fields += "requirements" -> requirements
	val actionCostDelta = Field.fromValue(Sentinel.actionCostDelta).createField[GatherMethod]("actionCostDelta",f => f.actionCostDelta, (f,actionCostDelta) => f.actionCostDelta = actionCostDelta, GatherMethod) 
	fields += "actionCostDelta" -> actionCostDelta
	val staminaCostDelta = Field.fromValue(Sentinel.staminaCostDelta).createField[GatherMethod]("staminaCostDelta",f => f.staminaCostDelta, (f,staminaCostDelta) => f.staminaCostDelta = staminaCostDelta, GatherMethod) 
	fields += "staminaCostDelta" -> staminaCostDelta
	val skills = Field.fromValue(Sentinel.skills).createField[GatherMethod]("skills",f => f.skills, (f,skills) => f.skills = skills, GatherMethod) 
	fields += "skills" -> skills
	val difficulty = Field.fromValue(Sentinel.difficulty).createField[GatherMethod]("difficulty",f => f.difficulty, (f,difficulty) => f.difficulty = difficulty, GatherMethod) 
	fields += "difficulty" -> difficulty
	val amount = Field.fromValue(Sentinel.amount).createField[GatherMethod]("amount",f => f.amount, (f,amount) => f.amount = amount, GatherMethod) 
	fields += "amount" -> amount
	val gatherFlags = Field.fromValue(Sentinel.gatherFlags).createField[GatherMethod]("gatherFlags",f => f.gatherFlags, (f,gatherFlags) => f.gatherFlags = gatherFlags, GatherMethod) 
	fields += "gatherFlags" -> gatherFlags

	def apply(f : GatherMethod => Unit) : GatherMethod = { val v = new GatherMethod; f(v); v }
					 
	def copyInto(from : GatherMethod, to : GatherMethod) {
		to.name = from.name
		to.requirements = from.requirements
		to.actionCostDelta = from.actionCostDelta
		to.staminaCostDelta = from.staminaCostDelta
		to.skills = from.skills
		to.difficulty = from.difficulty
		to.amount = from.amount
		to.gatherFlags = from.gatherFlags
	}
}
import arx.ax4.game.entities.VegetationLayer
object VegetationLayer extends Clazz[VegetationLayer]("VegetationLayer", classOf[VegetationLayer]){
	val Sentinel = new VegetationLayer
	override def instantiate = new VegetationLayer
	val cover = Field.fromValue(Sentinel.cover).createField[VegetationLayer]("cover",f => f.cover, (f,cover) => f.cover = cover, VegetationLayer) 
	fields += "cover" -> cover
	val moveCost = Field.fromValue(Sentinel.moveCost).createField[VegetationLayer]("moveCost",f => f.moveCost, (f,moveCost) => f.moveCost = moveCost, VegetationLayer) 
	fields += "moveCost" -> moveCost
	val kind = Field.fromValue(Sentinel.kind).createField[VegetationLayer]("kind",f => f.kind, (f,kind) => f.kind = kind, VegetationLayer) 
	fields += "kind" -> kind

	def apply(f : VegetationLayer => Unit) : VegetationLayer = { val v = new VegetationLayer; f(v); v }
					 
	def copyInto(from : VegetationLayer, to : VegetationLayer) {
		to.cover = from.cover
		to.moveCost = from.moveCost
		to.kind = from.kind
	}
}
import arx.ax4.game.entities.Resource
object Resource extends Clazz[Resource]("Resource", classOf[Resource]){
	val Sentinel = new Resource
	override def instantiate = new Resource
	val kind = Field.fromValue(Sentinel.kind).createField[Resource]("kind",f => f.kind, (f,kind) => f.kind = kind, Resource) 
	fields += "kind" -> kind
	val amount = Field.fromValue(Sentinel.amount).createField[Resource]("amount",f => f.amount, (f,amount) => f.amount = amount, Resource) 
	fields += "amount" -> amount
	val recoveryAmount = Field.fromValue(Sentinel.recoveryAmount).createField[Resource]("recoveryAmount",f => f.recoveryAmount, (f,recoveryAmount) => f.recoveryAmount = recoveryAmount, Resource) 
	fields += "recoveryAmount" -> recoveryAmount
	val recoveryPeriod = Field.fromValue(Sentinel.recoveryPeriod).createField[Resource]("recoveryPeriod",f => f.recoveryPeriod, (f,recoveryPeriod) => f.recoveryPeriod = recoveryPeriod, Resource) 
	fields += "recoveryPeriod" -> recoveryPeriod
	val canRecoverFromZero = Field.fromValue(Sentinel.canRecoverFromZero).createField[Resource]("canRecoverFromZero",f => f.canRecoverFromZero, (f,canRecoverFromZero) => f.canRecoverFromZero = canRecoverFromZero, Resource) 
	fields += "canRecoverFromZero" -> canRecoverFromZero
	val structural = Field.fromValue(Sentinel.structural).createField[Resource]("structural",f => f.structural, (f,structural) => f.structural = structural, Resource) 
	fields += "structural" -> structural
	val gatherMethods = Field.fromValue(Sentinel.gatherMethods).createField[Resource]("gatherMethods",f => f.gatherMethods, (f,gatherMethods) => f.gatherMethods = gatherMethods, Resource) 
	fields += "gatherMethods" -> gatherMethods

	def apply(f : Resource => Unit) : Resource = { val v = new Resource; f(v); v }
					 
	def copyInto(from : Resource, to : Resource) {
		to.kind = from.kind
		to.amount = from.amount
		to.recoveryAmount = from.recoveryAmount
		to.recoveryPeriod = from.recoveryPeriod
		to.canRecoverFromZero = from.canRecoverFromZero
		to.structural = from.structural
		to.gatherMethods = from.gatherMethods
	}
}
import arx.ax4.game.entities.Skill
object Skill extends Clazz[Skill]("Skill", classOf[Skill]){
	val Sentinel = new Skill
	override def instantiate = new Skill
	val displayName = Field.fromValue(Sentinel.displayName).createField[Skill]("displayName",f => f.displayName, (f,displayName) => f.displayName = displayName, Skill) 
	fields += "displayName" -> displayName
	val levelUpPerks = Field.fromValue(Sentinel.levelUpPerks).createField[Skill]("levelUpPerks",f => f.levelUpPerks, (f,levelUpPerks) => f.levelUpPerks = levelUpPerks, Skill) 
	fields += "levelUpPerks" -> levelUpPerks

	def apply(f : Skill => Unit) : Skill = { val v = new Skill; f(v); v }
					 
	def copyInto(from : Skill, to : Skill) {
		to.displayName = from.displayName
		to.levelUpPerks = from.levelUpPerks
	}
}
import arx.ax4.game.entities.TagInfo
object TagInfo extends Clazz[TagInfo]("TagInfo", classOf[TagInfo]){
	val Sentinel = new TagInfo
	override def instantiate = new TagInfo
	val kind = Field.fromValue(Sentinel.kind).createField[TagInfo]("kind",f => f.kind, (f,kind) => f.kind = kind, TagInfo) 
	fields += "kind" -> kind
	val hidden = Field.fromValue(Sentinel.hidden).createField[TagInfo]("hidden",f => f.hidden, (f,hidden) => f.hidden = hidden, TagInfo) 
	fields += "hidden" -> hidden

	def apply(f : TagInfo => Unit) : TagInfo = { val v = new TagInfo; f(v); v }
					 
	def copyInto(from : TagInfo, to : TagInfo) {
		to.kind = from.kind
		to.hidden = from.hidden
	}
}
}
