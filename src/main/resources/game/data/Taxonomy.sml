Taxonomy {
//  UnknownThing {}
//  Material {
//    Wood : {
//      Ironwood : ""
//    }
//  }

  UnknownThing : []
  Material : []

  Wood : Material
  Stone : Material
  Metal : Material

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
  Weapon : Item
  Axe : Item

  Weapons {
    BattleAxe: [Weapon, Axe]
    Sword: Weapon
    Longsword: Sword
    Shortsword: Sword

    Spear : Weapon
    Longspear : Spear
    Shortspear : Spear
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
    MoveAction : Action
    AttackAction : Action
    GatherAction : Action
    SwitchActiveCharacterAction : Action
  }

  Sex : []
  Sexes {
    Ungendered : Sex
    Male : Sex
    Female : Sex
  }
}