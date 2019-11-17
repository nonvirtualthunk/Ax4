

ResourceSelectionWidget {
  type : Div

  position : [centered, centered]
  dimensions : [60%, WrapContent]

  consumeMouseButtonEvents : true

  children : {
    Title : {
      type : TextDisplayWidget
      position : [centered, 0]
      dimensios : [intrinsic, intrinsic]

      fontScale : 2
      text : "Gather Resource"
      drawBackground : false
    }

    ResourceSelectionList {
      type : ListWidget
      y : "0 below Title"
      dimensions : [100%, WrapContent]
      drawBackground : false

      listItemArchetype : ResourceSelectionWidgets.ResourceSelectionItem
      listItemBinding : "possibleResources -> resource"
    }

  }
}


ResourceSelectionItem {
  type : Div

  background.image : "ui/fancyBackground_ns.png"
  height : WrapContent
  width : 100%

  children {
    ResourceIcon {
      type : ImageDisplayWidget

      image : "%(resource.resourceIcon)"
      color : "%(resource.iconColor)"
      scalingStyle : ScaleToFit

      drawBackground : false

      height : 64
      width : 64
    }
    YieldDisplay {
      type : TextDisplayWidget
      x : 0 right of ResourceIcon
      y : Centered

      text : "x%(resource.amount) (%(resource.remainingAmount))"
      fontColor : "%(resource.fontColor)"

      drawBackground : false

      fontScale : 1.5
    }
    MethodDisplay {
      type : TextDisplayWidget

      x : 20 right of YieldDisplay
      y : centered

      text : "%(resource.methodName)"
      fontColor : "%(resource.fontColor)"

      drawBackground : false

      fontScale : 1.5
    }
    DisabledReasonDisplay {
      type : TextDisplayWidget

      x : 10 right of MethodDisplay
      y : centered

      text : "%(resource.disabledReason)"
      fontColor : "%(resource.fontColor)"

      drawBackground : false

      fontScale : 1.5
    }
  }
}