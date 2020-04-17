package arx.ax4.graphics.components

import arx.Prelude
import arx.Prelude._
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.{ColorComponentMix, HueShift, Physical}
import arx.ax4.game.event.CardEvents.{CardDrawn, HandDiscarded, HandDrawn}
import arx.ax4.game.event.{DamageEvent, EntityMoved, GainSkillLevelEvent, StrikeEvent}
import arx.ax4.game.logic.AllegianceLogic
import arx.ax4.graphics.data.{AxAnimatingWorldData, AxDrawingConstants, AxGraphicsData}
import arx.ax4.graphics.logic.EntityDrawLogic
import arx.core.introspection.Field
import arx.core.math.Interpolation
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec2f, Vec3f, Vec4f}
import arx.core.vec.coordinates.{AxialVec3, CartVec, CartVec3}
import arx.engine.data.{Reduceable, TAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.components.{DrawPriority, GraphicsComponent}
import arx.engine.graphics.data.TGraphicsData
import arx.engine.simple.{DrawLayer, HexCanvas}
import arx.engine.world.EventState.Started
import arx.engine.world.{GameEventClock, HypotheticalWorld, HypotheticalWorldView, World, WorldQueryParser, WorldView}
import arx.graphics.Image
import arx.graphics.data.SpriteLibrary
import arx.graphics.helpers.{Color, RGBA}
import arx.resource.ResourceManager
import sun.java2d.cmm.ColorTransform

import scala.reflect.ClassTag

class AnimationGraphicsComponent extends  GraphicsComponent {
	override def drawPriority = DrawPriority.First

	override protected def onUpdate(game: World, display: World, dt: UnitOfTime, time: UnitOfTime): Unit = {
		val animData = display[AxAnimatingWorldData]
		val compData = display[AnimationGraphicsComponent.Data]


		animData.hypotheticalWorld = new HypotheticalWorld(game, animData.currentGameWorldView)
		// something something animations
		val advance = !animData.animations.exists(a => a.blocking)
		animData.animations = animData.animations.filterNot(a => a(animData.hypotheticalWorld, time))

		implicit val view = game.view

		if (advance) {
			while (!animData.animations.exists(_.blocking) && animData.currentGameWorldView.currentTime < game.currentTime) {
				game.updateViewToTime(animData.currentGameWorldView, animData.currentGameWorldView.currentTime + 1)

				val event = animData.currentGameWorldView.events.last
				if (event.state == Started) {
					event pmatch {
						case EntityMoved(entity, from, to) =>
							val endPos = to.asCartesian
							val startPos = from.asCartesian
							animData.animations ::= FieldAnimation(entity, Physical.offset, Interpolation.between(CartVec.Zero, endPos.xy - startPos.xy), time, time + 0.5.seconds)
						case StrikeEvent(attackInfo) =>
							implicit val view = animData.currentGameWorldView
							val allPositions = attackInfo.allTargets.flatMap(e => e.dataOpt[Physical].map(_.position.asCartesian))
							if (allPositions.isEmpty) { Noto.error(s"Strike animation had no valid target physical positions to decide animation direction: $attackInfo") }
							else {
								val endPos = allPositions.reduce(_ + _) / allPositions.size.toFloat
								val startPos = attackInfo.attacker(Physical).position.asCartesian
								val strikeVector = CartVec((endPos.xy - startPos.xy).normalizeSafe * 0.5f)
								animData.animations ::= FieldAnimation(attackInfo.attacker, Physical.offset, Interpolation.between(CartVec.Zero,  strikeVector).sin010, time, time + 0.5.seconds)
							}
							val curStamina = attackInfo.attacker(CharacterInfo).stamina
							animData.animations ::= FieldAnimation(attackInfo.attacker, CharacterInfo.stamina, Interpolation.betweenI(curStamina, curStamina.reduceBy(attackInfo.attackData.staminaCost, true)), time, time + 0.5.seconds)
						case DamageEvent(entity, damage, damageType) =>
							implicit val view = animData.currentGameWorldView
							val startTransforms = entity(Physical).colorTransforms
//							val colorInterp = Interpolation.fromFunction(pcnt => startTransforms ::: List(HueShift(0.0f, pcnt))).curve(Interpolation.sin010)
							val colorInterp = Interpolation.fromFunction(pcnt => startTransforms ::: List(ColorComponentMix(RGBA(1.0f,0.1f,0.1f,1.0f), pcnt))).curve(Interpolation.sin010)

							val startHealth = entity(CharacterInfo).health
							val duration = (damage/5.0f).seconds
							animData.animations ::= FieldAnimation(entity, CharacterInfo.health, Interpolation.betweenI(startHealth, startHealth.reduceBy(damage, limitToZero = true)), time, time + duration)
							animData.animations ::= FieldAnimation(entity, Physical.colorTransforms, colorInterp, time, time + duration)
						case GainSkillLevelEvent(entity, skill, newLevel) if AllegianceLogic.isPlayerCharacter(entity) =>
							implicit val view = animData.currentGameWorldView
							val skillIcon = SpriteLibrary.iconFor(skill).image
							val upIcon = ResourceManager.image("third-party/shikashiModified/up_arrow.png")

							for ((offset, img) <- List((CartVec3(-0.2f,0.0f,0.0f), upIcon), (CartVec3(0.2f,0.0f,0.0f), skillIcon))) {
								val pos = EntityDrawLogic.characterPosition(entity).getOrElse(CartVec3.Zero) + offset
								animData.animations ::= ImageAnimation(
									img,
									Interpolation.between(pos + CartVec3(0.0f, 0.8f, 0.0f), pos + CartVec3(0.0f, 1.3f, 0.0f)),
									Right(Vec2f(64.0f, 64.0f)),
									Interpolation.between(RGBA(1.0f, 1.0f, 1.0f, 1.0f), RGBA(1.0f, 1.0f, 1.0f, 0.0f)).sin01,
									DrawLayer.OverEntity,
									time,
									time + 5.seconds
								).nonBlocking()
							}
						case HandDrawn(entity) =>
							animData.animations ::= WaitAnimation(time + 0.1.second)
						case HandDiscarded(entity, cards) =>
							animData.animations ::= WaitAnimation(time + 0.1.second)
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

	override def draw(game: World, display: World): Unit = {
//		val animData = display[AxAnimatingWorldData]
//		val view = animData.animatedGameWorldView
//		animations.foreach(anim => anim.draw(view, ))
	}
}

class AnimationGraphicsRenderingComponent extends AxCanvasGraphicsComponent {
	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = display[AxAnimatingWorldData].animations.nonEmpty

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: HexCanvas, dt: UnitOfTime): Unit = {
		val animData = display[AxAnimatingWorldData]

		for (anim <- animData.animations) {
			anim.draw(animData.animatedGameWorldView, canvas, Prelude.curTime())
		}
	}

	override protected def onInitialize(game: World, display: World): Unit = {}
}

object AnimationGraphicsComponent {
	class Data extends AxGraphicsData with TWorldAuxData {
		var animationsLastComputedFor = GameEventClock(0)
		var animationsLastComputedAt = curTime()
	}
}

trait Animation {
	var blocking : Boolean = true
	def apply(world : World, time : UnitOfTime) : Boolean
	def draw(implicit world : WorldView, canvas : HexCanvas, time : UnitOfTime) : Unit = {}

	def nonBlocking() : this.type = {
		blocking = false
		this
	}
}

case class FieldAnimation[C <: TAuxData,T](entity : Entity, field : Field[C,T], interpolation : Interpolation[T], startTime : UnitOfTime, endTime : UnitOfTime)(implicit val tag : ClassTag[C]) extends Animation {
	import arx.core.introspection.FieldOperations._

	override def apply(world: World, time: UnitOfTime): Boolean = {
		val pcnt = ((time - startTime).inSeconds / (endTime - startTime).inSeconds).clamp(0.0f,1.0f)
		world.modify(entity, field setTo interpolation.interpolate(pcnt), None)
		time >= endTime - 0.0166666667.seconds
	}
}

case class WaitAnimation(endTime : UnitOfTime) extends Animation {
	override def apply(world: World, time: UnitOfTime): Boolean = {
		time >= endTime
	}
}


case class ImageAnimation(image : Interpolation[Image],
								  position : Interpolation[CartVec3],
								  dimensions : Either[CartVec, ReadVec2f],
								  color : Interpolation[Color],
								  layer : DrawLayer,
								  startTime : UnitOfTime,
								  endTime : UnitOfTime
								 ) extends Animation {
	override def apply(world: World, time: UnitOfTime): Boolean = {
		time >= endTime - 0.0166666667.seconds
	}

	override def draw(implicit world: WorldView, canvas: HexCanvas, time: UnitOfTime): Unit = {
		val pcnt = ((time - startTime).inSeconds / (endTime - startTime).inSeconds).clamp(0.0f,1.0f)

		var builder = canvas.quad(position.interpolate(pcnt).xy)
   		.texture(image.interpolate(pcnt))
   		.color(color.interpolate(pcnt))
   		.layer(layer)
		builder = dimensions match {
			case Left(cv) => builder.dimensions(cv)
			case Right(vv) => builder.dimensions(vv)
		}

		builder.draw()
	}
}