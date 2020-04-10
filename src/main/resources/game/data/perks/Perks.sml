Perks {
//  PiercingStab {
//    name: "Piercing Stab"
//    description: "Strike through multiple enemies with a powerful stab"
//    icon: "graphics/icons/card_back_large.png"
//    effect: AddCard(PiercingStab)
//  }
//  SwiftStrike {
//    name: "Swift Stab"
//    description: "Stab swiftly at an enemy"
//    icon: "graphics/icons/card_back_large.png"
//    effect: AddCard(SwiftStrike)
//  }
//  Parry {
//    name: "Parry"
//    description: "Defend yourself by parrying incoming attacks with your weapon"
//    icon: "graphics/icons/card_back_large.png"
//    effect: AddCard(Parry)
//  }.

  SpearProficiency {
    name: "Spear Proficiency"
    description: "Become proficient in the use of the spear and increase your accuracy and damage"
    icon: "graphics/icons/spear.png"
    effect {
      type: ConditionalAttackModifier

      condition: WeaponIs(Items.Weapons.spear)

      modifier {
        accuracyBonus: 1
        damageBonus: 1
      }
    }
  }
  SpearMastery {
    name: "Spear Mastery"
    description: "Master the use of the spear and increase your accuracy and damage"
    effect {
      type: ConditionalAttackModifier

      condition: WeaponIs(Items.Weapons.spear)

      modifier {
        accuracyBonus: 2
        damageBonus: 2
      }
    }
  }
  CloseRangeSpearFighter {
    name: "Close Range Spear Fighter"
    description: "Learn how to strike with your spear more flexibly, allowing you to strike foes at closer range"
    effect {
      type: ConditionalAttackModifier

      condition: WeaponIs(Items.Weapons.spear)
      modifier {
        minRangeDelta: -1
      }
    }
  }
  MeleeFighter {
    name: "Melee Fighter"
    description: "Learn how to fight better at close range"
    icon: "graphics/icons/melee.png"
    effects: [
      {
        type: ConditionalAttackModifier

        condition: AttackIs(MeleeAttack)

        modifier {
          accuracyBonus: 1
        }
      },
      "MaxHP(+1)"
    ]
  }
}