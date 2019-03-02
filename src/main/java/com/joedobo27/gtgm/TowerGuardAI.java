package com.joedobo27.gtgm;

import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.creatures.ai.CreatureAI;
import com.wurmonline.server.creatures.ai.CreatureAIData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TowerGuardAI extends CreatureAI {

    static final String id_factory_name = "jdbTowerGhost";

    @Override
    protected boolean pollMovement(@NotNull Creature creature, long l) {
        // Delete guard if it has no target.
        if (creature.target == -10L) {
            creature.destroy();
            return true;
        }
        Creature target = Creatures.getInstance().getCreatureOrNull(creature.target);
        if (target == null) {
            creature.destroy();
            return true;
        }
        if (Math.abs(creature.getPosX() - target.getPosX()) > 2 || Math.abs(creature.getPosY() - target.getPosY()) > 2) {
            creature.setTeleportPoints(target.getPosX(), target.getPosY(), target.isOnSurface() ? 0 : -1, 0 );
            creature.teleport();
            return false;
        }
        return false;
    }

    @Override
    protected boolean pollAttack(@Nullable Creature creature, long l) {
        return false;
    }

    @Override
    protected boolean pollBreeding(@Nullable Creature creature, long l) {
        return false;
    }

    @Override
    public CreatureAIData createCreatureAIData() {
        return new TowerGuardAIData();
    }

    @Override
    public void creatureCreated(@NotNull Creature creature) {
        creature.getCreatureAIData().setCreature(creature);
    }

    static int faceTarget(Creature target, GhostTower ghostTower) {
        int angle = (int)Math.toDegrees(Math.atan2(target.getPosY() - ghostTower.getGhostTower().getPosY(),
                target.getPosX() - ghostTower.getGhostTower().getPosX()));
        if(angle < 0){
            angle += 360;
        } else
            angle -= 90;
        return angle;
    }
}
