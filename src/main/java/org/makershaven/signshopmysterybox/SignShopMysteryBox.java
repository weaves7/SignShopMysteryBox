package org.makershaven.signshopmysterybox;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.makershaven.signshopmysterybox.operations.givePlayerItemsFromMysteryBox;
import org.wargamer2010.signshop.configuration.SignShopConfig;
import org.wargamer2010.signshop.configuration.configUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SignShopMysteryBox extends JavaPlugin {
    private static final int B_STATS_ID = 13345;
    private static final Logger logger = Logger.getLogger("Minecraft");
    // private static SignShopMysteryBox instance = null;

    public static void log(String message, Level level) {
        if (!message.isEmpty())
            logger.log(level, ("[SignShopMysteryBox] " + message));
    }

    @Override
    public void onEnable() {
        // instance = this;
        PluginManager pm = Bukkit.getServer().getPluginManager();
        if (!pm.isPluginEnabled("SignShop")) {
            log("SignShop is not loaded, can not continue.", Level.SEVERE);
            pm.disablePlugin(this);
            return;
        }

        createDir();

        String filename = "config.yml";
        FileConfiguration ymlThing = configUtil.loadYMLFromPluginFolder(this, filename);
        if (ymlThing != null) {
            configUtil.loadYMLFromJar(this, SignShopMysteryBox.class, ymlThing, filename);

            SignShopConfig.registerExternalOperation(new givePlayerItemsFromMysteryBox());
            SignShopConfig.setupOperations(configUtil.fetchStringStringHashMap("signs", ymlThing), "org.makershaven.signshopmysterybox.operations");
            SignShopConfig.registerErrorMessages(configUtil.fetchStringStringHashMap("errors", ymlThing));
            for (Map.Entry<String, HashMap<String, String>> entry : configUtil.fetchHasmapInHashmap("messages", ymlThing).entrySet()) {
                SignShopConfig.registerMessages(entry.getKey(), entry.getValue());
            }

            getSettings(ymlThing);
        }

        if (SignShopConfig.metricsEnabled()) {
            Metrics metrics = new Metrics(this, B_STATS_ID);
            log("Thank you for enabling metrics!", Level.INFO);
        }
        log("Enabled", Level.INFO);

    }

    private void getSettings(FileConfiguration ymlThing) {
        //EnabledWorlds = ymlThing.getStringList("EnabledWorlds"); // Empty list if not found
        // EnableSaveXP = ymlThing.getBoolean("EnableSaveXP", EnableSaveXP);
    }

    private void createDir() {
        if (!this.getDataFolder().exists()) {
            if (!this.getDataFolder().mkdir()) {
                log("Could not create plugin folder!", Level.SEVERE);
            }
        }
    }
}
