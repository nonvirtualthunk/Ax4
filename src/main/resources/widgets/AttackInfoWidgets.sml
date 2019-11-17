
BasicAttackText {
  type : TextDisplayWidget
  fontScale : 1.5
  drawBackground : false
}


BasicAttackInfo {
  type : Div

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
      text : "%(attack.accuracyBonus)  %(attack.damage)"
      x : "20 right of AttackName"
    }
  }
}