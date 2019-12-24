Taxonomy {
//  UnknownThing {}
//  Material {
//    Wood : {
//      Ironwood : ""
//    }
//  }

  UnknownThing : []
  Material : []
  Materials {
    Wood : Material
    Stone : Material
    Metal : Material

    Ironwood : Wood

    Hay : Material

  }

  LivingThing : []

  Species : []
  Specieses {
    Humanoid : Species
    Monstrous : Species

    Human : Humanoid
    MudMonster : Monstrous
    Slime : Monstrous
  }

  Item : []
  Items {
    RawMaterial : Item
    RefinedMaterial : Item

    Foodstuff : Item
    HumanFoodstuff : Foodstuff
    AnimalFoodstuff : Foodstuff

    Consumable : Item
    Potion : [Consumable]

    Tool : Item
    FineCuttingTool : Tool
    SturdyCuttingTool : Tool

    Log : RawMaterial
    Plank : RefinedMaterial

    IronwoodLog : Log
    IronwoodPlank : Plank

    HayBushel : [RawMaterial, AnimalFoodstuff]

    StaminaPotion : Potion

    Weapon : Item
    Axe : [Item, SturdyCuttingTool]

    Weapons {
      BattleAxe: [Weapon, Axe]
      Sword: Weapon
      Longsword: Sword
      Shortsword: Sword

      Spear : Weapon
      Longspear : Spear
      Shortspear : Spear

      Scythe : [Weapon, Tool, SturdyCuttingTool]
    }
  }

  AttackType : []
  AttackTypes {
    PhysicalAttack : AttackType
    SlashingAttack : PhysicalAttack
    StabbingAttack : PhysicalAttack

    NaturalAttack : AttackType

    MeleeAttack : AttackType
    RangedAttack : AttackType
    ReachAttack : AttackType
  }

  Terrain : []
  Terrains {
    Flatland : Terrain
    Hills : Terrain
    Mountains : Terrain

    Plateaus : Hills
  }

  Vegetation : []
  Vegetations {
    Grass : Vegetation
    Forest : Vegetation
    DeciduousForest : Forest
    EvergreenForest : Forest
    Jungle : Forest
  }

  CharacterClass : []
  CharacterClasses {
    CombatClass : CharacterClass
    MeleeCombatClass : CombatClass
    RangedCombatClass : CombatClass
    MagicClass : CharacterClass
  }

  Action : []
  Actions {
    DoNothing : Action
    MoveAction : Action
    AttackAction : Action
    GatherAction : Action
    SwitchSelectedCharacterAction : Action
  }

  Sex : []
  Sexes {
    Ungendered : Sex
    Male : Sex
    Female : Sex
  }


  Skill : []
  Skills {
    WeaponSkill : Skill
    ArmorSkill : Skill

    MeleeSkill : Skill

    SpearSkill : [WeaponSkill, MeleeSkill]
    SwordSkill : [WeaponSkill, MeleeSkill]
    AxeSkill : [WeaponSkill, MeleeSkill]
    ShieldSkill : [ArmorSkill, MeleeSkill]

    UnarmedSkill : [WeaponSkill, MeleeSkill]
  }


  Reaction : []
  Reactions {
    Parry : Reaction
    Defend : Reaction
    Block : Reaction
    Counter : Reaction
    Dodge : Reaction
  }


  GameConcept : []
  GameConcepts {
    AccuracyBonus : GameConcept
    DefenseBonus : GameConcept
    ArmorValue : GameConcept

  }

  DamageType : []
  DamageTypes {
    Physical : DamageType

    Piercing : Physical
    Bludgeoning : Physical
    Slashing : Physical
    Unknown : DamageType
  }

  BodyPart : []
  BodyParts {
    Gripping : BodyPart
    Thinking : BodyPart
    Appendage : BodyPart
    Dextrous : BodyPart

    Hand : Gripping
    Pseudopod : [Gripping, Dextrous, Appendage]
    Arm : [Dextrous, Appendage]
    Leg : Appendage
    Head : Thinking
  }

  CardType : []
  CardTypes {
    AttackCard : CardType
    SkillCard : CardType
    ItemCard : CardType
    SpellCard : CardType
    ActionCard : CardType
    MoveCard : CardType
    GatherCard : CardType

    Harvest : GatherCard
    Gather : GatherCard
    Move : MoveCard
    Slash : AttackCard
  }

  Flag : []
  Flags {
    Harvester : Flag
  }

  Tag : []
  Tags {
    Tool : Tag
  }
}