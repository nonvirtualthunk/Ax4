package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{AttackAction, BiasedHexSelector, EntitySelector, GameAction, GameActionIntentInstance, MoveAction, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{Physical, Tile}
import arx.ax4.game.entities.{AttackData, AttackReference, CardEffect, EntityTarget, HexTargetPattern, Tiles}
import arx.ax4.game.logic.{AllegianceLogic, AxPathfinder, CharacterLogic, CombatLogic, MovementLogic}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}

case class AttackCardEffect(attackRef: AttackReference) extends CardEffect {

	def targetSelector(attacker : Entity, attackData : AttackData) : Either[Selector[Entity], BiasedHexSelector]= attackData.targetPattern match {
		case hexTargetPattern: HexTargetPattern => Right(BiasedHexSelector(hexTargetPattern, (_,_) => true))
		case entityTarget: EntityTarget => Left(EntitySelector((view, ent) => AllegianceLogic.areEnemies(attacker, ent)(view), "Enemy creature")
									.withAmount(entityTarget.count))
	}

	override def nextSelector(world: WorldView, attacker: Entity, results: SelectionResult): Option[Selector[_]] = {
		implicit val view = world
		attackRef.resolve() match {
			case Some(attack) =>
				val tSel = targetSelector(attacker, attack)
				if (results.fullySatisfied(tSel)) {
					None
				} else {
					tSel match {
						case Left(value) => Some(value)
						case Right(value) => Some(value)
					}
				}
			case None =>
				Noto.error(s"Could not resolve attack: $attackRef")
				None
		}
	}


	override def applyEffect(world: World, attacker: Entity, selectionResult: SelectionResult): Unit = {
		implicit val view = world.view
		attackRef.resolve()(world.view) match {
			case Some(attack) =>
				val targets = selectionResult(targetSelector(attacker, attack))
				val entityTargets = targets match {
					case Left(entityTargets) => entityTargets
					case Right(hexTargets) => hexTargets.flatMap(hex => Tiles.tileAt(hex.vec)(Tile).entities)
				}
				CombatLogic.attack(world, attacker, entityTargets, attackRef)

			case None =>
				Noto.error(s"Could not resolve attack when applying: $attackRef")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		attackRef.resolve()(world).isDefined
	}

}