

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


    CardMainSection {
      type : Div

      x : 5
      y : 0 below CardImage
      width : rel(-10)
      height : expand to parent

      children : {
        CardEffects {
          type : ListWidget

          y : centered
          width : 100%
          height : wrapContent
          drawBackground : false

          listItemArchetype : CardWidgets.CardEffect
          listItemBinding: "card.effects -> effect"
          listItemGapSize : 30
          separator : "CardWidgets.CardEffectGroupDivider"
          selectable : true
        }
      }
    }

  }

}

CardEffect {
  type : TextDisplayWidget

  drawBackground : false

  text : "%(effect)"

  textAlignment : centered
  width : 100%

  fontScale : 2
}

CardEffectGroupDivider {
  type : ImageDisplayWidget

  image : "graphics/ui/card_divider.png"
  scalingStyle : scale(200%)
  x : centered

  drawBackground : false
}



CardPileWidget {
  type : ImageDisplayWidget

  drawBackground : false

  image : "%(pile.icon)"
  showing : "%(pile.showing)"

  scalingStyle : scale(200%)

  children {
    CardCountWidget: {
      type: TextDisplayWidget

      drawBackground : false

      text: "%(pile.cardCount)"
      fontScale : 3
      fontColor : [0,0,0,1]

      x: 0 from right
      y: 0 from bottom
    }
  }
}