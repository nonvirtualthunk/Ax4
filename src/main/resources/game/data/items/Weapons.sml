Weapons {
  Longspear : {
    durability : 25

    usesBodyParts : {
      gripping : 2
    }

    primaryAttack : primary // redundant, since "primary" is the default primary attack key

    attacks : {
      primary : {
        name : stab
        accuracyBonus : 1
        strikeCount : 1
        staminaCostPerStrike : 2
        minRange : 1
        maxRange : 2
        damage : 1d8 Slashing
      }
    }
  }
}