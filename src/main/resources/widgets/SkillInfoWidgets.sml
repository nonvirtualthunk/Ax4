
BasicSkillText {
  type : TextDisplayWidget
  fontScale : 1.5
  drawBackground : false
}

BasicSkillInfo : {
  type : Div

  dimensions : [100%, WrapContent]
  backgroundPixelScale : 1
  background.image : "ui/fancyBackground_ns.png"

  children : {
    SkillIcon : {
      type : ImageDisplayWidget
      image : "%(skill.icon)"
      drawBackground : true
      background.image : "ui/minimalistBorder_ne.png"
      scalingStyle : scale(200%)
      y : centered
    }

    SkillText : ${BasicSkillText} {
      x : "30 right of SkillIcon"
      text : "%(skill.name) %(skill.level)"
      fontScale : 1.5
    }

    SkillXpText : ${BasicSkillText} {
      x : "30 right of SkillIcon"
      y : "0 below SkillText"
      text : "XP: %(skill.currentXp)/%(skill.requiredXp)"
      fontScale : 1
    }
  }
}