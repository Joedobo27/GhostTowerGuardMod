package com.joedobo27.gtgm;


import com.joedobo27.libs.item.AdvancedCreationImporter;
import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.CreatureTemplate;
import com.wurmonline.server.creatures.CreatureTemplateFactory;
import com.wurmonline.server.skills.SkillList;
import com.wurmonline.server.skills.Skills;
import com.wurmonline.server.skills.SkillsFactory;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.io.IOException;
import java.util.logging.Logger;


public class GhostTowerGuardMod implements WurmServerMod, ItemTemplatesCreatedListener, ServerStartedListener,
        PlayerMessageListener {

    static final Logger logger = Logger.getLogger(GhostTowerGuardMod.class.getName());

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        if (!message.startsWith("ghost") && !message.startsWith("help")) {
            return MessagePolicy.PASS;
        }
        ConfigureOptions options = ConfigureOptions.getInstance();
        if (communicator.getPlayer().getTarget() == null) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage("Ghosts only attack your target.");
            return MessagePolicy.DISCARD;
        }
        GhostTower[] ghostTowers = GhostTower.getNearGhostTowers(communicator.getPlayer());
        if (ghostTowers.length < 1) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    String.format("The %s isn't within %d tiles of a Ghost Tower.",
                            communicator.getPlayer().getTarget().getName(), options.getHelpRespondRange())
            );
            return MessagePolicy.DISCARD;
        }
        GhostTower ghostTower = ghostTowers[0];

        // spawn ghost on top of mob
        Creature target = communicator.getPlayer().getTarget();
        int angle = (int)Math.toDegrees(Math.atan2(target.getPosY() - ghostTower.getGhostTower().getPosY(),
                target.getPosX() - ghostTower.getGhostTower().getPosX()));
        if(angle < 0){
            angle += 360;
        } else
            angle -= 90;
        try {
            Creature creature = Creature.doNew(500, target.getPosX(), target.getPosY(), angle,
                    target.getLayer(), "Tower Ghost", (byte) (Server.rand.nextInt(2)));
            // Set ghost's target to mob
            creature.setOpponent(target);
        }catch (Exception e) {
            logger.warning(e.getMessage());
            return MessagePolicy.DISCARD;
        }

        return MessagePolicy.DISCARD;
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

    @Override
    public void onItemTemplatesCreated() {
        ItemTemplateImporter.importTemplates("mods\\GhostTowerGuardMod\\ItemTemplates\\",
                "mods\\jdbCommon\\");
    }

    @Override
    public void onServerStarted() {
        AdvancedCreationImporter.importAdvancedCreation(
                "mods\\GhostTowerGuardMod\\AdvancedCreationEntries\\", "mods\\jdbCommon\\");

        final Skills skills = SkillsFactory.createSkills("Tower Ghost");
        skills.learnTemp(SkillList.BODY_STRENGTH, 99.9f); //102
        skills.learnTemp(SkillList.BODY_CONTROL, 99.9f); //104
        skills.learnTemp(SkillList.BODY_STAMINA, 99.9f); //103
        skills.learnTemp(SkillList.MIND_LOGICAL, 99.9f); //100
        skills.learnTemp(SkillList.MIND_SPEED, 99.9f); //101
        skills.learnTemp(SkillList.SOUL_STRENGTH, 99.9f); //105
        skills.learnTemp(SkillList.SOUL_DEPTH, 99.9f); //106
        skills.learnTemp(SkillList.WEAPONLESS_FIGHTING, 99.9f);
        skills.learnTemp(SkillList.GROUP_FIGHTING, 99.9f);
        try {
            CreatureTemplate temp = CreatureTemplateFactory.getInstance().createCreatureTemplate(500,
                    "Tower Ghost", "Tower Ghost", "A ghostly tower guard.",
                    "model.creature.humanoid.human.spirit.guard", new int[]{12, 13, 45}, (byte) 0, skills,
                    (short) 100, (byte) 0, (short) 180, (short) 20, (short) 35, "sound.death.spirit.male",
                    "sound.death.spirit.female", "sound.combat.hit.spirit.male",
                    "sound.combat.hit.spirit.female", 0.001f, 1000.0f, 1000.0f,
                    0.0f, 0.0f, 0.0f, 3.0f, 100, new int[0], 100,
                    100, (byte) 2);
            temp.setAlignment(99.9f);
            temp.setBaseCombatRating(100.0f);
            temp.combatDamageType = 2;
            temp.setNoSkillgain(true);
        }catch (IOException e) {
            logger.warning(e.getMessage());
        }
    }
}
