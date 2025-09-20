package fr.iamacat.catculling.mixins.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.logisticscraft.occlusionculling.util.Vec3d;

import fr.iamacat.catculling.CatCullingBase;
import fr.iamacat.catculling.EntityCullingBase;
import fr.iamacat.catculling.handlers.Config;
import fr.iamacat.catculling.handlers.CullableEntityRegistry;
import fr.iamacat.catculling.handlers.CullableParticleWrapper;

@Mixin(EffectRenderer.class)
public class EffectRendererMixin {

    @Unique
    private static final Vec3d TEMP_CAMERA = new Vec3d(0, 0, 0);
    @Unique
    private static final Vec3d TEMP_PARTICLE = new Vec3d(0, 0, 0);
    @Unique
    private Entity currentRenderViewEntity;
    @Unique
    private float currentRenderPartialTicks;
    @Unique
    private Frustrum currentFrustum;

    @Shadow
    private List[] fxLayers;
    @Shadow
    private TextureManager renderer;
    @Shadow
    private static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void catculling$captureRenderParams(Entity renderViewEntity, float partialTicks, CallbackInfo ci) {
        this.currentRenderViewEntity = renderViewEntity;
        this.currentRenderPartialTicks = partialTicks;

        // Get the current frustum using ClippingHelper which is available in 1.7.10
        try {
            // Create a new frustum for this render pass
            this.currentFrustum = new Frustrum();
            // Get the interpolated camera position
            double cameraX = renderViewEntity.lastTickPosX
                + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
            double cameraY = renderViewEntity.lastTickPosY
                + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
            double cameraZ = renderViewEntity.lastTickPosZ
                + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
            this.currentFrustum.setPosition(cameraX, cameraY, cameraZ);
        } catch (Exception e) {
            this.currentFrustum = null;
        }
    }

    @Redirect(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/"
                + "minecraft/client/renderer/Tessellator;FFFFFF)V"))
    private void catculling$redirectRenderParticle(EntityFX particle, Tessellator tessellator, float partialTicks,
        float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (shouldRenderParticle(particle)) {
            particle
                .renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            CatCullingBase.instance.renderedParticles++;
        } else {
            CatCullingBase.instance.skippedParticles++;
        }
    }

    @Redirect(
        method = "renderLitParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/"
                + "minecraft/client/renderer/Tessellator;FFFFFF)V"))
    private void catculling$redirectRenderLitParticle(EntityFX particle, Tessellator tessellator, float partialTicks,
        float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (shouldRenderParticle(particle)) {
            particle
                .renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            CatCullingBase.instance.renderedParticles++;
        } else {
            CatCullingBase.instance.skippedParticles++;
        }
    }

    @Unique
    private boolean shouldRenderParticle(EntityFX particle) {
        if (!EntityCullingBase.enabled) {
            return true;
        }

        CullableParticleWrapper cullable = CullableEntityRegistry.getWrapper(particle);
        if (cullable.isForcedVisible()) {
            return true;
        }

        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null || CatCullingBase.instance.culling == null) {
                return true;
            }

            if (mc.thePlayer.noClip || mc.gameSettings.thirdPersonView != 0) {
                return true;
            }

            double cameraX = currentRenderViewEntity.lastTickPosX
                + (currentRenderViewEntity.posX - currentRenderViewEntity.lastTickPosX) * currentRenderPartialTicks;
            double cameraY = currentRenderViewEntity.lastTickPosY
                + (currentRenderViewEntity.posY - currentRenderViewEntity.lastTickPosY) * currentRenderPartialTicks;
            double cameraZ = currentRenderViewEntity.lastTickPosZ
                + (currentRenderViewEntity.posZ - currentRenderViewEntity.lastTickPosZ) * currentRenderPartialTicks;
            TEMP_CAMERA.set(cameraX, cameraY, cameraZ);

            double dx = particle.posX - cameraX;
            double dy = particle.posY - cameraY;
            double dz = particle.posZ - cameraZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq > Config.tracingDistance * Config.tracingDistance) {
                return true;
            }

            // First check frustum culling if available
            if (currentFrustum != null) {
                // Create a small bounding box around the particle for frustum checking
                double size = 0.3;
                net.minecraft.util.AxisAlignedBB particleAABB = net.minecraft.util.AxisAlignedBB.getBoundingBox(
                    particle.posX - size,
                    particle.posY - size,
                    particle.posZ - size,
                    particle.posX + size,
                    particle.posY + size,
                    particle.posZ + size);

                if (!currentFrustum.isBoundingBoxInFrustum(particleAABB)) {
                    cullable.setCulled(true);
                    return false;
                }
            }

            // Use direct particle culling instead of AABB approach for better performance
            TEMP_PARTICLE.set(particle.posX, particle.posY, particle.posZ);
            boolean visible = CatCullingBase.instance.culling.isParticleVisible(TEMP_PARTICLE, TEMP_CAMERA);
            cullable.setCulled(!visible);
            return visible;

        } catch (Exception e) {
            return true;
        }
    }
}
