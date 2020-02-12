PerkSelectionWidget {
  type : Div
  x : centered
  y : 20
  width : 90%
  height : 90%
  z : 50

  children : {
    Heading : {
      type : TextDisplayWidget
      text : "%(perkSelection.source)"
      background.image : "ui/greenMinorStyledBorder_ne.png"
      backgroundPixelScale : 2
      fontScale : 2
      x : centered
    }
    PerkOptions : {
      type : DynamicWidget
      background.image : "ui/greenMinorStyledBorder_ne.png"
      backgroundPixelScale : 2

      x : 0
      y : 10 below Heading
      width : 100%
      height : rel(-120)
    }
  }
}

PerkInfoWidget {
  type : Div
  width : 25%
  height : 100%
  drawBackground : true
  background.image: "ui/fancyBackgroundWhite_ns.png"
  backgroundColor: [100,100,100,255]
  backgroundPixelScale : 2

  overlays {
    selectedOverlay : {
      image: "graphics/ui/fancySelectedOverlay.png"
      drawOverlay: "%(perkInfo.selected)"
      pixelScale: 2
    }
  }

  children : {
    PerkIcon : {
      type : ImageDisplayWidget
      drawBackground : false
      image : "%(perkInfo.perk.icon)"
//      showing : "%(perkInfo.hasIcon)"
      showing : true
      scalingStyle : scale to height 96px

      x : centered
      y : 10
    }

    PerkName : {
      type : TextDisplayWidget
      drawBackground : false
      text : "%(perkInfo.perk.name)"
      fontScale : 2

      y : 10 below PerkIcon
      x : centered
    }

    PerkDescription : {
      type : TextDisplayWidget
      drawBackground : false
      text : "%(perkInfo.perk.description)"
      fontScale : 1.5
      width : 100%

      y : 10 below PerkName
      x : centered
      textAlignment : center
    }

    PerkEffects : {
      type : Div
      y : 50 below PerkDescription
      x : centered
    }
  }
}