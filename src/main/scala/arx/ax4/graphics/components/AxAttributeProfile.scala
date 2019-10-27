package arx.ax4.graphics.components

import arx.graphics.AttributeProfile
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_UNSIGNED_BYTE}

object AxAttributeProfile extends AttributeProfile(List("vertex" -> (3, GL_FLOAT), "texCoord" -> (2, GL_FLOAT), "color" -> (4, GL_UNSIGNED_BYTE), "visionPcnt" -> (1, GL_FLOAT), "lightColor" -> (4,GL_UNSIGNED_BYTE))) {
	val VertexAttribute = attributesByName("vertex")
	val TexCoordAttribute = attributesByName("texCoord")
	val ColorAttribute = attributesByName("color")
	val VisionAttribute = attributesByName("visionPcnt")
	val LightColorAttribute = attributesByName("lightColor")
}