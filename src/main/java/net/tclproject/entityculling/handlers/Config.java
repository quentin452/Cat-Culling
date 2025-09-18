package net.tclproject.entityculling.handlers;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.File;
import net.minecraftforge.common.config.Configuration;

public class Config {
  private static final String GENERIC_CATEGORY = "Generic";

  public static boolean renderNametagsThroughWalls = true;
  public static String[] blockEntityWhitelist;
  public static int tracingDistance = 128;
  public static boolean debugMode = false;
  public static boolean aggressiveMode = false;
  public static int sleepDelay = 10;
  public static int hitboxLimit = 50;

  public static void load(FMLPreInitializationEvent event) {
    Configuration config =
        new Configuration(
            new File(event.getModConfigurationDirectory(), "EntityCulling.cfg"), "1.1", true);
    config.load();

    config.addCustomCategoryComment(GENERIC_CATEGORY, "Generic Options");

    tracingDistance =
        config.getInt(
            "tracingDistance",
            GENERIC_CATEGORY,
            128,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "128 works out to be roughly equal to minecraft's defaults");
    sleepDelay =
        config.getInt(
            "sleepDelay",
            GENERIC_CATEGORY,
            10,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "The delay between async pathtracing runs that update which TEs need to be culled");
    hitboxLimit =
        config.getInt(
            "hitboxLimit",
            GENERIC_CATEGORY,
            50,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "Limit to a hitbox (anything larger than this will be considered too big to cull)");

    String blockEntityWhitelistString =
        config
            .get(
                GENERIC_CATEGORY,
                "entityWhitelist",
                "tile.beacon",
                "Comma-separated list of entities and blocks whitelisted from this mod, e.g. tile.beacon")
            .getString();
    blockEntityWhitelist = blockEntityWhitelistString.split(",");

    renderNametagsThroughWalls =
        config.getBoolean(
            "renderNametagsThroughWalls", GENERIC_CATEGORY, true, "Renders nametags through walls");
    debugMode =
        config.getBoolean(
            "debugMode", GENERIC_CATEGORY, false, "Try this before sending an issue report.");
    aggressiveMode =
        config.getBoolean(
            "aggressiveMode",
            GENERIC_CATEGORY,
            false,
            "Aggressively calculate bounding box culling with no breathing room. May result in additional performance at the cost of stability and/or graphics issues.");

    config.save();
  }
}
