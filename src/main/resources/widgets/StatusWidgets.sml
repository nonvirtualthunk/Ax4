BasicStatusDisplay {
  type : TextDisplayWidget

  drawBackground : false
  fontScale : 1

  text : [
    {text : "%(status.status)", scale : 3}
    {text : "%(status.number)", scale : 2}
  ]
}