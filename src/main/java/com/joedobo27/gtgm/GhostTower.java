package com.joedobo27.gtgm;


import com.wurmonline.server.Items;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.villages.Village;
import com.wurmonline.server.villages.Villages;
import org.gotti.wurmunlimited.modloader.interfaces.MessagePolicy;
import org.gotti.wurmunlimited.modsupport.IdFactory;
import org.gotti.wurmunlimited.modsupport.IdType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.joedobo27.libs.item.ItemTemplateImporter.getItemTemplateBuilderNames;

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
        @Nullable Creature target = communicator.getPlayer().getTarget();
        if (target == null) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage("Ghosts only attack your target.");
            return MessagePolicy.PASS;
        }
        Player player = communicator.getPlayer();
        if (player == null) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Broken code occurred, sorry.");
            GhostTowerGuardMod.logger.warning(String.format(
                    "%s, %s, %s  onPlayerMessage error where communicator.player is null.", communicator.toString(),
                    message, title));
            return MessagePolicy.PASS;
        }
        //  NO attacking creatures of same kingdom.
        if (target.getKingdomId() == player.getKingdomId()){
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Ghosts wont attack a creature in your kingdom.");
            return MessagePolicy.PASS;
        }
        // No attacking hitched creatures.
        if (target.isHitched()){
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Ghosts wont attack a hitched creature.");
            return MessagePolicy.PASS;
        }
        // No attacking creatures cared-for by players of same kingdom.
        if (target.isCaredFor()) {
            @Nullable Player playerCareTaker = Players.getInstance().getPlayerOrNull(target.getCareTakerId());
            if (playerCareTaker == null) {
                communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                        "Broken code occurred, sorry. This cared for creature doesn't have an owner.");
                GhostTowerGuardMod.logger.warning(String.format(" %s - %d  cared for creature without a player owner.",
                        target.getName(), target.getWurmId()));
                return MessagePolicy.PASS;
            } else if (playerCareTaker.getKingdomId() == player.getKingdomId()) {
                communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                        "Ghosts won't attack a creature cared-for by a player in your kingdom.");
                return MessagePolicy.PASS;
            }
        }
        // NO attacking creatures branded to villages of same kingdom.
        @Nullable Village targetVillage = target.getBrandVillage();
        if (targetVillage != null && targetVillage.kingdom == player.getKingdomId()) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Ghosts won't attack a creature branded to a village in your kingdom.");
            return MessagePolicy.PASS;
        }
        // No attacking creatures tamed/dominated/charmed by players of same kingdom.
        @Nullable Creature dominator =  target.getDominator();
        if (dominator != null && dominator.getKingdomId() == player.getKingdomId()) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    "Ghosts won't attack a creature controlled by a player in your kingdom.");
            return MessagePolicy.PASS;
        }
        // NO attacking creatures that aren't threats.
         if (Arrays.stream(player.getLatestAttackers())
                .noneMatch(longId -> longId == target.getWurmId())) {
             communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                     "Ghosts won't attack a creature that isn't currently or hasn't recently threatened you.");
             return MessagePolicy.PASS;
         }
         // No village permission to attack on deed.
        @Nullable Village occupiedVillage = Villages.getVillage(target.getTileX(), target.getTileY(), target.isOnSurface());
        if (occupiedVillage != null && (!occupiedVillage.isActionAllowed((short)326, player) ||
                !occupiedVillage.isActionAllowed((short)716, player))) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    String.format("Ghosts refuse to help you because village %s has denied you attacking permission",
                            occupiedVillage.getName()));
        }

        ConfigureOptions options = ConfigureOptions.getInstance();
        GhostTower[] ghostTowers = GhostTower.getNearGhostTowers(communicator.getPlayer().getWurmId());
        if (ghostTowers.length < 1) {
            communicator.getPlayer().getCommunicator().sendNormalServerMessage(
                    String.format("The %s isn't within %d tiles of a Ghost Tower.",
                            communicator.getPlayer().getTarget().getName(), options.getHelpRespondRange())
            );
            return MessagePolicy.PASS;
        }
        GhostTower ghostTower = ghostTowers[0];

        // Spawn ghost on top of mob
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
            return MessagePolicy.PASS;
        }

        return MessagePolicy.PASS;
    }

    static void initialize() {
        ConfigureOptions options = ConfigureOptions.getInstance();
            Item[] ghostTowers = Arrays.stream(Items.getAllItems())
                .filter(item ->
                        item.getTemplateId() == getItemTemplateBuilderNames().get(options.getSquareTowerIdName()) ||
                        item.getTemplateId() == getItemTemplateBuilderNames().get(options.getBlackTowerIdName()) ||
                        item.getTemplateId() == getItemTemplateBuilderNames().get(options.getRoundTowerIdName()) ||
                        item.getTemplateId() == getItemTemplateBuilderNames().get(options.getFortTowerIdName()))
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
