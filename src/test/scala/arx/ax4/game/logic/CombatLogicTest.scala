package arx.ax4.game.logic

import arx.ax4.game.entities.AttackConditionals.AttackConditional
import arx.ax4.game.entities.AttackKey.Primary
import arx.ax4.game.entities.Companions.CombatData
import arx.ax4.game.entities.SpecialAttack.PowerAttack
import arx.ax4.game.entities._
import arx.ax4.game.logic.CombatLogicTest.{AntiMudDefense, Sandbox, SingleAttackBuff}
import arx.core.introspection.FieldOperations._
import arx.core.introspection.ReflectionAssistant
import arx.core.representation.ConfigValue
import arx.engine.data.Reduceable
import arx.engine.entity.Taxonomy
import arx.engine.world.{World, WorldQuery, WorldView}
import arx.game.data.RandomizationStyle.Median
import arx.game.data.{DefaultGameAuxData, RandomizationWorldData}
import arx.graphics.helpers.{RichText, RichTextRenderSettings}
import arx.resource.ResourceManager
import org.scalatest.FunSuite

import scala.reflect.ClassTag

class CombatLogicTest extends FunSuite {


	test("test trivial attack scenario") {
		val sandbox = new Sandbox
		import sandbox._
		implicit val view = world.view

		CombatLogic.attack(world, attacker, List(defenderA, defenderB), primaryAttack)

		WorldQuery.assert(s"health < 10 AND health > 0 FROM CharacterInfo WHERE id == $defenderA OR id == $defenderB", "baseline, both defenders should have been hit, but not killed")
	}

	test("test trivial attack scenario, with conditional defense bonus") {
		val sandbox = new Sandbox
		import sandbox._
		implicit val view = world.view

		world.modify(defenderA, CombatData.conditionalDefenseModifiers append (AntiMudDefense -> DefenseModifier(10,1)), None)

		CombatLogic.attack(world, attacker, List(defenderA, defenderB), primaryAttack)

		WorldQuery.assert(s"health == 10 FROM CharacterInfo WHERE id == $defenderA", "defender with anti-mud monster defenses should be fine")
		WorldQuery.assert(s"health < 10 FROM CharacterInfo where id == $defenderB", "but defender without such defenses should still get hit")

		// give the attacker a bonus when attacking a single target, with enough of a bonus to compensate for the conditional defense bonus it has
		world.modify(attacker, CombatData.conditionalAttackModifiers append (SingleAttackBuff -> AttackModifier(accuracyBonus = 20)), None)

		CombatLogic.attack(world, attacker, List(defenderA), primaryAttack)

		// two strikes, each dealing 1d6 (median -> 4) damage, minus 1 each from the conditional bonus to armor = 6 damage, so 4 health remaining
		WorldQuery.assert(s"health == 4 from CharacterInfo where id == $defenderA")
	}

	test("test trivial attack scenario, with special attack") {
		val sandbox = new Sandbox
		import sandbox._
		implicit val view = world.view

		// use the power attack special attack which reduces accuracy but doubles damage. With default arrangement the accuracy penalty
		// is too high for the attack to hit, so it should be a miss as a result
		val attackData = primaryAttack.copy()
		attackData.merge(PowerAttack.attackModifier)
		CombatLogic.attack(world, attacker, List(defenderA, defenderB), attackData)

		WorldQuery.assert(s"health == 10 FROM CharacterInfo WHERE id == $defenderA OR id == $defenderB", "defenders should not be hurt because attack too inaccurate")

		// give the attacker a bonus when attacking a single target, with enough of a bonus to compensate for the penalty from power attack
		world.modify(attacker, CombatData.conditionalAttackModifiers append (SingleAttackBuff -> AttackModifier(accuracyBonus = 20)), None)

		CombatLogic.attack(world, attacker, List(defenderA), attackData)

		// the target attacked with power attack should now have 0 health, but the other one should be untouched
		WorldQuery.assert(s"health == 0 FROM CharacterInfo WHERE id == $defenderA")
		WorldQuery.assert(s"health == 10 FROM CharacterInfo WHERE id == $defenderB")

	}

}


object CombatLogicTest {

	case object AntiMudDefense extends AttackConditional {
		override def source: String = "anti mud defense"

		override def isTrueForProspect(implicit view : WorldView, prospect: AttackProspect): Boolean = {
			prospect.attacker.dataOpt[CharacterInfo].exists(ci => ci.species == Taxonomy("MudMonster"))
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Anti mud defense")
	}

	case object SingleAttackBuff extends AttackConditional {
		override def source: String = "single attack buff"
		override def isTrueForProspect(implicit view: WorldView, prospect: AttackProspect): Boolean = prospect.allTargets.size == 1

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText("single attack buff")
	}

	case object TestPowerAttack extends SpecialAttack {
		condition = AttackConditionals.AnyAttack
		attackModifier = AttackModifier(
			namePrefix = Some("power attack : "),
			accuracyBonus = -10,
			damageBonuses = Vector(DamageModifier(DamagePredicate.All, DamageDelta.DamageMultiplier(2.0f)))
		)
	}

	def loadDataFromConfig[T <: AxAuxData](cv : ConfigValue)(implicit clazz : ClassTag[T]) = {
		val x : T = ReflectionAssistant.instantiate[T]
		x.loadFromConfig(cv)
		x
	}

	class Sandbox {
		val world = new World()
		world.registerSubtypesOf[AxAuxData]()
		world.registerSubtypesOf[DefaultGameAuxData]()
		world.attachWorldData(new RandomizationWorldData().initBy(r => r.randomizationStyle = Median))

		val attacker = world.createEntity()
		attacker.attachI(new CombatData)(cd => {
			cd.specialAttacks += "power attack" -> TestPowerAttack
		})(world)
		attacker.attachI(new CharacterInfo)(ci => {
			ci.health = Reduceable(10)
			ci.species = Taxonomy("MudMonster")
		})(world)


		val weaponConf = ResourceManager.sml("TestWeapons.sml")

		val weapon = world.createEntity()
		weapon attach loadDataFromConfig[CombatData](weaponConf.TestSword) in world
		weapon attach loadDataFromConfig[Item](weaponConf.TestSword) in world
		weapon attach loadDataFromConfig[Equipment](weaponConf.TestSword) in world
		weapon attach loadDataFromConfig[Weapon](weaponConf.TestSword) in world


		val defenderA = world.createEntity()
		defenderA attach new CombatData in world
		defenderA.attachI(new CharacterInfo)(ci => ci.health = Reduceable(10))(world)
		val defenderB = world.createEntity()
		defenderB attach new CombatData in world
		defenderB.attachI(new CharacterInfo)(ci => ci.health = Reduceable(10))(world)

		val primaryAttack = world.view.data[Weapon](weapon).attacks(Primary)
	}
}