package net.tclproject.entityculling.asm;

import java.util.List;
import java.util.function.Predicate;

import net.tclproject.entityculling.config.EntityCullingUnofficialConfig;

import com.falsepattern.lib.mixin.IMixin;
import com.falsepattern.lib.mixin.ITargetedMod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Mixin implements IMixin {

    client_TestMixin(Side.CLIENT, m -> EntityCullingUnofficialConfig.fixModdedParticles, "TestMixin"),

    // MOD-FILTERED MIXINS

    // The modFilter argument is a predicate, so you can also use the .and(),
    // .or(), and .negate() methods to mix and match multiple predicates.
    ;

    @Getter
    public final Side side;
    @Getter
    public final Predicate<List<ITargetedMod>> filter;
    @Getter
    public final String mixin;

    static Predicate<List<ITargetedMod>> require(TargetedMod in) {
        return modList -> modList.contains(in);
    }

    static Predicate<List<ITargetedMod>> avoid(TargetedMod in) {
        return modList -> !modList.contains(in);
    }
}
