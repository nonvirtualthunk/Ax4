package arx.ax4.graphics.resources

import arx.Prelude
import arx.Prelude.toArxString
import arx.ax4.game.entities.{CharacterInfo, Terrain, Vegetation}
import arx.ax4.graphics.resources.Tileset.{TerrainImageset, VegetationImageset}
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.AxialVec
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.graphics.Image
import arx.graphics.helpers.Color
import arx.resource.ResourceManager
import arx.Prelude.toRicherIterable
import arx.ax4.graphics.resources.CharacterImageset.SpeciesImageset
import arx.engine.world.WorldView

class Tileset {
	var terrain = Map[Taxon, TerrainImageset]()
	var vegetation = Map[Taxon, VegetationImageset]()

	def drawInfoFor(position : AxialVec, terrain : Terrain, vegetation: Vegetation) = {
		val TerrainImageset(default,_) = imagesetForTerrain(terrain.kind)

		val mainLayer = vegetation.layers.sortBy(_.layer)
			.flatMap(l => baseImageLayerForTerrainVegetation(position, terrain.kind, l.kind))
   		.headOption
   		.getOrElse(ImageLayer(default.pickBasedOn(position, terrain.kind), Color.White))

		List(mainLayer)
	}

	def imagesetForTerrain(tKind : Taxon) = tKind.selfAndAncestorsUpTo(Taxonomy("Terrain")).toStream
		.flatMap(terrain.get)
		.headOption
		.getOrElse(Tileset.DefaultTerrainImageset)

	def imagesetForVegetation(vKind : Taxon) = vKind.selfAndAncestorsUpTo(Taxonomy("Vegetation")).toStream
   	.flatMap(vegetation.get)
   	.headOption
   	.getOrElse(Tileset.DefaultVegetationImageset)

	def baseImageLayerForTerrainVegetation(position : AxialVec, tKind : Taxon, vKind : Taxon) = {
		val TerrainImageset(_, byVeg) = imagesetForTerrain(tKind)
		// todo: this will be weird with hierarchicals that match multiple since it does depth first, effectively
		vKind.selfAndAncestorsUpTo(Taxonomy("Vegetation")).toStream.flatMap(byVeg.get).headOption
   		.map(vi => ImageLayer(vi.imageset.pickBasedOn(position, vKind), Color.White))
	}
}

object Tileset {
	private val DefaultImage = ResourceManager.image("third-party/zeshioModified/07 Canyon/02 Ravine/PixelHex_zeshio_tile-492.png")
	private val DefaultImageset = Imageset(Some(DefaultImage), Vector())
	private val DefaultTerrainImageset = TerrainImageset(DefaultImageset,Map())
	private val DefaultVegetationImageset = VegetationImageset(DefaultImageset, replaces = false)

	case class TerrainImageset(defaultImageset : Imageset, imagesetsByVegetationKind : Map[Taxon, VegetationImageset])

	case class VegetationImageset(imageset : Imageset, replaces : Boolean)


	def load(config : ConfigValue) = {
		val tileset = new Tileset
		for (fullTerrainConf <- config.expectField("Terrain"); (terrainName, terrainConf) <- fullTerrainConf.fields) {
			for (terrainTaxon <- Taxonomy.getByName(terrainName)) {
				val defaultImageset = Imageset.loadFrom(terrainConf.Default)
				val byTerrainImagesets = terrainConf.fields.filter(k => k._1 != "Default").flatMap {
					case (k,v) => Taxonomy.getByName(k).map(ttx => ttx -> loadVegetationImageset(v))
				}
				tileset.terrain += terrainTaxon -> TerrainImageset(defaultImageset, byTerrainImagesets)
			}
		}

		for (terrainConf <- config.expectField("Vegetation"); (vegName, vegConfig) <- terrainConf.fields) {
			for (vegTaxon <- Taxonomy.getByName(vegName)) {
				tileset.vegetation += vegTaxon -> loadVegetationImageset(vegConfig)
			}
		}
		tileset
	}

	def loadVegetationImageset(config : ConfigValue) = {
		VegetationImageset(Imageset.loadFrom(config), config.replaces.boolOrElse(false))
	}
}

class CharacterImageset {
	var species = Map[Taxon, SpeciesImageset]()

	def imagesetForSpecies(species : Taxon) = this.species.getOrElse(species, CharacterImageset.DefaultSpeciesImageset)

	def drawInfoFor(implicit view : WorldView, character : Entity, display : WorldView) : Seq[ImageLayer] = {
		val characterInfo = view.data[CharacterInfo](character)
		val speciesImageset = species.getOrElse(characterInfo.species, CharacterImageset.DefaultSpeciesImageset)
		val specificImageset = speciesImageset.bySex.getOrElse(characterInfo.sex, speciesImageset.ungendered)
		val img = specificImageset.pickBasedOn(character)
		List(ImageLayer(img, Color.White))
	}
}

object CharacterImageset {
	private val DefaultImage = ResourceManager.image("third-party/oryx/creatures_24x24/oryx_16bit_fantasy_creatures_279.png")
	private val DefaultSpeciesImageset = SpeciesImageset(Map(), Imageset(Some(DefaultImage), Vector()))

	private val sexes = Taxonomy.descendantsOf("sex")



	case class SpeciesImageset(bySex : Map[Taxon, Imageset], ungendered : Imageset)
//	case class ClassImageset()

	def load(config : ConfigValue) = {
		val charset = new CharacterImageset

		for (overallSpeciesConf <- config.fieldOpt("Species")) {
			for ((speciesName,speciesConf) <- overallSpeciesConf.fields ; speciesKind = Taxonomy(speciesName)) {
				charset.species += speciesKind -> loadSpeciesImageset(speciesConf)
			}
		}

		charset
	}

	def loadSpeciesImageset(config : ConfigValue) = {

		val genderedImagesets = sexes.filter(s => config.hasField(s.name.toLowerCase()))
   			.map(s => s -> Imageset.loadFrom(config.field(s.name.toLowerCase())))
   			.toMap
		SpeciesImageset(genderedImagesets, Imageset.loadFrom(config))
	}
}