

ActionSelectionButton {
  consumeMouseButtonEvents : true
}

ActionSelectionButtonBar {
  type : Div
  drawBackground : false

  dimensions : [rel(-500), WrapContent]
  x : 10
  y : "10 from bottom"

  children : {
    SpecialAttackButton : ${ActionSelectionButton} {
      type : ImageDisplayWidget
      image : "third-party/DHGZ/sword1.png"
      background.image : "ui/button_ne.png"

      scalingStyle: scale(400%)

      position : [0,0]
    }
  }
}

SpecialAttackSelectionList {
  type: Div

  background.image : "ui/greenWoodBorder.png"

  x : 10
  y : "150 from bottom"

  dimensions : [600, WrapContent]

  consumeMouseButtonEvents : true

  children : {
    Label {
      type : TextDisplayWidget

      width : 100%
      drawBackground : false

      textAlignment : Centered
      text : "Special Attacks"
      fontScale : 2
    }

    AttackList {
      type : ListWidget

      y : "4 below Label"

      listItemArchetype : AttackInfoWidgets.BasicAttackInfo
      listItemBinding: "specialAttacks -> attack"

      dimensions : [100%, WrapContent]

      drawBackground : false
    }
  }
}
