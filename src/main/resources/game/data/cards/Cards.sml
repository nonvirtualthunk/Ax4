Cards {

  Move {
    name : Move

    cardEffectGroups : [
      {
        costs : [AP(1), Stamina(0)]
        effects: [Move(3)]
      },
      {
        name : Hurry
        costs : [AP(2), Stamina(1)]
        effects: [Move(5)]
      }
    ]


    xp: Move -> 1
  }

  Gather {
    name : Gather
    apCost : 2
    staminaCost : 0
    effects: [Gather(1)]

    xp: Gather -> 1
  }

  Harvest {
    name: Harvest
    apCost: 2
    staminaCost: 1
    effects: [Gather(1)]

    xp: Gather -> 1
  }

  PiercingStab {
    name: Piercing Stab

    effects : [
      {
        type : SpecialAttack

        nameOverride: PiercingStab
        accuracyBonus : -1
        targetPatternOverride : "line(1,2)"
        minRangeOverride: 1
        maxRangeOverride: 1

        condition : [HasDamageType(Piercing), HasAtLeastMaxRange(2)]
      }
    ]

    tags: [Expend]

    xp : WeaponSkill -> 2
  }

  SwiftStrike {
    name: Swift Strike

    effects: [
      {
        type: SpecialAttack

        nameOverride: Swift Strike
        accuracyBonus: -1
        damageBonus : -1
        staminaCostDelta : 1
      },
      Draw(1)
    ]

    xp : WeaponSkill -> 2
  }

  SweepingLegStrike {
    name : Sweeping Leg Strike
    tags : [Expend]

    effects: [
      {
        type: SpecialAttack
        name: Sweeping Leg Strike
        damageBonus : 1
        triggeredEffects : [{
          trigger : Hit
          affects : Target
          effect : Slow(1)
        }]
      }
    ]

    xp : WeaponSkill -> 2
  }

  RingingBlow {
    name : Ringing Blow
    tags : [Expend]

    effects: [
      {
        type: SpecialAttack
        name: Ringing Blow
        damageModifiers : [
          {
            predicate : "primary"
            delta : damageType(bludgeoning)
          }
        ]
        onHitTargetEffects : [Stunned(1)]
      }
    ]

    xp : WeaponSkill -> 2
  }

  DoubleStrike {
    name : Double Strike
    tags : [Expend]

    effects: [
      {
        type : SpecialAttack
        name : Double Strike
        strikeCountMultiplier : 2
        staminaCostMultiplier : 2
      }
    ]
  }

  ChargingStrike {
    name : Charging Strike
    tags : [Expend]

    effects : [
      Move(2),
      {
        type : SpecialAttack
        name : Charging Strike
        damageMultiplier : 2
        actionCostDelta : 1
      }
    ]
  }

  FlashingPoints {
    name : Flashing Points
    tags : [Expend]

    effectsDescription : "Every strike applies 2 [dazzled] for the rest of the turn"

    effects : [FlashingPoints(1)]
  }

  Parry {
    name: Parry

    apCost: 1
    staminaCost: 1
    effects: [Parry(3)]

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
        type: SpecialAttack

        nameOverride: flurry of blows
        accuracyBonus: -1
        baseDamageOverride: 1d4 Bludgeoning

        actionCostDelta: -1
        actionCostMinimum: 0
        staminaCostDelta: -1
        staminaCostMinimum: 0

        condition : [IsUnarmedAttack]
      }
    ]

    selfEffects : [Tiring(1)]

    triggeredEffects: [
      {
        trigger: {
          type: OnCardPlay
          playedCardCondition: isA(NaturalAttackCard)
          sourceCardCondition: cardIsIn(DiscardPile)
        },
        effect : MoveCardTo(Hand),
        description : "Return to hand whenever an unarmed attack is made"
      }
    ]

    xp: UnarmedSkill -> 1
  }
}