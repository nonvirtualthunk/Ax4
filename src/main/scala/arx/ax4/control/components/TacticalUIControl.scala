package arx.ax4.control.components

import arx.ai.search.Pathfinder
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile, TurnData}
import arx.ax4.game.entities.{AllegianceData, AttackData, AttackReference, CharacterInfo, DamageElement, FactionData, Physical, SkillsLibrary, Tile, Tiles, TurnData}
import arx.ax4.game.event.{ActiveIntentChanged, EntityMoved}
import arx.ax4.graphics.components.EntityGraphics
import arx.ax4.graphics.data.{AxDrawingConstants, SpriteLibrary, TacticalUIData, TacticalUIMode}
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec3f, Vec4f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, HexDirection}
import arx.engine.control.components.ControlComponent
import arx.engine.control.event.{KeyModifiers, KeyPressEvent, KeyReleaseEvent, MouseButton, MouseEvent, MouseMoveEvent, MousePressEvent, MouseReleaseEvent, UIEvent}
import arx.engine.graphics.data.PovData
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World, WorldView}
import arx.graphics.{GL, TToImage}
import arx.Prelude.toArxList
import arx.ax4.control.components.widgets.InventoryWidget
import arx.ax4.game.action.{AttackIntent, BiasedAxialVec3, DoNothingIntent, GameAction, GatherIntent, MoveIntent}
import arx.ax4.game.event.TurnEvents.{TurnEndedEvent, TurnStartedEvent}
import arx.ax4.game.logic.{ActionLogic, CharacterLogic, CombatLogic, SkillsLogic}
import arx.ax4.graphics.data.TacticalUIMode.ChooseSpecialAttackMode
import arx.core.datastructures.{Watcher, Watcher2}
import arx.core.math.Sext
import arx.engine.control.components.windowing.widgets.DimensionExpression.ExpandTo
import arx.engine.control.components.windowing.{SimpleWidget, WindowingControlComponent}
import arx.engine.control.components.windowing.widgets.{DimensionExpression, ListItemSelected, SpriteDefinition}
import arx.engine.control.data.WindowingControlData
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, IdentityData}
import arx.engine.event.Event
import arx.graphics.helpers.{Color, ImageSection, RGBA, RichText, RichTextRenderSettings, RichTextSection, THasRichTextRepresentation, TextSection}
import arx.resource.ResourceManager
import org.lwjgl.glfw.GLFW

class TacticalUIControl(windowing : WindowingControlComponent) extends AxControlComponent {
	import arx.core.introspection.FieldOperations._

	var lastSelected : (Option[Entity], GameEventClock) = (None, GameEventClock(0))


	override def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val view = gameView
		val tuid = display[TacticalUIData]
		val selectedState = (tuid.selectedCharacter, view.currentTime)
		if (selectedState != lastSelected) {
			updateBindings(view, display, tuid.selectedCharacter)
			lastSelected = selectedState
		}
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView

		val tuid = display[TacticalUIData]

		val desktop = display[WindowingControlData].desktop
		desktop.drawing.drawAsForegroundBorder = true
		desktop.drawing.backgroundImage = Some(ResourceManager.image("ui/woodBorder2.png"))
		val selCInfo = desktop.createChild("widgets/CharacterInfoWidgets.sml", "SelectedCharacterInfo")
		selCInfo.showing = Moddable(() => tuid.selectedCharacter.isDefined)
		selCInfo.consumeEventHighPrecedence {
			case me : MouseEvent => true
		}
		tuid.selectedCharacterInfoWidget = selCInfo

		val mainSection = desktop.createChild(SimpleWidget).widget
		mainSection.width = ExpandTo(tuid.selectedCharacterInfoWidget)
		mainSection.height = DimensionExpression.Proportional(1.0f)
		mainSection.drawing.drawBackground = false
		tuid.mainSectionWidget = mainSection


		val actionSelBar = mainSection.createChild("widgets/ActionSelectionWidgets.sml", "ActionSelectionButtonBar")
		actionSelBar.showing = Moddable(() => tuid.selectedCharacter.isDefined)
		for (w <- actionSelBar.descendantsWithIdentifier("SpecialAttackButton")) {
			w.consumeEvent {
				case MouseReleaseEvent(_,_,_) => tuid.toggleUIMode(ChooseSpecialAttackMode)
			}
		}

		tuid.specialAttacksList = mainSection.createChild("widgets/ActionSelectionWidgets.sml", "SpecialAttackSelectionList")
//		specialAttackList.showing = Moddable(() => )
		(desktop.descendantsWithIdentifier("SpecialAttackSelectionList") ::: desktop.descendantsWithIdentifier("AttackDisplay")).foreach(w =>
			w.onEvent {
				case ListItemSelected(_, _, Some(attackDisplayInfo: SimpleAttackDisplayInfo)) =>
					val selC = display[TacticalUIData].selectedCharacter.get
					game.modify(selC, CharacterInfo.activeAttack -> Some(attackDisplayInfo.attackRef))
					game.modify(selC, CharacterInfo.defaultIntent -> AttackIntent(attackDisplayInfo.attackRef))
					CharacterLogic.setActiveIntent(selC, AttackIntent(attackDisplayInfo.attackRef))(game)
			}
		)
		tuid.specialAttacksList.showing = Moddable(() => tuid.activeUIMode == TacticalUIMode.ChooseSpecialAttackMode)

