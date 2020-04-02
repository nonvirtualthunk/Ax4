Cards {

  Harvest {
    name: Harvest
    apCost: 2
    staminaCost: 1
    effects: [Gather(1)]
  }

  PiercingStab {
    name: Piercing Stab

    specialAttack: piercingStab
  }

  SwiftStab {
    name: Swift Stab

    specialAttack: swiftStab

    effects: [Draw(1)]
  }

  Parry {
    name: Parry

    apCost: 1
    staminaCost: 1
    effects: [Parry(1)]

    xp: Parry -> 1
  }

  Block {
    name: Block

    apCost: 1
    staminaCost: 1
    effects: [Block(1)]

    xp: Block -> 1
  }

  FlurryOfBlows {
    name: Flurry of Blows

    apCost: 0
    staminaCost: 0
    effects: [
      {
        type: Attack

        name: flurry of blows
        accuracyBonus: -1
        strikeCount: 1
        minRange: 0
        maxRange: 1
        damage: 1d4 Bludgeoning
      }
    ]

    selfEffects : [Tiring(1)]

    triggeredEffects: [
      {
        trigger: {
          type: OnCardPlay
          playedCardCondition: isA(AttackCard)
          sourceCardCondition: cardIsIn(DiscardPile)
        },
        effect : MoveCardTo(Hand)
      }
    ]
  }
}