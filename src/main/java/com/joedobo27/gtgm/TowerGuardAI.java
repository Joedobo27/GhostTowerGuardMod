package com.joedobo27.gtgm;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.ai.CreatureAI;
import com.wurmonline.server.creatures.ai.CreatureAIData;

public class TowerGuardAI extends CreatureAI {

    public static final String id_factory_name = "jdbTowerGhost";

    @Override
    protected boolean pollMovement(Creature creature, long l) {
        // Delete guard if it has no target.
        if (creature.target == -10L) {
            creature.destroy();
            return true;
        }
        return false;
    }

    @Override
    protected boolean pollAttack(Creature creature, long l) {
        return false;
    }

    @Override
    protected boolean pollBreeding(Creature creature, long l) {
        return false;
    }

    @Override
    public CreatureAIData createCreatureAIData() {
        return null;
    }

    @Override
    public void creatureCreated(Creature creature) {

    }
}