		tuid.inventoryWidget = new InventoryWidget(mainSection)
		tuid.inventoryWidget.widget.showing = Moddable(() => tuid.activeUIMode == TacticalUIMode.InventoryMode)



		val activeFaction = gameView.worldData[TurnData].activeFaction
		for (selectChar <- gameView.entitiesWithData[CharacterInfo]
			.filter(e => e[AllegianceData].faction == activeFaction)
			.find(e => e[CharacterInfo].actionPoints.currentValue > 0 || e[CharacterInfo].movePoints > Sext(0))) {
			selectCharacter(game, display, selectChar)
		}


		onControlEvent {
			case MousePressEvent(button, pos, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val pressedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				fireEvent(HexMousePressEvent(button,pressedHex,pos,modifiers))
			case MouseMoveEvent(pos, delta, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val mousedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				val tuid = display[TacticalUIData]
				val biasDir = (0 until 6).minBy(q => (mousedHex.neighbor(q).asCartesian(const.HexSize.toFloat) - unprojected.xy).lengthSafe)
				tuid.mousedOverBiasedHex = BiasedAxialVec3(AxialVec3(mousedHex,0), HexDirection.fromInt(biasDir))


				fireEvent(HexMouseMoveEvent(mousedHex,pos,modifiers))
			case KeyReleaseEvent(GLFW.GLFW_KEY_ENTER, _) =>
				val TD = game.view.worldData[TurnData]
				val factions = game.view.entitiesWithData[FactionData].toList.sortBy(e => e.id)
				val activeFaction = TD.activeFaction
				val activeIndex = factions.indexOf(activeFaction)
				val nextFaction = factions((activeIndex + 1) % factions.size)
				game.addEvent(TurnEndedEvent(activeFaction, TD.turn))

				if (activeIndex == factions.size - 1) {
					game.modifyWorld(TurnData.turn + 1)
				}

				game.modifyWorld(TurnData.activeFaction -> nextFaction)
				game.addEvent(TurnStartedEvent(nextFaction, TD.turn))
			case KeyReleaseEvent(GLFW.GLFW_KEY_I, _) =>
				val tuid = display[TacticalUIData]
				tuid.toggleUIMode(TacticalUIMode.InventoryMode)
			case KeyReleaseEvent(GLFW.GLFW_KEY_G, _) =>
				val tuid = display[TacticalUIData]
				for (selC <- tuid.selectedCharacter) {
					CharacterLogic.setActiveIntent(selC, GatherIntent)(game)
				}
		}
	}

	def selectCharacter(game : World, display : World, selC: Entity) = {
		implicit val view = game.view
		val tuid = display[TacticalUIData]
		tuid.selectedCharacter = Some(selC)
		if (selC.data[CharacterInfo].activeIntent == DoNothingIntent) {
			val availableAttacks = CombatLogic.availableAttacks(game.view, selC, includeBaseAttacks = true, includeSpecialAttacks = false)
			val newIntent = availableAttacks.find(a => a.attackKey == "primary")
   			.orElse(availableAttacks.headOption) match {
				case Some(attackRef) => AttackIntent(attackRef)
				case None => MoveIntent
			}
			println(s"New Intent: $newIntent")

			game.startEvent(ActiveIntentChanged(selC, newIntent))
			game.modify(selC, CharacterInfo.activeIntent -> newIntent)
			game.modify(selC, CharacterInfo.defaultIntent -> newIntent)
			game.endEvent(ActiveIntentChanged(selC, newIntent))
		}
	}

	def updateBindings(implicit game : WorldView, display : World, character : Option[Entity]): Unit = {
		val tuid = display[TacticalUIData]
		for (selC <- tuid.selectedCharacter) {
			val desktop = display[WindowingControlData].desktop
			desktop.bind("selectedCharacter.name", selC[IdentityData].name.getOrElse("nameless"))
			desktop.bind("selectedCharacter.portrait", () => EntityGraphics.characterImageset.drawInfoFor(game, selC, display.view).head.image)

			desktop.bind("selectedCharacter.health.cur", () => selC[CharacterInfo].health.currentValue)
			desktop.bind("selectedCharacter.health.max", () => selC[CharacterInfo].health.maxValue)

			desktop.bind("selectedCharacter.actions.cur", () => selC[CharacterInfo].actionPoints.currentValue)
			desktop.bind("selectedCharacter.actions.max", () => selC[CharacterInfo].actionPoints.maxValue)

			desktop.bind("selectedCharacter.move.cur", () => selC[CharacterInfo].curPossibleMovePoints)
			desktop.bind("selectedCharacter.move.max", () => selC[CharacterInfo].maxPossibleMovePoints)

			desktop.bind("selectedCharacter.speed", () => selC[CharacterInfo].moveSpeed)

			tuid.selectedCharacterInfoWidget.bind("attacks", () => {
				CombatLogic.availableAttacks(game, selC, includeBaseAttacks = true,  includeSpecialAttacks = false)
   				.sortBy(aref => aref.attackKey != "primary")
					.map(aref => {
						val selected = selC[CharacterInfo].activeIntent == AttackIntent(aref)
					CombatLogic.resolveUntargetedConditionalAttackData(game, selC, aref) match {
						case Some((attackData, modifiers)) => SimpleAttackDisplayInfo.fromAttackData(aref, attackData, selected)
						case None => SimpleAttackDisplayInfo(aref, "unresolved", AttackBonus(0), DamageExpression(Map()), RGBA(0.1f,0.2f,0.3f,1f))
					}
				}).toList
			})

			tuid.specialAttacksList.bind("specialAttacks", () => {
				CombatLogic.availableAttacks(game, selC, includeBaseAttacks = false,  includeSpecialAttacks = true)
   				.sortBy(aref => s"${aref.attackKey} ${aref.specialKey}")
   				.map(aref => {
						val selected = selC[CharacterInfo].activeIntent == AttackIntent(aref)
						CombatLogic.resolveUntargetedConditionalAttackData(game, selC, aref) match {
							case Some((attackData, modifiers)) => SimpleAttackDisplayInfo.fromAttackData(aref, attackData, selected)
							case None => SimpleAttackDisplayInfo(aref, "unresolved", AttackBonus(0), DamageExpression(Map()), RGBA(0.1f,0.2f,0.3f,1f))
						}
					})
			})

			tuid.selectedCharacterInfoWidget.bind("skills", () => {
				SkillsLogic.skillLevels(selC).toList.map {
					case (taxon, level) =>
						SkillsLibrary(taxon) match {
							case Some(skill) => SimpleSkillDisplayInfo(skill.displayName.capitalize, level, SpriteLibrary.iconFor(taxon), SkillsLogic.currentLevelXp(selC, taxon), SkillsLogic.currentLevelXpRequired(selC, taxon))
							case None => Noto.error(s"Skill display couldn't find info for skill $taxon")
						}
				}
			})

			tuid.inventoryWidget.updateBindings(game, display, selC)
		}
	}
}

case class DamageExpression(damageElements : Map[AnyRef, DamageElement]) extends THasRichTextRepresentation  {
	import arx.Prelude.int2RicherInt
	override def toString: String = {
		damageElements.values.map(de => {
			var sections : List[String] = Nil
			sections ::= de.damageDice.toString
			if (de.damageMultiplier != 1.0f) {
				sections ::= s"x${de.damageMultiplier}"
			}
			if (de.damageBonus != 0) {
				sections ::= de.damageBonus.toSignedString
			}
			sections ::= de.damageType.name
			sections.reverse.reduce(_ + " " + _)
		}).foldLeft("")(_ + " " + _).trim
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		val sections = damageElements.values.flatMap(de => {
			var sections = List[RichTextSection]()
			sections ::= TextSection(de.damageDice.toString)
			if (de.damageMultiplier != 1.0f) {
				sections ::= TextSection(s"x${de.damageMultiplier}")
			}
			if (de.damageBonus != 0) {
				sections ::= TextSection(de.damageBonus.toSignedString)
			}
			sections ::= (SpriteLibrary.getSpriteDefinitionFor(de.damageType) match {
				case Some(SpriteDefinition(icon, icon16)) if !settings.noSymbols => ImageSection(icon,2, Color.White)
				case _ => TextSection(" " + de.damageType.name)
			})

			sections.reverse
		})

		RichText(sections.toList)
	}
}
case class AttackBonus(bonus : Int) {
	import arx.Prelude.int2RicherInt
	override def toString: String = bonus.toSignedString
}
case class SimpleAttackDisplayInfo(attackRef : AttackReference, name : String, accuracyBonus : AttackBonus, damage : DamageExpression, selectedColor : Color)
object SimpleAttackDisplayInfo {
	def fromAttackData(aref : AttackReference, attackData : AttackData, selected : Boolean) : SimpleAttackDisplayInfo = {
		SimpleAttackDisplayInfo(aref, attackData.name.capitalize, AttackBonus(attackData.accuracyBonus), DamageExpression(attackData.damage),
			if (selected) { RGBA(0.1f,0.2f,0.3f,1f) } else { RGBA(0.5f,0.5f,0.5f,1.0f) })
	}
}

case class SimpleSkillDisplayInfo(name : String, level : Int, icon : TToImage, currentXp : Int, requiredXp : Int)

case class HexMousePressEvent(button : MouseButton, hex : AxialVec, pos : ReadVec2f, modifiers : KeyModifiers) extends UIEvent
case class HexMouseMoveEvent(hex : AxialVec, pos : ReadVec2f, modifiers: KeyModifiers) extends UIEvent