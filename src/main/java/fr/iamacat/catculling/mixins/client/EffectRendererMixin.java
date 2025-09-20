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
    private static final Vec3d TEMP_AABB_MIN = new Vec3d(0, 0, 0);
    @Unique
    private static final Vec3d TEMP_AABB_MAX = new Vec3d(0, 0, 0);
    @Unique
    private Entity currentRenderViewEntity;
    @Unique
    private float currentRenderPartialTicks;
    @Unique
    private Frustrum currentFrustum;
    @Unique
    private static final net.minecraft.util.AxisAlignedBB TEMP_PARTICLE_AABB = 
        net.minecraft.util.AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    @Unique
    private static final double PARTICLE_SIZE = 0.3;
    
    // Cache for camera position to avoid recalculating for every particle
    @Unique
    private double cachedCameraX = Double.NaN;
    @Unique
    private double cachedCameraY = Double.NaN; 
    @Unique
    private double cachedCameraZ = Double.NaN;
    @Unique
    private boolean cameraPositionCached = false;

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
        
        // Pre-calculate camera position once per render frame
        cachedCameraX = renderViewEntity.lastTickPosX
            + (renderViewEntity.posX - renderViewEntity.lastTickPosX) * partialTicks;
        cachedCameraY = renderViewEntity.lastTickPosY
            + (renderViewEntity.posY - renderViewEntity.lastTickPosY) * partialTicks;
        cachedCameraZ = renderViewEntity.lastTickPosZ
            + (renderViewEntity.posZ - renderViewEntity.lastTickPosZ) * partialTicks;
        cameraPositionCached = true;
        
        TEMP_CAMERA.set(cachedCameraX, cachedCameraY, cachedCameraZ);

        // Get the current frustum using ClippingHelper which is available in 1.7.10
        try {
            // Create a new frustum for this render pass
            this.currentFrustum = new Frustrum();
            this.currentFrustum.setPosition(cachedCameraX, cachedCameraY, cachedCameraZ);
        } catch (Exception e) {
            this.currentFrustum = null;
        }
    }

    @Inject(method = "renderParticles", at = @At("RETURN"))
    private void catculling$resetCacheOnReturn(Entity renderViewEntity, float partialTicks, CallbackInfo ci) {
        // Reset cache flag for next render frame
        cameraPositionCached = false;
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

            // Use cached camera position if available, otherwise calculate
            double cameraX, cameraY, cameraZ;
            if (cameraPositionCached) {
                cameraX = cachedCameraX;
                cameraY = cachedCameraY;
                cameraZ = cachedCameraZ;
            } else {
                cameraX = currentRenderViewEntity.lastTickPosX
                    + (currentRenderViewEntity.posX - currentRenderViewEntity.lastTickPosX) * currentRenderPartialTicks;
                cameraY = currentRenderViewEntity.lastTickPosY
                    + (currentRenderViewEntity.posY - currentRenderViewEntity.lastTickPosY) * currentRenderPartialTicks;
                cameraZ = currentRenderViewEntity.lastTickPosZ
                    + (currentRenderViewEntity.posZ - currentRenderViewEntity.lastTickPosZ) * currentRenderPartialTicks;
                TEMP_CAMERA.set(cameraX, cameraY, cameraZ);
            }

            // Early distance check
            double dx = particle.posX - cameraX;
            double dy = particle.posY - cameraY;
            double dz = particle.posZ - cameraZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq > Config.tracingDistance * Config.tracingDistance) {
                return true;
            }

            // First check frustum culling if available - reuse AABB object
            if (currentFrustum != null) {
                // Reuse the static AABB object to avoid allocation
                TEMP_PARTICLE_AABB.setBounds(
                    particle.posX - PARTICLE_SIZE,
                    particle.posY - PARTICLE_SIZE,
                    particle.posZ - PARTICLE_SIZE,
                    particle.posX + PARTICLE_SIZE,
                    particle.posY + PARTICLE_SIZE,
                    particle.posZ + PARTICLE_SIZE);

                if (!currentFrustum.isBoundingBoxInFrustum(TEMP_PARTICLE_AABB)) {
                    cullable.setCulled(true);
                    return false;
                }
            }

            // Use pre-calculated AABB bounds
            TEMP_AABB_MIN.set(particle.posX - PARTICLE_SIZE, particle.posY - PARTICLE_SIZE, particle.posZ - PARTICLE_SIZE);
            TEMP_AABB_MAX.set(particle.posX + PARTICLE_SIZE, particle.posY + PARTICLE_SIZE, particle.posZ + PARTICLE_SIZE);

            boolean visible = CatCullingBase.instance.culling.isAABBVisible(TEMP_AABB_MIN, TEMP_AABB_MAX, TEMP_CAMERA);
            cullable.setCulled(!visible);
            return visible;

        } catch (Exception e) {
            return true;
        }
    }
}
