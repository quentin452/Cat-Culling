package net.tclproject.entityculling.asm;

import net.tclproject.entityculling.config.EntityCullingUnofficialConfig;

import org.apache.logging.log4j.Logger;

import com.falsepattern.lib.config.ConfigException;
import com.falsepattern.lib.config.ConfigurationManager;
import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.IMixinPlugin;
import com.falsepattern.lib.mixin.ITargetedMod;

import lombok.Getter;

public class ECMixinPlugin implements IMixinPlugin {

    @Getter
    private final Logger logger = IMixinPlugin.createLogger("EntityCullingUnofficial");

    public ECMixinPlugin() {
        try {
            ConfigurationManager.initialize(EntityCullingUnofficialConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ITargetedMod[] getTargetedModEnumValues() {
        return TargetedMod.values();
    }

    @Override
    public IMixin[] getMixinEnumValues() {
        return Mixin.values();
    }
}
