

CardWidget {
  type : Window

  dimensions : [400, 600]
  background.image : "graphics/ui/card_border.png"
  backgroundPixelScale : 2
  consumeMouseButtonEvents: true

  children {
    CardName {
      type : TextDisplayWidget

      text : "- %(card.name) -"
      drawBackground : false
      fontScale : 2
      textAlignment : center
      width : 100%
    }
  }

}