

CardWidget {
  type : Window

  dimensions : [450, 800]
  background.image : "graphics/ui/card_border.png"
  backgroundPixelScale : 2
  consumeMouseButtonEvents: true

  children {
    CardName {
      type : TextDisplayWidget

      text : "%(card.name)"
      drawBackground : false
      fontScale : 2
      textAlignment : center
      width : 100%
    }

    CardImage {
      type : ImageDisplayWidget

      image : "%(card.image)"
      scalingStyle : scale(400%)
      positionStyle : center

      x : centered
      y : 5 below CardName

      width : 350
      height : 250

      background.image : "ui/fancyBackgroundWhite_ns.png"
      backgroundPixelScale : 1
      backgroundColor : [0.9,0.88,0.85,1.0]
    }

    CardMainCost {
      type : TextDisplayWidget

      drawBackground : false

      text : "%(card.mainCost)"

      x : 0 from right
      y : 0

      fontScale : 2
    }

    CardSecondaryCost {
      type : TextDisplayWidget

      drawBackground : false

      text : "%(card.secondaryCost)"

      x : 5
      y : 0

      fontScale : 2
    }

    CardPrimaryEffect {
      type : TextDisplayWidget

      drawBackground : false

      text : "%(card.effects)"

      x : 5
      y : 265
      width : rel(-10)

      textAlignment : centered

      fontScale : 2
    }
  }

}