File {
  root: "third-party/zeshioModified/"
}

treePath : "16 Trees"

Terrain {
  Flatland : {
    Default : {
      textures: {
        primary: "03 Dirt/01 Solid Tiles/PixelHex_zeshio_tile-032.png"
        variants: [
          "03 Dirt/01 Solid Tiles/PixelHex_zeshio_tile-030.png",
          "03 Dirt/01 Solid Tiles/PixelHex_zeshio_tile-031.png"]
      }
    }

    Grass : {
      replaces : true
      textures: {
        primary: "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-001.png"
        variants: [
          "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-002.png",
          "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-003.png",
          "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-004.png",
          "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-005.png",
          "01 Grass/01 Solid Tiles/PixelHex_zeshio_tile-006.png"]
      }
    }

    Forest : {
      textures: {
        primary: ${treePath}"/PixelHex_zeshio_tile-1197.png"
        variants: [
          ${treePath}"/PixelHex_zeshio_tile-1196.png",
          ${treePath}"/PixelHex_zeshio_tile-1195.png",
          ${treePath}"/PixelHex_zeshio_tile-1198.png",
          ${treePath}"/PixelHex_zeshio_tile-1204.png",
          ${treePath}"/PixelHex_zeshio_tile-1206.png",
        ]
      }
    }
  }

  Hills : {
    Default : {
      textures: {
        primary: "05 Mountains/01 Solid Tiles/PixelHex_zeshio_tile-168.png"
      }
    }

    Grass : {
      textures: {
        replaces : true
        primary: "01 Grass/03 Hills/PixelHex_zeshio_tile-020.png"
        variantChance: 0.2
        variants: [
          "01 Grass/03 Hills/PixelHex_zeshio_tile-019.png",
          "01 Grass/03 Hills/PixelHex_zeshio_tile-021.png",
          "01 Grass/03 Hills/PixelHex_zeshio_tile-022.png"
        ]
      }
    }

    Forest : {
      textures : {
        primary: ${treePath}"/PixelHex_zeshio_tile-1203.png"
        variants: [
          ${treePath}"/PixelHex_zeshio_tile-1204.png",
          ${treePath}"/PixelHex_zeshio_tile-1195.png"
        ]
      }
    }
  }

  Mountains : {
    Default : {
      textures : {
        primary: "05 Mountains/01 Solid Tiles/PixelHex_zeshio_tile-165.png"
        variantChance : 0.5
        variants: [
          "05 Mountains/01 Solid Tiles/PixelHex_zeshio_tile-167.png"
          "05 Mountains/01 Solid Tiles/PixelHex_zeshio_tile-1191.png"
          "05 Mountains/01 Solid Tiles/PixelHex_zeshio_tile-1193.png"
        ]
      }
    }

    Forest : {
      textures : {
        primary: ${treePath}"/PixelHex_zeshio_tile-1203.png"
      }
    }
  }
}