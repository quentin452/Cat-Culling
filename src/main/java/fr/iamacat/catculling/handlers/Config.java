package fr.iamacat.catculling.handlers;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class Config {

    private static final String GENERIC_CATEGORY = "Generic";
    private static final String CULLING_CATEGORY = "Culling Types";

    public static boolean renderNametagsThroughWalls = true;
    public static String[] blockEntityWhitelist;
    public static int tracingDistance = 64;
    public static boolean debugMode = false;
    public static boolean aggressiveMode = false;
    public static int sleepDelay = 10;
    public static int hitboxLimit = 50;

    // Type-based culling configuration options
    public static boolean enableEntityOcclusionCulling = true;
    public static boolean enableTileEntityOcclusionCulling = true;
    public static boolean enableParticleOcclusionCulling = true;
    public static boolean enableParticleFrustumCulling = true;
    public static boolean enableEntityItemFrustumCulling = true;

    public static void load(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(
            new File(event.getModConfigurationDirectory(), "CatCulling.cfg"),
            "1.1",
            true);
        config.load();

        config.addCustomCategoryComment(GENERIC_CATEGORY, "Generic Options");
        config.addCustomCategoryComment(
            CULLING_CATEGORY,
            "Individual Culling Type Controls - Enable or disable specific types of culling");

        tracingDistance = config.getInt(
            "tracingDistance",
            GENERIC_CATEGORY,
            64,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "128 works out to be roughly equal to minecraft's defaults");
        sleepDelay = config.getInt(
            "sleepDelay",
            GENERIC_CATEGORY,
            10,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "The delay between async pathtracing runs " + "that update which TEs need to be culled");
        hitboxLimit = config.getInt(
            "hitboxLimit",
            GENERIC_CATEGORY,
            50,
            Short.MIN_VALUE,
            Short.MAX_VALUE,
            "Limit to a hitbox (anything larger than " + "this will be considered too big to cull)");

        // Type-based culling configuration
        enableEntityOcclusionCulling = config.getBoolean(
            "enableEntityOcclusionCulling",
            CULLING_CATEGORY,
            true,
            "Enable occlusion culling for entities (checks if entities are blocked by blocks)");
        enableTileEntityOcclusionCulling = config.getBoolean(
            "enableTileEntityOcclusionCulling",
            CULLING_CATEGORY,
            true,
            "Enable occlusion culling for tile entities (checks if tile entities are blocked by blocks)");
        enableParticleOcclusionCulling = config.getBoolean(
            "enableParticleOcclusionCulling",
            CULLING_CATEGORY,
            true,
            "Enable occlusion culling for particles (checks if particles are blocked by blocks)");
        enableParticleFrustumCulling = config.getBoolean(
            "enableParticleFrustumCulling",
            CULLING_CATEGORY,
            true,
            "Enable frustum culling for particles (checks if particles are outside camera view)");
        enableEntityItemFrustumCulling = config.getBoolean(
            "enableEntityItemFrustumCulling",
            CULLING_CATEGORY,
            true,
            "Enable frustum culling for item entities (checks if dropped items are outside camera view)");

        String blockEntityWhitelistString = config
            .get(
                GENERIC_CATEGORY,
                "entityWhitelist",
                "tile.beacon",
                "Comma-separated list of entities and blocks whitelisted " + "from this mod, e.g. tile.beacon")
            .getString();
        blockEntityWhitelist = blockEntityWhitelistString.split(",");

        renderNametagsThroughWalls = config
            .getBoolean("renderNametagsThroughWalls", GENERIC_CATEGORY, true, "Renders nametags through walls");
        debugMode = config.getBoolean("debugMode", GENERIC_CATEGORY, false, "Try this before sending an issue report.");
        aggressiveMode = config.getBoolean(
            "aggressiveMode",
            GENERIC_CATEGORY,
            false,
            "Aggressively calculate bounding box culling with no breathing room. "
                + "May result in additional performance at the cost of stability "
                + "and/or graphics issues.");

        config.save();
    }
}
