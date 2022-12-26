package org.makershaven.signshopmysterybox;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.makershaven.signshopmysterybox.operations.givePlayerItemsFromMysteryBox;
import org.wargamer2010.signshop.SignShop;
import org.wargamer2010.signshop.api.Reloadable;
import org.wargamer2010.signshop.configuration.configUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SignShopMysteryBox extends JavaPlugin implements Reloadable {
    private static final int B_STATS_ID = 13345;
    private static final Logger logger = Logger.getLogger("Minecraft");


    public static void debug(String message){
        if (SignShop.getInstance().getSignShopConfig().debugging()) log(message,Level.INFO);
    }
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

        registerConfigurablesWithSignShop();

        if (SignShop.getInstance().getSignShopConfig().metricsEnabled()) {
            Metrics metrics = new Metrics(this, B_STATS_ID);
            log("Thank you for enabling metrics!", Level.INFO);
        }
        log("Enabled", Level.INFO);

    }

    private void createDir() {
        if (!this.getDataFolder().exists()) {
            if (!this.getDataFolder().mkdir()) {
                log("Could not create plugin folder!", Level.SEVERE);
            }
        }
    }

    private void registerConfigurablesWithSignShop() {
        String filename = "config.yml";
        FileConfiguration ymlThing = configUtil.loadYMLFromPluginFolder(this, filename);
        if (ymlThing != null) {
            configUtil.loadYMLFromJar(this, SignShopMysteryBox.class, ymlThing, filename);

            SignShop.getInstance().getSignShopConfig().registerExternalOperation(new givePlayerItemsFromMysteryBox());
            SignShop.getInstance().getSignShopConfig().setupOperations(configUtil.fetchStringStringHashMap("signs", ymlThing), "org.makershaven.signshopmysterybox.operations");
            SignShop.getInstance().getSignShopConfig().registerErrorMessages(configUtil.fetchStringStringHashMap("errors", ymlThing));
            for (Map.Entry<String, HashMap<String, String>> entry : configUtil.fetchHasmapInHashmap("messages", ymlThing).entrySet()) {
                SignShop.getInstance().getSignShopConfig().registerMessages(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public void reload() {
        registerConfigurablesWithSignShop();
    }
}
