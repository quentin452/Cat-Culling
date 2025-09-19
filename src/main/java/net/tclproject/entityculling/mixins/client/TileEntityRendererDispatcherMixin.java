package net.tclproject.entityculling.mixins.client;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin {

    @Inject(method = "func_147549_a", at = @At("HEAD"), cancellable = true)
    private void entityculling$cullTileEntity(TileEntity tileEntity, double x, double y, double z, float partialTicks,
        CallbackInfo ci) {
        CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(tileEntity);

        if (!cullable.isForcedVisible() && cullable.isCulled()) {
            EntityCulling.instance.skippedBlockEntities++;
            ci.cancel();
            return;
        }

        EntityCulling.instance.renderedBlockEntities++;
    }
}
