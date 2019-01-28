package com.joedobo27.gtgm;

public class ConfigureOptions {

    /**
     * max distance a target can be for tower to attack.
     */
    private int helpRespondRange;

    private static final ConfigureOptions instance;

    public int getHelpRespondRange() {
        return helpRespondRange;
    }

    static ConfigureOptions getInstance() {
        return instance;
    }

    static {
        instance = new ConfigureOptions();
    }
}
