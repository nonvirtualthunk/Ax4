Flags {

  Parry {
    description : "Parry incoming attacks, reducing their chance to hit"
    tickDownOn : DodgeEvent
    resetAtEndOfTurn : true
    hidden : false
    countsAs : DefenseDelta
  }

  Block {
    description : "Block incoming attacks, reducing the damage they deal"
    tickDownOn : ArmorUsedEvent
    resetAtEndOfTurn : true
    hidden : false
    countsAs : ArmorDelta
  }

  Tiring {
    description : "Becomes increasingly tiring the more it is used in a turn. Costs [N] additional stamina each time it is used."
    resetAtEndOfTurn : true
    hidden : false
    countsAs : StaminaCostDelta
  }




  ApCostDelta {
    description : "AP Cost delta"
    resetAtEndOfTurn : false
    hidden : true
  }

  StaminaCostDelta {
    description : "Stamina Cost delta"
    resetAtEndOfTurn : false
    hidden : true
  }

  AccuracyDelta {
    description : "Accuracy delta"
    resetAtEndOfTurn : false
    hidden : true
  }

  DefenseDelta {
    description : "Defense delta"
    resetAtEndOfTurn : false
    hidden : true
  }

  ArmorDelta {
    description : "Armor delta"
    resetAtEndOfTurn : false
    hidden : true
  }
}