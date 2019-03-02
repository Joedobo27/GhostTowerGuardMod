package com.joedobo27.gtgm;


import com.joedobo27.libs.bytecode.ByteCodeWild;
import com.joedobo27.libs.creature.CreatureTemplateImporter;
import com.joedobo27.libs.item.AdvancedCreationImporter;
import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.ai.CreatureAI;
import javassist.*;
import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
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
        try {
            ClassPool classPool = HookManager.getInstance().getClassPool();
            CtClass ctAdvancedCreationEntry = classPool.get("com.wurmonline.server.items.AdvancedCreationEntry");
            ctAdvancedCreationEntry.getClassFile().compact();
            String name = "cont";
            String descriptor = Descriptor.ofMethod(classPool.get("com.wurmonline.server.items.Item"), new CtClass[]{
                    classPool.get("com.wurmonline.server.creatures.Creature"),
                    classPool.get("com.wurmonline.server.items.Item"),
                    CtPrimitiveType.longType, CtPrimitiveType.floatType
            });
            CtMethod ctCont = ctAdvancedCreationEntry.getMethod(name, descriptor);

            ByteCodeWild find = new ByteCodeWild(ctAdvancedCreationEntry.getClassFile().getConstPool(),
                    ctCont.getMethodInfo().getCodeAttribute());
            find.addIload("obc", "I");
            find.addFload("endQl", "F");
            find.addIload("mat", "B");
            find.addIload("rarity", "B");
            find.addAload("performer", "Lcom/wurmonline/server/creatures/Creature;");
            find.addInvokevirtual("com/wurmonline/server/creatures/Creature", "getName",
                    "()Ljava/lang/String;");
            find.addInvokestatic("com/wurmonline/server/items/ItemFactory", "createItem",
                    "(IFBBLjava/lang/String;)Lcom/wurmonline/server/items/Item;");
            find.addAstore("newItem", "Lcom/wurmonline/server/items/Item;");
            find.trimFoundBytecode();
            int insertLine = find.getTableLineNumberAfter();

            String source = "" +
                    "if (com.joedobo27.gtgm.GhostTower.isGhostTowerTemplateType(newItem.getTemplateId())) {" +
                    "                        com.joedobo27.gtgm.GhostTower.addGhostTower(newItem.getWurmId());" +
                    "                    }";
            ctCont.insertAt(insertLine, source);

        }catch (NotFoundException | CannotCompileException | RuntimeException e) {
            GhostTowerGuardMod.logger.warning(e.getMessage());
        }
    }

    @Override
    public void onItemTemplatesCreated() {
        ItemTemplateImporter.importTemplates("mods\\GhostTowerGuardMod\\ItemTemplates\\",
                "mods\\jdbCommon\\");

        HashMap<String, CreatureAI> creatureAIHashMap = new HashMap<>();
        creatureAIHashMap.put(TowerGuardAI.id_factory_name, new TowerGuardAI());
        CreatureTemplateImporter.importTemplates("mods\\GhostTowerGuardMod\\CreatureTemplates\\",
                "mods\\jdbCommon\\", creatureAIHashMap);
    }

    @Override
    public void onServerStarted() {
        AdvancedCreationImporter.importAdvancedCreation(
                "mods\\GhostTowerGuardMod\\AdvancedCreationEntries\\", "mods\\jdbCommon\\");

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
