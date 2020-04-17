Cards {

  SlimeSmash {
    name: Slime Smash
    apCost : 1
    staminaCost : 0
    effects: [
      {
        type : Attack

        name : slime smash
        accuracyBonus : 0
        strikeCount : 1
        minRange : 0
        maxRange : 1
        damage : 1d4 Bludgeoning

        onHitTargetEffects : [
          AddCard(Slime)
        ]
      }
    ]
  }


}