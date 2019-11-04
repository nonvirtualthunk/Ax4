package arx.ax4.graphics.components

import arx.Prelude._
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.{ColorComponentMix, HueShift, Physical}
import arx.ax4.game.event.{DamageEvent, EntityMoved, StrikeEvent}
import arx.ax4.graphics.data.{AxAnimatingWorldData, AxDrawingConstants, AxGraphicsData}
import arx.core.introspection.Field
import arx.core.math.Interpolation
import arx.core.units.UnitOfTime
import arx.core.vec.{Vec2f, Vec3f, Vec4f}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.data.{Reduceable, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.components.{DrawPriority, GraphicsComponent}
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.EventState.Started
import arx.engine.world.{GameEventClock, HypotheticalWorld, HypotheticalWorldView, World, WorldQueryParser}
import arx.graphics.helpers.RGBA
import sun.java2d.cmm.ColorTransform

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
						case StrikeEvent(attackInfo) =>
							implicit val view = animData.currentGameWorldView
							val HexSize = display[AxDrawingConstants].HexSize
							val allPositions = attackInfo.allTargets.flatMap(e => e.dataOpt[Physical].map(_.position.asCartesian(HexSize.toFloat)))
							if (allPositions.isEmpty) { Noto.error(s"Strike animation had no valid target physical positions to decide animation direction: $attackInfo") }
							else {
								val endPos = allPositions.reduce(_ + _) / allPositions.size.toFloat
								val startPos = attackInfo.attacker(Physical).position.asCartesian(HexSize.toFloat)
								val strikeVector = (endPos.xy - startPos.xy).normalizeSafe * HexSize.toFloat * 0.5f
								animations ::= FieldAnimation(attackInfo.attacker, Physical.offset, Interpolation.between(Vec2f.Zero,  strikeVector).sin010, time, time + 0.5.seconds)
							}
							val curStamina = attackInfo.attacker(CharacterInfo).stamina
							animations ::= FieldAnimation(attackInfo.attacker, CharacterInfo.stamina, Interpolation.betweenI(curStamina, curStamina.reduceBy(attackInfo.attackData.staminaCostPerStrike, true)), time, time + 0.5.seconds)
						case DamageEvent(entity, damage, damageType) =>
							implicit val view = animData.currentGameWorldView
							val startTransforms = entity(Physical).colorTransforms
//							val colorInterp = Interpolation.fromFunction(pcnt => startTransforms ::: List(HueShift(0.0f, pcnt))).curve(Interpolation.sin010)
							val colorInterp = Interpolation.fromFunction(pcnt => startTransforms ::: List(ColorComponentMix(RGBA(1.0f,0.1f,0.1f,1.0f), pcnt))).curve(Interpolation.sin010)

							val startHealth = entity(CharacterInfo).health
							val duration = (damage/5.0f).seconds
							animations ::= FieldAnimation(entity, CharacterInfo.health, Interpolation.betweenI(startHealth, startHealth.reduceBy(damage, limitToZero = true)), time, time + duration)
							animations ::= FieldAnimation(entity, Physical.colorTransforms, colorInterp, time, time + duration)
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