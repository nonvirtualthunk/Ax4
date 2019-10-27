package arx.ax4.graphics.resources

import arx.Prelude
import arx.core.representation.ConfigValue
import arx.graphics.Image
import arx.graphics.helpers.Color
import arx.resource.ResourceManager
import arx.Prelude.toArxString

case class ImageLayer (image : Image, color : Color)

case class Imageset(primary : Option[Image], variants : Vector[Image], variantChance : Float = 0.33f) {
	def pickBasedOn(a : Any*) = {
		primary match {
			case Some(prim) if variants.isEmpty => prim
			case Some(prim) =>
				val r = Prelude.permute(a.hashCode())
				if (r % (1.0f / variantChance).round == 0) {
					variants(Prelude.permute(r) % variants.size)
				} else {
					prim
				}
			case None if variants.nonEmpty =>
				val r = Prelude.permute(a.hashCode())
				variants(Prelude.permute(r) % variants.size)
			case None => ResourceManager.defaultImage
		}
	}
}
object Imageset {
	def loadFrom(config : ConfigValue) = {
		val prefix = config.rootConfigValue.fieldOpt("File").flatMap(_.fieldOpt("root")).map(c => c.str).getOrElse("").ensureEndsWith("/")

		val textures = config.field("textures")
		val primary = textures.fieldOpt("primary").map(p => ResourceManager.image(prefix + p.str))
		val texturesField = if (textures.isArr) { Some(textures) } else { textures.fieldOpt("variants") }
		val variants = texturesField.flatMap(v => v.arrOpt).map(cl => cl.map(
			v => ResourceManager.image(prefix + v.str)
		)).getOrElse(Vector()).toVector

		val variantChance = if (primary.isDefined) { textures.field("variantChance").floatOrElse(0.33f) } else { 1.0f }

		Imageset(primary, variants, variantChance)
	}
}
