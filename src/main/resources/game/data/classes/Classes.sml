Classes {

  Soldier {
    description : "A soldier trained in combat"

    cardRewards {
      Parry {
        targetLevel: 1
        free : true
      }
    }

    levelUpPerks: {
      ArmorProficiency {
        targetLevel: 1
        free : true
      }
      SpearProficiency {
        targetLevel : 1
      }
      SpearMastery {
        targetLevel : 2
        requires: Perk(SpearProficiency)
      }
      CloseRangeSpearFighter {
        targetLevel : 3
        requires: Perk(SpearProficiency)
      }
    }
  }
}