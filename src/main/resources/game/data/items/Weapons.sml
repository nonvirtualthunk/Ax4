Weapons {
  Longspear : {
    durability : 25

    weaponSkills : [spearSkill]

    usesBodyParts : {
      gripping : 2
    }

    primaryAttack : primary // redundant, since "primary" is the default primary attack key

    attacks : {
      primary : {
        name : stab
        actionCost : 2
        accuracyBonus : 1
        strikeCount : 1
        staminaCostPerStrike : 2
        minRange : 2
        maxRange : 2
        damage : 1d8 Piercing
      }

      secondary : {
        name : slam
        actionCost : 2
        accuracyBonus : -1
        strikeCount : 1
        staminaCostPerStrike : 1
        minRange : 1
        maxRange : 1
        damage : 1d4 Bludgeoning
      }
    }
  }
}