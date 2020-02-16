BasicStatusDisplay {
  type : TextDisplayWidget

  drawBackground : false
  fontScale : 1.5

  text : [
    {text : "%(status.status)", scale : 2}
    {text : "%(status.number)", scale : 1}
  ]
}