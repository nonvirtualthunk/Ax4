Skills {

  SpearSkill : {
    displayName : "Spear"
    levels : [

    ]

    levelUpEffects : {
      PiercingStab {
        levelRange : 1-3
      }
      SpearProficiency {
        levelRange : 1-4
      },
      SpearMastery {
        levelRange : 2-5
        requires : SpearProficiency
      },
      CloseRangeSpearFighter {
        levelRange : 2-5
        requires : SpearProficiency
      }
    }
  }
}

LevelUpEffects {
  PiercingStab {
    name : "Piercing Stab"
    description : "Strike through multiple enemies with a powerful stab"
    effect : SpecialAttack(PiercingStab)
  }
  SwiftStab {
    name : "Swift Stab"
  }
  SpearProficiency {
    name : "Spear Proficiency"
    description : "Become proficient in the use of the spear and increase your accuracy and damage"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)

      accuracyBonus : 1
      damageBonus : 1
    }
  }
  SpearMastery {
    name : "Spear Mastery"
    description : "Master the use of the spear and increase your accuracy and damage"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)

      accuracyBonus : 2
      damageBonus : 2
    }
  }
  CloseRangeSpearFighter {
    name : "Close Range Spear Fighter"
    description : "Learn how to strike with your spear more flexibly allowing you to strike foes at closer range"
    effect {
      type : ConditionalAttackModifier

      condition : WeaponIs(Items.Weapons.spear)

      minRangeDelta : -1
    }
  }
}