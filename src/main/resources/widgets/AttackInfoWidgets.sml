
BasicAttackText {
  type : TextDisplayWidget
  fontScale : 1.5
  drawBackground : false
}


BasicAttackInfo {
  type : Div

  dimensions : [100%, WrapContent]
  backgroundPixelScale : 1
  background.image : "ui/greenWoodBorder.png"

  children : {
    AttackName : ${BasicAttackText} {
      text : "%(attack.name)"
    }
    AttackInfo : ${BasicAttackText} {
      text : "%(attack.accuracyBonus)  %(attack.damage)"
      x : "20 right of AttackName"
    }
  }
}