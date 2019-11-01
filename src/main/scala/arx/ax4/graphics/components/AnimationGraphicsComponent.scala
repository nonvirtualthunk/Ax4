package arx.ax4.graphics.components

import arx.Prelude._
import arx.ax4.game.entities.Companions.Physical
import arx.ax4.game.event.EntityMoved
import arx.ax4.graphics.data.{AxAnimatingWorldData, AxDrawingConstants, AxGraphicsData}
import arx.core.introspection.Field
import arx.core.math.Interpolation
import arx.core.units.UnitOfTime
import arx.core.vec.Vec2f
import arx.engine.data.TWorldAuxData
import arx.engine.entity.Entity
import arx.engine.graphics.components.{DrawPriority, GraphicsComponent}
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.EventState.Started
import arx.engine.world.{GameEventClock, HypotheticalWorld, HypotheticalWorldView, World, WorldQueryParser}

import scala.reflect.ClassTag

class AnimationGraphicsComponent extends GraphicsComponent {
	var animations = List[Animation]()

	override def drawPriority = DrawPriority.First

	override protected def onUpdate(game: World, display: World, dt: UnitOfTime, time: UnitOfTime): Unit = {
		val animData = display[AxAnimatingWorldData]
		val compData = display[AnimationGraphicsComponent.Data]


		animData.hypotheticalWorld = new HypotheticalWorld(game, animData.currentGameWorldView)
		// something something animations
		val advance = animations.isEmpty
		animations = animations.filterNot(a => a(animData.hypotheticalWorld, time))

		if (advance) {
			while (animations.isEmpty && animData.currentGameWorldView.currentTime < game.currentTime) {
				game.updateViewToTime(animData.currentGameWorldView, animData.currentGameWorldView.currentTime + 1)

				val event = animData.currentGameWorldView.events.last
				if (event.state == Started) {
					event pmatch {
						case EntityMoved(entity, from, to) =>
							val HexSize = display[AxDrawingConstants].HexSize
							val endPos = to.asCartesian(HexSize.toFloat)
							val startPos = from.asCartesian(HexSize.toFloat)
							animations ::= FieldAnimation(entity, Physical.offset, Interpolation.between(Vec2f.Zero, endPos.xy - startPos.xy), time, time + 0.5.seconds)
					}
				}

				compData.animationsLastComputedAt = curTime()
			}
		}
	}

	override protected def onInitialize(game: World, display: World): Unit = {
		val animData = display[AxAnimatingWorldData]
		animData.currentGameWorldView = game.viewAtTime(game.currentTime)
		animData.hypotheticalWorld = new HypotheticalWorld(game, animData.currentGameWorldView)
	}

	override def draw(game: World, graphics: World): Unit = {}
}

object AnimationGraphicsComponent {
	class Data extends AxGraphicsData with TWorldAuxData {
		var animationsLastComputedFor = GameEventClock(0)
		var animationsLastComputedAt = curTime()
		var animations = List()
	}
}

trait Animation {
	def apply(world : World, time : UnitOfTime) : Boolean
}

case class FieldAnimation[C,T](entity : Entity, field : Field[C,T], interpolation : Interpolation[T], startTime : UnitOfTime, endTime : UnitOfTime)(implicit val tag : ClassTag[C]) extends Animation {
	import arx.core.introspection.FieldOperations._

	override def apply(world: World, time: UnitOfTime): Boolean = {
		val pcnt = ((time - startTime).inSeconds / (endTime - startTime).inSeconds).clamp(0.0f,1.0f)
		world.modify(entity, field setTo interpolation.interpolate(pcnt), None)
		time >= endTime - 0.0166666667.seconds
	}
}