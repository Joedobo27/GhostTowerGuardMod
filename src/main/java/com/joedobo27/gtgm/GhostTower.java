package com.joedobo27.gtgm;


import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class GhostTower implements Comparable<GhostTower>{

    private final Item ghostTower;

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

    static void addGhostTower(Item ghostTower) {
        new GhostTower(ghostTower);
    }

    synchronized static void removeGhostTower(Item ghostTower) {
        ghostTowers.stream()
                .filter(ghostTower1 -> ghostTower1.ghostTower.getWurmId() == ghostTower.getWurmId())
                .findAny()
                .ifPresent(gt -> ghostTowers.remove(gt));
    }

    @Override
    public int compareTo(@NotNull GhostTower anotherGhostTower) {
        return Long.compare(anotherGhostTower.doCordToHash(), this.doCordToHash());
    }

    private static GhostTower[] getNearGhostTowers(Creature creature) {
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

    Item getGhostTower() {
        return ghostTower;
    }
}
