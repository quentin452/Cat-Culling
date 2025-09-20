package fr.iamacat.catculling.mixins.client;

import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.iamacat.catculling.CatCullingBase;
import fr.iamacat.catculling.handlers.CullableEntityRegistry;
import fr.iamacat.catculling.handlers.CullableEntityWrapper;

@Mixin(TileEntityRendererDispatcher.class)
public class TileEntityRendererDispatcherMixin {

    @Inject(method = "func_147549_a", at = @At("HEAD"), cancellable = true)
    private void catculling$cullTileEntity(TileEntity tileEntity, double x, double y, double z, float partialTicks,
        CallbackInfo ci) {
        CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(tileEntity);

        if (!cullable.isForcedVisible() && cullable.isCulled()) {
            CatCullingBase.instance.skippedBlockEntities++;
            ci.cancel();
            return;
        }

        CatCullingBase.instance.renderedBlockEntities++;
    }
}
