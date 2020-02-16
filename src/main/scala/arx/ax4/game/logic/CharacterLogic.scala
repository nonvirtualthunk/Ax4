package arx.ax4.game.logic


import arx.ax4.game.entities.{AllegianceData, AttackData, AttackKey, CharacterInfo, CombatData, DamageElement, DamageKey, DamageType, DeckData, Equipment, Inventory, Physical, QualitiesData, ReactionData, TagData, TargetPattern, Weapon}
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.{EntityCreated, MovePointsGained}
import arx.engine.entity.{Entity, IdentityData, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._
import arx.core.math.Sext
import arx.game.data.DicePool

object CharacterLogic {

	def curActionPoints(character : Entity)(implicit view : WorldView) = {
		character.dataOpt[CharacterInfo] match {
			case Some(ci) => ci.actionPoints.currentValue
			case None => 0
		}
	}

	def curMovePoints(character : Entity)(implicit view : WorldView) = {
		character.dataOpt[CharacterInfo].map(_.movePoints).getOrElse(Sext(0))
	}

	def curStamina(character : Entity)(implicit view : WorldView) : Int = {
		character.dataOpt[CharacterInfo].map(_.stamina.currentValue).getOrElse(0)
	}

	def useActionPoints(character : Entity, ap : Int)(implicit game : World) = {
		game.modify(character, CharacterInfo.actionPoints reduceBy ap)
	}

	def useStamina(character : Entity, stamina : Int)(implicit game : World) = {
		game.modify(character, CharacterInfo.stamina reduceBy stamina)
	}

	def gainMovePoints(character : Entity, mp : Sext)(implicit game : World) = {
		game.startEvent(MovePointsGained(character, mp))
		game.modify(character, CharacterInfo.movePoints + mp)
		game.endEvent(MovePointsGained(character, mp))
	}


	def createCharacter(faction : Entity)(implicit world : World) = {
		val creature = world.createEntity()
		world.attachData(creature, new DeckData)
		world.attachData(creature, new CharacterInfo)
		world.attachData(creature, new Physical)
		world.attachData(creature, new IdentityData)
		world.attachData(creature, new Equipment)
		world.attachData(creature, new Inventory)
		world.attachData(creature, new CombatData)
		world.attachData(creature, new QualitiesData)
		world.attachData(creature, new ReactionData)
		world.attachData(creature, new TagData)
		world.attachDataWith(creature, (wd : Weapon) => {
			wd.attacks += AttackKey.Primary -> AttackData(creature, "Punch", Taxonomy("BludgeoningAttack"), 0, 1, 1, 1, 0, 1, Vector(DamageElement(DamageKey.Primary, DicePool(1).d(4), 0, 1.0f, DamageType.Bludgeoning)), TargetPattern.SingleEnemy, cardCount = 2)
			wd.weaponSkills = List(Taxonomy("UnarmedSkill"))
			wd.naturalWeapon = true
		})
		world.attachDataWith(creature, (ad : AllegianceData) => {
			ad.faction = faction
		})

		world.addEvent(EntityCreated(creature))
		creature
	}

}
