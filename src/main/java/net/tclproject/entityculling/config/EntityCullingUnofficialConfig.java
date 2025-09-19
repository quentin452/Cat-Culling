package net.tclproject.entityculling.config;

import com.falsepattern.lib.config.Config;

@Config(modid = "entityculling")
public class EntityCullingUnofficialConfig {

    @Config.Comment("Fix modded particles to support correctly particle culling for them")
    @Config.DefaultBoolean(true)
    @Config.RequiresWorldRestart
    public static boolean fixModdedParticles;
}
