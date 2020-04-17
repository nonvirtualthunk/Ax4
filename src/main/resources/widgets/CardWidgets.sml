

CardWidget {
  type : Window

  dimensions : [500, 900]
  background.image : "graphics/ui/card_border.png"
  backgroundPixelScale : 2
//  consumeMouseButtonEvents: true

  children {
    CardName {
      type : TextDisplayWidget

      text : "%(card.name)"
      drawBackground : false
      fontScale : 2
      textAlignment : center
      width : 80%
      x : centered
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

      fontScale : 1.5
    }

    CardSecondaryCost {
      type : TextDisplayWidget

      drawBackground : false

      text : "%(card.secondaryCost)"

      x : 5
      y : 0

      fontScale : 1.5
    }

//    CardTags {
//      type : TextDisplayWidget
//
//      drawBackground : false
//
//      text : "%(card.tags)"
//
//      x : 5
//      y : 0 below CardImage
//      width : rel(-10)
//      textAlignment : centered
//      showing : "%(card.hasTags)"
//
//      fontScale : 2
//    }

    CardMainSection {
      type : Div

      x : 0
      y : 0 below CardImage
      width : 100%
      height : expand to parent

      children : {
        CardPrimaryEffect {
          type : TextDisplayWidget

          drawBackground : false

          text : "%(card.effects)"

          y : centered
          width : 100%

          textAlignment : centered

          fontScale : 2
        }
      }
    }

  }

}