
BasicAttackText {
  type : TextDisplayWidget
  fontScale : 1.5
  drawBackground : false
}


BasicAttackInfo {
  type : Window

  dimensions : [100%, WrapContent]
  backgroundPixelScale : 1
  background.image : "ui/fancyBackground_ns.png"

  children : {
    SelectedIcon : {
      type : ImageDisplayWidget
      image : "third-party/DHGZ/frame16.png"
      drawBackground : false
      scalingStyle : scale(200%)
      y : centered

      children : {
        SelectedIconFill {
          type : ImageDisplayWidget
          image : "third-party/DHGZ/frame16_fill.png"
          scalingStyle : scale(200%)
          drawBackground : false

          color : "%(attack.selectedColor)"
        }
      }
    }
    AttackName : ${BasicAttackText} {
      text : "%(attack.name)"
      x : "10 right of SelectedIcon"
    }
    AttackInfo : ${BasicAttackText} {
      text: [
        {text: "%(attack.accuracyBonus)"},
        {taxon: "GameConcepts.Accuracy", scale : 2}
        {text: "%(attack.damage)"}
      ]
      x : "20 right of AttackName"
    }
//    AttackInfo : ${BasicAttackText} {
//      text : "%(attack.accuracyBonus)  %(attack.damage)"
//      x : "20 right of AttackName"
//    }
  }
}

ConsideredAttackInfo {
  type : Div

  backgroundPixelScale : 1
  background.image : ui/fancyBackground_ns.png
  drawBackground : true

  children {
    AttackerSide : {
      type : Div

      children {
        AttackInfo : ${BasicAttackText} {
          text: [
            {text: "%(attack.accuracyBonus)"},
            {taxon: "GameConcepts.Accuracy", scale : 1}
            {text: "%(attack.damage)"}
          ]
        }
      }
    }

    VsDivider : ${BasicAttackText} {
      text : "-vs-"
      x : 10 right of AttackerSide
      y : centered
    }

    DefenderSide : {
      type : Div

      x : 10 right of VsDivider

      children {
        Defense : ${BasicAttackText} {
          text: [
            {text: "%(attack.defense)"},
            {taxon: "GameConcepts.Defense"}
            {text: "%(attack.defenderHP) HP"}
          ]
        }
//        Defense : ${BasicAttackText} {
//          text : "Defense: %(attack.defense)"
//        }
//        HP : ${BasicAttackText} {
//          y : 0 below Defense
//          text : "HP: %(attack.defenderHP)"
//        }
      }
    }
  }
}