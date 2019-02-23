package com.joedobo27.gtgm;


import com.joedobo27.libs.creature.CreatureTemplateImporter;
import com.joedobo27.libs.item.AdvancedCreationImporter;
import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.ai.CreatureAI;
import org.gotti.wurmunlimited.modloader.interfaces.*;

import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Logger;


public class GhostTowerGuardMod implements WurmServerMod, Initable, ItemTemplatesCreatedListener, Configurable,
        ServerStartedListener, PlayerMessageListener {

    static final Logger logger = Logger.getLogger(GhostTowerGuardMod.class.getName());

    @Override
    public void configure(Properties properties) {
        ConfigureOptions.setOptions(properties);
    }

    @Override
    public void init() {
        GhostTower.injectAddTowerCode();
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

        HashMap<String, CreatureAI> creatureAIHashMap = new HashMap<>();
        creatureAIHashMap.put("jdbTowerGhost", new TowerGuardAI());
        CreatureTemplateImporter.importTemplates("mods\\GhostTowerGuardMod\\CreatureTemplates\\",
                "mods\\jdbCommon\\", creatureAIHashMap);
        GhostTower.initialize();
    }

    @Override
    public MessagePolicy onPlayerMessage(Communicator communicator, String message, String title) {
        return  GhostTower.onPlayerMessage(communicator, message, title);
    }

    @Override
    public boolean onPlayerMessage(Communicator communicator, String s) {
        return false;
    }

}
