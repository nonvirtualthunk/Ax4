package arx.ax4.game.components

import arx.application.Noto
import arx.ax4.game.entities.{AllegianceData, CardPlay, MonsterAttack, TargetPattern}
import arx.ax4.game.entities.Companions.{CardData, DeckData, MonsterData, Physical, TagData}
import arx.ax4.game.event.TurnEvents.{EntityTurnStartEvent, TurnEndedEvent, TurnStartedEvent}
import arx.ax4.game.logic.{AllegianceLogic, AxPathfinder, CharacterLogic, CombatLogic, IdentityLogic, MovementLogic, TurnLogic}
import arx.core.math.Sext
import arx.core.units.UnitOfTime
import arx.engine.entity.{Entity, Taxonomy}
import arx.engine.game.components.GameComponent
import arx.engine.world.{World, WorldQuery}
import arx.game.logic.Randomizer

class AIComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		onGameEventEndWithPrecedence(-100) {
			case TurnStartedEvent(faction, turnNumber) if !AllegianceLogic.isPlayerFaction(faction) =>
				takeAITurn(faction)(world)
				TurnLogic.endTurn(world)
		}
	}

	def takeAITurn(faction: Entity)(implicit world: World) = {
		implicit val view = world.view

		for (entity <- AllegianceLogic.entitiesInFaction(faction)) {
			println(s"Taking turn for entity ${IdentityLogic.name(entity)}")

			val randomizer = Randomizer(world)

			val MD = entity(MonsterData)
			val MonsterAttack(attack) = randomizer.takeRandom(MD.monsterAttacks, 1).head

			println(s"Chosen attack : $attack")


			attack.targetPattern match {
				case TargetPattern.Enemies(numEnemies) if numEnemies == 1 =>
					val allEnemies = AllegianceLogic.enemiesOf(entity)
					allEnemies.map(enemy => enemy -> AxPathfinder.findPathToMatching(entity, entity(Physical).position, pos => {
						CombatLogic.canAttackBeMade(entity, pos, Left(enemy), attack)
					})).find(t => t._2.isDefined) match {
						case Some((chosenEnemy, path)) =>
							MovementLogic.moveCharacterOnPath(entity, path.get, Some(5))
							if (CombatLogic.canAttackBeMade(entity, CharacterLogic.position(entity), Left(chosenEnemy), attack)) {
								CombatLogic.attack(world, entity, Seq(chosenEnemy), attack)
							}
						case None => Noto.info("No path available to any enemy")
					}
				case _ =>
					Noto.warn("Monster attacks only support single target right now")
			}

			//		println("Cards: ")
			//		println(WorldQuery.run(s"SELECT * FROM DeckData WHERE id == ${entity.id}"))
			//
			//		val possibleCards = entity(DeckData).hand
			//		val attackCards = possibleCards.filter(c => IdentityLogic.isA(c, Taxonomy("CardTypes.AttackCard")))
			//		if (attackCards.isEmpty) {
			//			Noto.info("No attacks")
			//		} else {
			//			val attackCard = Randomizer(world).takeRandom(attackCards, 1).head
			//			val cardPlay = CardPlay(attackCard)
			//			cardPlay.instantiate(view, entity, attackCard) match {
			//				case Left(inst) =>
			//					val allEnemies = AllegianceLogic.enemiesOf(entity)
			//					val paths = allEnemies
			//				case Right(msg) =>
			//					Noto.info(s"Monster could not play card: $msg")
			//			}
			//		}
		}
	}
}
