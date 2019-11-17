

InventoryWidget {
  type : Div

  position : [centered, centered]
  dimensions : [45%, 80%]
  z : 1

  background.image : ui/greenWoodBorder.png

  children {
    InventoryLabel {
      type : TextDisplayWidget

      text : Inventory
      fontScale : 2
      textAlignment : Centered
      width : 100%

      drawBackground : false
    }

    ItemList {
      type : ListWidget

      listItemArchetype : InventoryWidgets.ItemListItem
      listItemBinding : "items -> item"

      y : 0 below InventoryLabel
      width : 100%
      height : WrapContent
      drawBackground : false
    }
  }
}

ItemListItem {
  type : Div

  background.image : "ui/fancyBackground_ns.png"
  width : 100%
  height : WrapContent

  children {
    IconDisplay {
      type : ImageDisplayWidget

      image : "%(item.icon)"
      scalingStyle : ScaleToFit

      drawBackground : false

      height : 64
      width : 64
    }
    NameDisplay {
      type : TextDisplayWidget

      text : "%(item.name)"
      fontScale : 1.5

      drawBackground : false
      y : centered
      x : 10 right of IconDisplay
    }
    UseButton {
      type : ImageDisplayWidget

      image : "third-party/DHGZ/use_stencil.png"
      scalingStyle : ScaleToFit

      drawBackground : false
      height : 48
      width : 48

      x : 0 from right
      y : centered
//      TODO: Figure out a way of handling center-in-wrap-content
//      TODO: Related, figure out how to have WrapContent in height but a child that has a relative in X not require a recursive dependency

      showing : "%(item.consumable)"
    }
  }
}