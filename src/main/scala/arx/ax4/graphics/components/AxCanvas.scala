package arx.ax4.graphics.components

import arx.application.Noto
import arx.core.introspection.CopyAssistant
import arx.core.vec.{ReadVec2f, Vec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, CartVec, Hex}
import arx.engine.simple.{CustomCanvas, TQuadBuilder}
import arx.graphics.Image
import arx.graphics.helpers.{Color, HSBA}
import arx.resource.ResourceManager

class AxCanvas(var hexSize : Int) extends CustomCanvas[AxQuadBuilder](AxAttributeProfile) {
	override protected def createQuadBuilderImpl(): AxQuadBuilder = new AxQuadBuilder(hexSize, blankTC)

	def quad(v : AxialVec) = createQuadBuilder().position(v)
	def quad(v : CartVec) = createQuadBuilder().position(v)
	def quad(v : ReadVec2f) = createQuadBuilder().position(v)
	def quad() = createQuadBuilder()
}


class AxQuadBuilder(hexSize : Int, blankTC : Array[ReadVec2f]) extends TQuadBuilder {
	protected var _forward = Vec2f.UnitX
	protected var _ortho = Vec2f.UnitY
	protected var _dimensions : ReadVec2f = Vec2f(hexSize, hexSize)
	protected var _color : Color = Color.White
	protected var _texCoords = blankTC
	protected var _textureIndexRotation = 0
	protected var _position : ReadVec2f = Vec2f.Zero
	protected var _visionPcnt : Float = 1.0f
	protected var _relativeOrigin : ReadVec2f = Vec2f.Zero
	protected var _lightColor : Color = Color.White
	protected var _hexBottomOrigin : Boolean = false
	protected var _layer : DrawLayer = DrawLayer.Terrain
	protected var _hexOffset : CartVec = CartVec.Zero

	def copy() = CopyAssistant.copyShallow(this)


	def offset(v : ReadVec2f) : this.type = {_position += v; this }
	def forward(v : ReadVec2f) = { _forward = v; this }
	def ortho(v : ReadVec2f) = { _ortho = v; this }
	def dimensions(v : CartVec) = { _dimensions = v * hexSize; this }
	def dimensions(w : Float, h : Float) = { _dimensions = Vec2f(w,h); this }
	def dimensions(v : ReadVec2f) = { _dimensions = v; this }
	def color(color : Color) = { _color = color; this }
	def texture(t : String) = { _texCoords = textureBlock.getOrElseUpdate(ResourceManager.image(t)); this }
	def texture(t : Image) = { _texCoords = textureBlock.getOrElseUpdate(t); this }
	def texture(t : String, scale : Int) : this.type = {texture(ResourceManager.image(t), scale)}
	def texture(t : Image, scale : Int) : this.type = {
		_texCoords = textureBlock.getOrElseUpdate(t)
		_dimensions = Vec2f(t.width * scale, t.height * scale)
		this
	}
	def texCoords(tc : Array[ReadVec2f]) = { _texCoords = tc; this }
	def textureIndexRotation(tir : Int) = { _textureIndexRotation = tir; this }
	def position(v : AxialVec) = { _position = v.asCartesian(hexSize); this }
	def position(v : CartVec) = { _position = v * hexSize; this }
	def position(v : ReadVec2f) = { _position = v; this }
	def visionPcnt(p : Float) = { _visionPcnt = p; this }
	def lightColor(c : Color) = { _lightColor = c; this }
	def hexBottomOrigin(offsetX : Float = 0.0f, offsetY : Float = 0.0f) = { _hexBottomOrigin = true; _relativeOrigin = Vec2f(0.0f,-1.0f); _hexOffset = CartVec(offsetX, offsetY); this }
	def layer(l : DrawLayer) = { _layer = l; this }
	/**
	 * Unscaled origin, generally in the range [-1,1] with (-1,-1) being bottom left origin, (1,1) being top right
	 */
	def relativeOrigin(v : ReadVec2f) = { _relativeOrigin = v; this }

	def draw(): Unit = {
		val vi = vbo.incrementVertexOffset(4)
		val ii = vbo.incrementIndexOffset(6)

		val hsba = _color.asHSBA
		val lightRGBA = _lightColor.asRGBA

		val posX = _position.x
		val posY = if (_hexBottomOrigin) {
			_position.y - (Hex.heightForSize(hexSize) * 0.5f).toInt
		} else {
			_position.y
		}

		val originX = (posX + _forward.x * _dimensions.x * _relativeOrigin.x * -0.5f + _ortho.x  * _dimensions.y * _relativeOrigin.y * -0.5f + _hexOffset.x * hexSize).toInt
		val originY = (posY + _forward.y * _dimensions.x * _relativeOrigin.x * -0.5f + _ortho.y  * _dimensions.y * _relativeOrigin.y * -0.5f + _hexOffset.y * Hex.heightForSize(hexSize)).toInt

		vbo.setA(AxAttributeProfile.VertexAttribute, vi + 0,
			((originX + _forward.x * _dimensions.x * -0.5f + _ortho.x * _dimensions.y * -0.5f).toInt / 2) * 2,
			((originY + _forward.y * _dimensions.x * -0.5f + _ortho.y * _dimensions.y * -0.5f).toInt / 2) * 2,
			_layer.depth)
		vbo.setA(AxAttributeProfile.VertexAttribute, vi + 1,
			((originX + _forward.x * _dimensions.x * +0.5f + _ortho.x * _dimensions.y * -0.5f).toInt / 2) * 2,
			((originY + _forward.y * _dimensions.x * +0.5f + _ortho.y * _dimensions.y * -0.5f).toInt / 2) * 2,
			_layer.depth)
		vbo.setA(AxAttributeProfile.VertexAttribute, vi + 2,
			((originX + _forward.x * _dimensions.x * +0.5f + _ortho.x * _dimensions.y * +0.5f).toInt / 2) * 2,
			((originY + _forward.y * _dimensions.x * +0.5f + _ortho.y * _dimensions.y * +0.5f).toInt / 2) * 2,
			_layer.depth)
		vbo.setA(AxAttributeProfile.VertexAttribute, vi + 3,
			((originX + _forward.x * _dimensions.x * -0.5f + _ortho.x * _dimensions.y * +0.5f).toInt / 2) * 2,
			((originY + _forward.y * _dimensions.x * -0.5f + _ortho.y * _dimensions.y * +0.5f).toInt / 2) * 2,
			_layer.depth)
		var i = 0
		while (i < 4) {
			vbo.setA(AxAttributeProfile.VisionAttribute, vi + i, _visionPcnt)
			vbo.setAbf(AxAttributeProfile.ColorAttribute, vi + i, hsba.h, hsba.s, hsba.b, hsba.a, 255)
			vbo.setAbf(AxAttributeProfile.LightColorAttribute, vi + i, lightRGBA.r, lightRGBA.g, lightRGBA.b, lightRGBA.a, 255)
			vbo.setA(AxAttributeProfile.TexCoordAttribute, vi + i, _texCoords((i + _textureIndexRotation) % 4))
			i += 1
		}
		vbo.setIQuad(ii, vi)
	}
}

sealed class DrawLayer(val depth : Int)
object DrawLayer {
	case object Terrain extends DrawLayer(0)
	case object UnderEntity extends DrawLayer(4)
	case object Entity extends DrawLayer(5)
	case object OverEntity extends DrawLayer(6)
}