

ResourceSelectionWidget {
  type : Window

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
  type : Window

  background.image : "ui/fancyBackground_ns.png"
  height : WrapContent
  width : 100%

  backgroundColor : "%(resource.iconColor)"
  edgeColor : "%(resource.iconColor)"

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

      fontScale : 2
    }
    MethodDisplay {
      type : TextDisplayWidget

      x : 20 right of YieldDisplay
      y : centered

      text : "%(resource.methodName)"
      fontColor : "%(resource.fontColor)"

      drawBackground : false

      fontScale : 2
    }
    DisabledReasonDisplay {
      type : TextDisplayWidget

      x : 10 right of MethodDisplay
      y : centered

      text : "%(resource.disabledReason)"
      fontColor : "%(resource.fontColor)"

      drawBackground : false

      fontScale : 2
    }

    CostDiv {
      type : Div

      x : 0 from right
      y : centered

      showing : "%(resource.hasExtraCosts)"

      children {
        ActionPointCost {
          type : TextDisplayWidget

          x : 0

          drawBackground : false

          fontScale : 2

          text : [
            {text : "%(resource.method.actionCostDelta)"}
            {horizontalPadding : 6}
            {image : "graphics/ui/action_point.png", scale : 2, color : "%(resource.iconColor)"}
          ]
        }
        StaminaCost {
          type : TextDisplayWidget

          x : 10 right of ActionPointCost

          drawBackground : false

          fontScale : 2

          text : [
            {text : "%(resource.method.staminaCostDelta)"}
            {horizontalPadding : 6}
            {image : "graphics/ui/stamina_point.png", scale : 2, color : "%(resource.iconColor)"}
          ]
        }
      }
    }
  }
}