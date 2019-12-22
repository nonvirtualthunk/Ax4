Terrain {
  Flatland: {
    fertility : 0
    cover : 0
    elevation : 0
    moveCost : 1
    kind : flatland
  }

  Hills: {
    fertility : 0
    cover : 1
    elevation : 1
    moveCost : 2
    kind: hills
  }

  Mountains: {
    fertility : -1
    cover : 1
    elevation : 2
    moveCost : 3
    kind: mountains
  }

  Plateaus : {
    fertility : 0
    cover : 1
    elevation : 1
    moveCost : 2
    kind : plateaus
  }
}