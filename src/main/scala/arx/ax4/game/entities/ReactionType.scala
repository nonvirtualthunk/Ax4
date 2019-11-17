package arx.ax4.game.entities

import arx.ax4.game.entities.Companions.{CombatData, DefenseModifier, QualitiesData}
import arx.core.macros.GenerateCompanion
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{ModifierReference, NestedModifier, World}

abstract class ReactionType {
	def applyToEntity(world : World, entity : Entity) : Seq[ModifierReference]
}

object ReactionType {
	import arx.core.introspection.FieldOperations._
	case object Parry extends ReactionType {
		override def applyToEntity(world: World, entity: Entity): Seq[ModifierReference] = {
			world.modify(entity, QualitiesData.qualities.incrementKey("parry", 1)) :: Nil
		}
	}
	case object Defend extends ReactionType {
		override def applyToEntity(world: World, entity: Entity): Seq[ModifierReference] = {
			world.modify(entity, NestedModifier(CombatData.defenseModifier, Companions.DefenseModifier.dodgeBonus + 1), "Defend Reaction") :: Nil
		}
	}
	case object Dodge extends ReactionType {
		override def applyToEntity(world: World, entity: Entity): Seq[ModifierReference] = {
			world.modify(entity, QualitiesData.qualities.incrementKey("dodge", 1)) :: Nil
		}
	}
	case object NoReaction extends ReactionType {
		override def applyToEntity(world: World, entity: Entity): Seq[ModifierReference] = Nil
	}
}


@GenerateCompanion
class ReactionData extends AxAuxData {
	var currentReaction : ReactionType = ReactionType.NoReaction
	var modifiersByType : Map[ReactionType, Seq[ModifierReference]] = Map()
}

@GenerateCompanion
class QualitiesData extends AxAuxData {
	var qualities = Map[String, Int]()
}
