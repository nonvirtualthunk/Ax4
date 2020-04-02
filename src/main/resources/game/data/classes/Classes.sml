Classes {

  Spearman {
    description : "A soldier trained in wielding a spear (more words here)"

    levelUpPerks: {
      SpearProficiency {
        levelRange: 1
        free: true
      },
      SpearMastery {
        levelRange: 2-5
        requires: Perk(SpearProficiency)
      },
      CloseRangeSpearFighter {
        levelRange: 2-5
        requires: Perk(SpearProficiency)
      }
    }
  }
}