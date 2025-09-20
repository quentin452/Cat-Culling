package net.tclproject.entityculling.mixins.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.EntityCullingBase;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableParticleWrapper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.logisticscraft.occlusionculling.util.Vec3d;

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

    @Shadow
    private List[] fxLayers;
    @Shadow
    private TextureManager renderer;
    @Shadow
    private static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void entityculling$captureRenderParams(Entity renderViewEntity, float partialTicks, CallbackInfo ci) {
        this.currentRenderViewEntity = renderViewEntity;
        this.currentRenderPartialTicks = partialTicks;
    }

    @Redirect(
        method = "renderParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/"
                + "minecraft/client/renderer/Tessellator;FFFFFF)V"))
    private void entityculling$redirectRenderParticle(EntityFX particle, Tessellator tessellator, float partialTicks,
        float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (shouldRenderParticle(particle)) {
            particle
                .renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            EntityCulling.instance.renderedParticles++;
        } else {
            EntityCulling.instance.skippedParticles++;
        }
    }

    @Redirect(
        method = "renderLitParticles",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/"
                + "minecraft/client/renderer/Tessellator;FFFFFF)V"))
    private void entityculling$redirectRenderLitParticle(EntityFX particle, Tessellator tessellator, float partialTicks,
        float rotationX, float rotationZ, float rotationYZ, float rotationXY, float rotationXZ) {
        if (shouldRenderParticle(particle)) {
            particle
                .renderParticle(tessellator, partialTicks, rotationX, rotationZ, rotationYZ, rotationXY, rotationXZ);
            EntityCulling.instance.renderedParticles++;
        } else {
            EntityCulling.instance.skippedParticles++;
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
            if (mc.thePlayer == null || mc.theWorld == null || EntityCulling.instance.culling == null) {
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

            double size = 0.3;
            TEMP_AABB_MIN.set(particle.posX - size, particle.posY - size, particle.posZ - size);
            TEMP_AABB_MAX.set(particle.posX + size, particle.posY + size, particle.posZ + size);

            boolean visible = EntityCulling.instance.culling.isAABBVisible(TEMP_AABB_MIN, TEMP_AABB_MAX, TEMP_CAMERA);
            cullable.setCulled(!visible);
            return visible;

        } catch (Exception e) {
            return true;
        }
    }
}
