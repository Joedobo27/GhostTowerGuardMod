package com.joedobo27.gtgm;


import com.joedobo27.libs.bytecode.ByteCodeWild;
import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import javassist.*;
import javassist.bytecode.Descriptor;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GhostTower implements Comparable<GhostTower>{

    private final Item ghostTower;
    private final static GhostTower[] EMPTY_TOWERS = new GhostTower[0];

    //private static SortedSet<GhostTower> ghostTowers = Collections.synchronizedSortedSet(new TreeSet<>());
    private static List<GhostTower> ghostTowers = Collections.synchronizedList(new ArrayList<>());

    private GhostTower(Item ghostTower) {
        this.ghostTower = ghostTower;
        ghostTowers.add(this);
    }

    static MessagePolicy onPlayerMessage(Communicator communicator, String message, String title){
        if (!message.startsWith("ghost") && !message.startsWith("help")) {
            return MessagePolicy.PASS;
        }
        ConfigureOptions options = ConfigureOptions.getInstance();
        if (communicator.getPlayer().getTarget() == null) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage("Ghosts only attack your target.");
            return MessagePolicy.DISCARD;
        }
        GhostTower[] ghostTowers = GhostTower.getNearGhostTowers(communicator.getPlayer().getWurmId());
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

        try {
            Creature creature = Creature.doNew(IdFactory.getIdFor(options.getTowerGhostIdName(), IdType.CREATURETEMPLATE)
                    , target.getPosX(), target.getPosY(), TowerGuardAI.faceTarget(target, ghostTower), target.getLayer(),
                    "Tower Ghost", (byte) (Server.rand.nextInt(2)));
            // Set ghost's target to mob
            creature.setOpponent(target);
            creature.setTarget(target.getWurmId(), true);
            creature.attackTarget();
        }catch (Exception e) {
            GhostTowerGuardMod.logger.warning(e.getMessage());
            return MessagePolicy.DISCARD;
        }

        return MessagePolicy.DISCARD;
    }

    static void initialize() {
            Item[] ghostTowers = Arrays.stream(Items.getAllItems())
                .filter(item -> item.getTemplateId() == ItemTemplateImporter.getItemTemplateBuilderNames().get("jdbSquareTower")
                        || item.getTemplateId() == ItemTemplateImporter.getItemTemplateBuilderNames().get("jdbBlackTower") ||
                        item.getTemplateId() == ItemTemplateImporter.getItemTemplateBuilderNames().get("jdbRoundTower") ||
                        item.getTemplateId() == ItemTemplateImporter.getItemTemplateBuilderNames().get("jdbFortTower"))
                .toArray(Item[]::new);
            Arrays.stream(ghostTowers)
                    .forEach(GhostTower::new);
    }

    static void addGhostTower(long itemId) {
        Items.getItemOptional(itemId).ifPresent(GhostTower::new);
    }

    synchronized static void removeGhostTower(long itemId) {
        ghostTowers.stream()
                .filter(ghostTower1 -> ghostTower1.ghostTower.getWurmId() == itemId)
                .findAny()
                .ifPresent(gt -> ghostTowers.remove(gt));
    }

    @Override
    public int compareTo(@NotNull GhostTower anotherGhostTower) {
        return Long.compare(anotherGhostTower.doCordToHash(), this.doCordToHash());
    }

    private static GhostTower[] getNearGhostTowers(long creatureId) {
        Creature creature = Server.getInstance().getCreatureOrNull(creatureId);
        if (creature == null) {
            return EMPTY_TOWERS;
        }
        ConfigureOptions options = ConfigureOptions.getInstance();
        return ghostTowers.stream()
                .filter(ghostTower1 ->
                        creature.getTileX() > ghostTower1.ghostTower.getTileX() - options.getHelpRespondRange() &&
                        creature.getTileX() < ghostTower1.ghostTower.getTileX() + options.getHelpRespondRange() &&
                        creature.getTileY() > ghostTower1.ghostTower.getTileY() - options.getHelpRespondRange() &&
                        creature.getTileY() < ghostTower1.ghostTower.getTileY() + options.getHelpRespondRange())
                .toArray(GhostTower[]::new);
    }

    private long doCordToHash() {
        int y = this.ghostTower.getTileY() * 2^Server.surfaceMesh.getSizeLevel();
        return y + this.ghostTower.getTileX();
    }

    private static long doCordToHash(Item item) {
        int y = item.getTileY() * 2^Server.surfaceMesh.getSizeLevel();
        return y + item.getTileX();
    }

    void cleanGhostTowers(){
        Set<GhostTower> ghostTowers1 = ghostTowers.stream()
                .filter(ghostTower1 -> Arrays.stream(Items.getAllItems())
                        .noneMatch(item -> ghostTower1.ghostTower.getWurmId() == item.getWurmId()))
                .collect(Collectors.toSet());
        if (ghostTowers1.size() > 0) {
            ghostTowers1.forEach(ghostTower1 -> ghostTowers.remove(ghostTower1));
        }
    }

    static public void injectAddTowerCode() {
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

    public static boolean isGhostTowerTemplateType(int templateId) {
        ConfigureOptions options = ConfigureOptions.getInstance();
        return templateId == IdFactory.getIdFor(options.getBlackTowerIdName(), IdType.ITEMTEMPLATE) ||
                templateId == IdFactory.getIdFor(options.getFortTowerIdName(), IdType.ITEMTEMPLATE) ||
                templateId == IdFactory.getIdFor(options.getRoundTowerIdName(), IdType.ITEMTEMPLATE) ||
                templateId == IdFactory.getIdFor(options.getSquareTowerIdName(), IdType.ITEMTEMPLATE);
    }

    Item getGhostTower() {
        return ghostTower;
    }
}
