leftMargin : 10


StatDisplay : {
  type : TextDisplayWidget

  position : [${leftMargin}, 10]
  drawBackground : false
  background.image : "ui/singlePixelBorder_ne.png"

  fontScale : 1.5
}


SelectedCharacterInfo {
  type : Div

  position : ["0 from Right", 0]
  dimensions : [500, "100%"]

  backgroundPixelScale : 1
  background.image : "ui/greenWoodBorder.png"

  backgroundEdges : [0]

  children : {
    Portrait : {
      type : ImageDisplayWidget

      position : ["10 from Right", "10 from Top"]
      dimensions : [150, 150]

      scalingStyle: Scale(4)
      positionStyle: Center

      image : "%(selectedCharacter.portrait)"
      backgroundEdges : [0,1,2,3]
      background.image : "ui/minimalistBorder_ne.png"
    }
    Name : ${StatDisplay} {
      y : 10
      text : "%(selectedCharacter.name)"
      fontScale : 2
    }
    Health : ${StatDisplay} {
      y : "10 below Name"
      text : "Health: %(selectedCharacter.health.cur) / %(selectedCharacter.health.max)"
    }
    Actions : ${StatDisplay} {
      y : "0 below Health"
      text : "Actions: %(selectedCharacter.actions.cur) / %(selectedCharacter.actions.max)"
    }
    Move : ${StatDisplay} {
      y : "0 below Actions"
      text : "Move: %(selectedCharacter.move.cur) / %(selectedCharacter.move.max)"
    }
    Speed : ${StatDisplay} {
      y : "0 below Move"
      text : "Speed: %(selectedCharacter.speed)"
    }


    InfoTabs : {
      type : TabWidget

      tabs : [
        { heading : Attacks , tab : AttackDisplay }
        { heading : Skills , tab : SkillDisplay}
      ]

      y : "0 below Speed"
      dimensions : [100%, 600]

      drawBackground : false

      tabHeight : 75

      children : {
        AttackDisplay : {
          type : ListWidget

          listItemArchetype : AttackInfoWidgets.BasicAttackInfo
          listItemBinding : "attacks -> attack"

          backgroundPixelScale : 1
          background.image : "ui/greenWoodBorder.png"
          drawBackground : true
        },

        SkillDisplay : {
          type : ListWidget

          listItemArchetype : SkillInfoWidgets.BasicSkillInfo
          listItemBinding : "skills -> skill"

          backgroundPixelScale : 1
          background.image : "ui/greenWoodBorder.png"
          drawBackground : true
        }
      }
    }

  }
}
