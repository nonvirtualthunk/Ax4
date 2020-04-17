BasicStatusDisplay {
  type : TextDisplayWidget

  drawBackground : false
  fontScale : 2

  text : [
    {text : "%(status.status)", scale : 1}
    {text : "%(status.number)", scale : 0.75}
  ]
}