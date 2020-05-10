package arx.ax4.graphics.components

import arx.Prelude
import arx.Prelude._
import arx.application.Noto
import arx.ax4.control.components.widgets.CardWidget
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.cardeffects.AddCardToDeck
import arx.ax4.game.entities.{ColorComponentMix, HueShift, Physical}
import arx.ax4.game.event.CardEvents.{CardDrawn, CardsAdded, HandDiscarded, HandDrawn}
import arx.ax4.game.event.{DamageEvent, EntityMoved, GainSkillLevelEvent, StrikeEvent}
import arx.ax4.game.logic.AllegianceLogic
import arx.ax4.graphics.data.{AxAnimatingWorldData, AxDrawingConstants, AxGraphicsData, TacticalUIData}
import arx.ax4.graphics.logic.EntityDrawLogic
import arx.core.introspection.Field
import arx.core.math.Interpolation
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec2f, Vec3f, Vec4f}
import arx.core.vec.coordinates.{AxialVec3, CartVec, CartVec3}
import arx.engine.control.components.windowing.{Div, Widget}
import arx.engine.control.components.windowing.widgets.PositionExpression
import arx.engine.data.{Moddable, Reduceable, TAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.components.{DrawPriority, GraphicsComponent}
import arx.engine.graphics.data.{PovData, TGraphicsData, WindowingGraphicsData}
import arx.engine.simple.{DrawLayer, HexCanvas}
import arx.engine.world.EventState.Started
import arx.engine.world.{GameEventClock, HypotheticalWorld, HypotheticalWorldView, World, WorldQueryParser, WorldView}
import arx.graphics.{GL, Image}
import arx.graphics.data.SpriteLibrary
import arx.graphics.helpers.{Color, RGBA}
import arx.resource.ResourceManager
import sun.java2d.cmm.ColorTransform

import scala.collection.mutable
import scala.reflect.ClassTag

class AnimationGraphicsComponent extends  GraphicsComponent {
	var hypotheticalWorldInitialized = false

	override def drawPriority = DrawPriority.First

	override protected def onUpdate(game: World, display: World, dt: UnitOfTime, time: UnitOfTime): Unit = {
		val animData = display[AxAnimatingWorldData]
		val compData = display[AnimationGraphicsComponent.Data]
		val parentWidget = display[TacticalUIData].mainSectionWidget


		if (!hypotheticalWorldInitialized || animData.hypotheticalWorld.view.currentTime != animData.currentGameWorldView.currentTime) {
			animData.hypotheticalWorld = new HypotheticalWorld(game, animData.currentGameWorldView)
			hypotheticalWorldInitialized = true
		} else {
			animData.hypotheticalWorld.clear()
		}
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
									Right(Interpolation(ReadVec2f(64.0f, 64.0f))),
									Interpolation.between(RGBA(1.0f, 1.0f, 1.0f, 1.0f), RGBA(1.0f, 1.0f, 1.0f, 0.0f)).sin01,
									DrawLayer.OverEntity,
									time,
									time + 5.seconds
								).nonBlocking()
							}
						case CardsAdded(entity, cards) =>
              animData.animations ::= CardAnimation(entity, cards, parentWidget, display[WindowingGraphicsData], display[PovData], time, time + (2.0f + cards.size * 0.3f).seconds)
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


case class WidgetAnimation(widget : Widget, updateFunc : (Widget, Float) => Unit, startTime : UnitOfTime, endTime : UnitOfTime) extends Animation {
	override def apply(world: World, time: UnitOfTime): Boolean = {
		if (time >= endTime - 0.0166666667.seconds) {
			widget.destroy()
			true
		} else {
			updateFunc(widget, (time - startTime).inSeconds / (endTime - startTime).inSeconds)
			false
		}
	}
}

case class ImageAnimation(image : Interpolation[Image],
								  position : Interpolation[CartVec3],
								  dimensions : Either[Interpolation[CartVec], Interpolation[ReadVec2f]],
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
			case Left(cv) => builder.dimensions(cv.interpolate(pcnt))
			case Right(vv) => builder.dimensions(vv.interpolate(pcnt))
		}

		builder.draw()
	}
}

case class CardAnimation(entity: Entity, cards: Vector[Entity], parentWidget : Widget, windowingGraphicsData: WindowingGraphicsData, povData : PovData, startTime : UnitOfTime, endTime : UnitOfTime) extends Animation {
	val totalDuration = endTime - startTime
	val frontDisplayEndTime = startTime + totalDuration * 0.4f
	val backDisplayEndTime = frontDisplayEndTime + totalDuration * 0.2f

	val div = parentWidget.createChild(Div)
	div.widget.x = PositionExpression.Centered
	div.widget.y = PositionExpression.Centered
	div.widget.z = 100
	div.widget.showing = Moddable(false)

	var cardWidgets : Vector[Widget] = Vector()


	case class CardBack (position : Interpolation[ReadVec2f],
											 dimensions : Interpolation[ReadVec2f])
	var cardBacks : Vector[CardBack] = Vector()
	val cardBackImage = ResourceManager.image("graphics/icons/card_back_large.png")

	def createCardWidgets(view : WorldView): Unit = {
		var prevWidget : Option[Widget] = None
		cardWidgets = for (card <- cards) yield {
			val widget = CardWidget(div, entity, card)(view)
			for (prev <- prevWidget) {
				widget.x = PositionExpression.Relative(prev, 10)
			}
			prevWidget = Some(widget)
			widget
		}
	}

	override def apply(world: World, time: UnitOfTime): Boolean = {
		if (time >= endTime) {
			if (!div.widget.closed) { div.destroy() }
			return true
		}

		if (time >= startTime) {
			if (cardWidgets.isEmpty) {
				createCardWidgets(world.view)
			}

			if (time <= frontDisplayEndTime) {
				div.widget.showing = Moddable(true)
			} else {
				if (!div.widget.closed) {
					cardBacks = cardWidgets.map(w => {
						val startDimensions = Vec2f(w.drawing.effectiveDimensions)
						val endDimensions = Vec2f(cardBackImage.dimensions * 2)

						val rawPos = Vec2f(w.drawing.absolutePosition.xy + startDimensions * 0.5f)
						val pixelPos = windowingGraphicsData.pov.project(Vec3f(rawPos, 0.0f), GL.viewport)
//						val startPos = Vec2f(rawPos.x, GL.viewport.h - rawPos.y - 1) + Vec2f(startDimensions.x * 0.5f, startDimensions.y * -0.5f)
						val startPos = povData.pov.unproject(Vec3f(pixelPos,0.0f), GL.viewport).xy//pixelPos// + Vec2f(startDimensions.x * 0.5f, startDimensions.y * -0.5f)
						val endPos = Vec2f(-GL.viewport.w*0.5f,-GL.viewport.h*0.5f)

						CardBack(Interpolation.between(startPos, endPos), Interpolation.between(startDimensions, endDimensions))
					})

					div.widget.destroy()
				}
			}
		}
		false
	}

	override def draw(implicit world: WorldView, canvas: HexCanvas, time: UnitOfTime): Unit = {
		val pcnt = if (time < backDisplayEndTime) {
			0.0f
		} else {
			(time - backDisplayEndTime).inSeconds / (endTime - backDisplayEndTime).inSeconds
		}
		for (cardBack <- cardBacks) {
			val pos = cardBack.position.interpolate(pcnt)
			val dim = cardBack.dimensions.interpolate(pcnt)

			canvas.quad(pos)
  			.texture(cardBackImage)
  			.dimensions(dim)
  			.layer(DrawLayer.Overlay)
  			.draw()
		}
	}
}