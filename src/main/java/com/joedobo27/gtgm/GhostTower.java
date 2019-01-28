package com.joedobo27.gtgm;


import com.joedobo27.libs.item.ItemTemplateImporter;
import com.wurmonline.server.Items;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
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

    static GhostTower[] getNearGhostTowers(Creature creature) {
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
