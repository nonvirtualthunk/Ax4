Skills {

  SpearSkill : {
    displayName : "Spear"

    levelUpPerks : {
      PiercingStab {
        levelRange : 1-3
      }
      SpearProficiency {
        levelRange : 1-4
      },
      SpearMastery {
        levelRange : 2-5
        requires : Perk(SpearProficiency)
      },
      CloseRangeSpearFighter {
        levelRange : 2-5
        requires : Perk(SpearProficiency)
      }
    }
  }

  UnarmedSkill : {
    displayName : "Unarmed"

    levelUpPerks : {

    }
  }
}

Perks {
  PiercingStab {
    name : "Piercing Stab"
    description : "Strike through multiple enemies with a powerful stab"
    icon : "graphics/icons/card_back_large.png"
    effect : AddCard(PiercingStab)
  }
  SwiftStab {
    name : "Swift Stab"
    description : "Stab swiftly at an enemy"
    icon : "graphics/icons/card_back_large.png"
    effect : AddCard(SwiftStab)
  }
  SpearProficiency {
    name : "Spear Proficiency"
    description : "Become proficient in the use of the spear and increase your accuracy and damage"
    icon : "graphics/icons/spear.png"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)

      modifier {
        accuracyBonus: 1
        damageBonus: 1
      }
    }
  }
  SpearMastery {
    name : "Spear Mastery"
    description : "Master the use of the spear and increase your accuracy and damage"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)

      modifier {
        accuracyBonus: 2
        damageBonus: 2
      }
    }
  }
  CloseRangeSpearFighter {
    name : "Close Range Spear Fighter"
    description : "Learn how to strike with your spear more flexibly allowing you to strike foes at closer range"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)
      modifier {
        minRangeDelta : -1
      }
    }
  }
}