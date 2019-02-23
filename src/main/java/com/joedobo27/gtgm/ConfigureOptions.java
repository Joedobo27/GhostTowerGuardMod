package com.joedobo27.gtgm;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigureOptions {

    /**
     * max tile distance a target can be for tower to attack.
     */
    private int helpRespondRange;
    private String blackTowerIdName;
    private String fortTowerIdName;
    private String roundTowerIdName;
    private String squareTowerIdName;
    private String towerGhostIdName;
    private static ConfigureOptions instance;

    private ConfigureOptions(int helpRespondRange, String blackTowerIdName, String fortTowerIdName,
                             String roundTowerIdName, String squareTowerIdName, String towerGhostIdName) {
        this.helpRespondRange = helpRespondRange;
        this.blackTowerIdName = blackTowerIdName;
        this.fortTowerIdName = fortTowerIdName;
        this.roundTowerIdName = roundTowerIdName;
        this.squareTowerIdName = squareTowerIdName;
        this.towerGhostIdName = towerGhostIdName;
    }

    public int getHelpRespondRange() {
        return helpRespondRange;
    }

    public String getBlackTowerIdName() {
        return blackTowerIdName;
    }

    public String getFortTowerIdName() {
        return fortTowerIdName;
    }

    public String getRoundTowerIdName() {
        return roundTowerIdName;
    }

    public String getSquareTowerIdName() {
        return squareTowerIdName;
    }

    public String getTowerGhostIdName() {
        return towerGhostIdName;
    }

    static ConfigureOptions getInstance() {
        return instance;
    }

    synchronized static void setOptions(@Nullable Properties properties) throws RuntimeException {
        if (instance == null) {
            if (properties == null) {
                properties = getProperties();
            }
            if (properties == null)
                throw new RuntimeException("properties can't be null here.");

            instance = new ConfigureOptions(
                    Integer.valueOf(properties.getProperty("help_respond_range", "20")),
                    properties.getProperty("black_tower_id_name"), properties.getProperty("fort_tower_id_name"),
                    properties.getProperty("round_tower_id_name"), properties.getProperty("square_tower_id_name"),
                    properties.getProperty("tower_ghost_id_name"));
        }
    }

    synchronized static void resetOptions() throws RuntimeException {
        instance = null;
        Properties properties = getProperties();
        if (properties == null)
            throw new RuntimeException("properties can't be null here.");
        instance = new ConfigureOptions(
                Integer.valueOf(properties.getProperty("help_respond_range", "20")),
                properties.getProperty("black_tower_id_name"), properties.getProperty("fort_tower_id_name"),
                properties.getProperty("round_tower_id_name"), properties.getProperty("square_tower_id_name"),
                properties.getProperty("tower_ghost_id_name"));
    }

    private static Properties getProperties() {
        try {
            File configureFile = new File("mods/GhostTowerGuardMod.properties");
            FileInputStream configureStream = new FileInputStream(configureFile);
            Properties configureProperties = new Properties();
            configureProperties.load(configureStream);
            return configureProperties;
        }catch (IOException e) {
            GhostTowerGuardMod.logger.warning(e.getMessage());
            return null;
        }
    }
}
