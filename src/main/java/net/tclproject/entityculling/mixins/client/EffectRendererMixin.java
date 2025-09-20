package net.tclproject.entityculling.mixins.client;

import java.util.List;
import java.util.concurrent.Callable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.EntityCullingBase;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableParticleWrapper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.logisticscraft.occlusionculling.util.Vec3d;

@Mixin(EffectRenderer.class)
public class EffectRendererMixin {

    // Reusable Vec3d instances to avoid allocations
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
    private List[] fxLayers = new List[4];
    @Shadow
    private TextureManager renderer;
    @Shadow
    private static final ResourceLocation particleTextures = new ResourceLocation("textures/particle/particles.png");

    @Inject(method = "renderParticles", at = @At("HEAD"))
    private void entityculling$captureRenderParams(Entity renderViewEntity, float partialTicks, CallbackInfo ci) {
        this.currentRenderViewEntity = renderViewEntity;
        this.currentRenderPartialTicks = partialTicks;
    }

    /*
     * @Redirect(
     * method = "renderParticles",
     * at =
     * @At(value = "INVOKE",
     * target =
     * "Lnet/minecraft/client/particle/EntityFX;renderParticle(Lnet/"
     * + "minecraft/client/renderer/Tessellator;FFFFFF)V"))
     * private void
     * entityculling$redirectRenderParticle(EntityFX particle,
     * Tessellator tessellator,
     * float partialTicks, float rotationX,
     * float rotationZ, float rotationYZ,
     * float rotationXY, float rotationXZ) {
     * if (shouldRenderParticle(particle)) {
     * particle.renderParticle(tessellator, partialTicks, rotationX, rotationZ,
     * rotationYZ, rotationXY, rotationXZ);
     * EntityCulling.instance.renderedParticles++;
     * } else {
     * EntityCulling.instance.skippedParticles++;
     * }
     * }
     */
    @Overwrite()
    public void renderParticles(Entity p_78874_1_, float p_78874_2_) {
        float f1 = ActiveRenderInfo.rotationX;
        float f2 = ActiveRenderInfo.rotationZ;
        float f3 = ActiveRenderInfo.rotationYZ;
        float f4 = ActiveRenderInfo.rotationXY;
        float f5 = ActiveRenderInfo.rotationXZ;
        EntityFX.interpPosX = p_78874_1_.lastTickPosX
            + (p_78874_1_.posX - p_78874_1_.lastTickPosX) * (double) p_78874_2_;
        EntityFX.interpPosY = p_78874_1_.lastTickPosY
            + (p_78874_1_.posY - p_78874_1_.lastTickPosY) * (double) p_78874_2_;
        EntityFX.interpPosZ = p_78874_1_.lastTickPosZ
            + (p_78874_1_.posZ - p_78874_1_.lastTickPosZ) * (double) p_78874_2_;

        for (int k = 0; k < 3; ++k) {
            final int i = k;

            if (!this.fxLayers[i].isEmpty()) {
                switch (i) {
                    case 0:
                    default:
                        this.renderer.bindTexture(particleTextures);
                        break;
                    case 1:
                        this.renderer.bindTexture(TextureMap.locationBlocksTexture);
                        break;
                    case 2:
                        this.renderer.bindTexture(TextureMap.locationItemsTexture);
                }

                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glDepthMask(false);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569F);
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawingQuads();

                for (int j = 0; j < this.fxLayers[i].size(); ++j) {
                    final EntityFX entityfx = (EntityFX) this.fxLayers[i].get(j);
                    if (entityfx == null) continue;
                    tessellator.setBrightness(entityfx.getBrightnessForRender(p_78874_2_));

                    try {
                        if (shouldRenderParticle(entityfx)) {
                            entityfx.renderParticle(tessellator, p_78874_2_, f1, f5, f2, f3, f4);
                            EntityCulling.instance.renderedParticles++;
                        } else {
                            EntityCulling.instance.skippedParticles++;
                        }
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Rendering Particle");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Particle being rendered");
                        crashreportcategory.addCrashSectionCallable("Particle", new Callable() {

                            public String call() {
                                return entityfx.toString();
                            }
                        });
                        crashreportcategory.addCrashSectionCallable("Particle Type", new Callable() {

                            public String call() {
                                return i == 0 ? "MISC_TEXTURE"
                                    : (i == 1 ? "TERRAIN_TEXTURE"
                                        : (i == 2 ? "ITEM_TEXTURE"
                                            : (i == 3 ? "ENTITY_PARTICLE_TEXTURE" : "Unknown - " + i)));
                            }
                        });
                        throw new ReportedException(crashreport);
                    }
                }

                tessellator.draw();
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDepthMask(true);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            }
        }
    }

    @Overwrite
    public void renderLitParticles(Entity p_78872_1_, float p_78872_2_) {
        float f1 = 0.017453292F;
        float f2 = MathHelper.cos(p_78872_1_.rotationYaw * 0.017453292F);
        float f3 = MathHelper.sin(p_78872_1_.rotationYaw * 0.017453292F);
        float f4 = -f3 * MathHelper.sin(p_78872_1_.rotationPitch * 0.017453292F);
        float f5 = f2 * MathHelper.sin(p_78872_1_.rotationPitch * 0.017453292F);
        float f6 = MathHelper.cos(p_78872_1_.rotationPitch * 0.017453292F);
        byte b0 = 3;
        List list = this.fxLayers[b0];

        if (!list.isEmpty()) {
            Tessellator tessellator = Tessellator.instance;

            for (int i = 0; i < list.size(); ++i) {
                EntityFX entityfx = (EntityFX) list.get(i);
                if (entityfx == null) continue;
                tessellator.setBrightness(entityfx.getBrightnessForRender(p_78872_2_));
                if (shouldRenderParticle(entityfx)) {
                    entityfx.renderParticle(tessellator, p_78872_2_, f1, f5, f2, f3, f4);
                    EntityCulling.instance.renderedParticles++;
                } else {
                    EntityCulling.instance.skippedParticles++;
                }
            }
        }
    }

    /**
     * Determines if a particle should be rendered based on visibility culling
     */
    private boolean shouldRenderParticle(EntityFX particle) {
        // Check if particle culling is enabled
        if (!EntityCullingBase.enabled) {
            return true; // Continue normal rendering
        }

        // Get wrapper for this particle
        CullableParticleWrapper cullable = CullableEntityRegistry.getWrapper(particle);

        // Check if forced visible
        if (cullable.isForcedVisible()) {
            return true;
        }

        // Calculate visibility on-the-fly to avoid delay from CullTask updates
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null || EntityCulling.instance.culling == null) {
                return true;
            }

            // Skip culling if in spectator mode or no-clip
            if (mc.thePlayer.noClip || mc.gameSettings.thirdPersonView != 0) {
                return true;
            }

            // Get camera position (manually interpolate for 1.7.10)
            double cameraX = currentRenderViewEntity.lastTickPosX
                + (currentRenderViewEntity.posX - currentRenderViewEntity.lastTickPosX) * currentRenderPartialTicks;
            double cameraY = currentRenderViewEntity.lastTickPosY
                + (currentRenderViewEntity.posY - currentRenderViewEntity.lastTickPosY) * currentRenderPartialTicks;
            double cameraZ = currentRenderViewEntity.lastTickPosZ
                + (currentRenderViewEntity.posZ - currentRenderViewEntity.lastTickPosZ) * currentRenderPartialTicks;
            TEMP_CAMERA.set(cameraX, cameraY, cameraZ);

            // Check distance limit
            double dx = particle.posX - cameraX;
            double dy = particle.posY - cameraY;
            double dz = particle.posZ - cameraZ;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq > Config.tracingDistance * Config.tracingDistance) {
                return true; // Too far to bother culling
            }

            // Create bounding box for particle (slightly larger to avoid edge cases)
            double size = 0.3; // Slightly larger than CullTask to avoid leaks
            TEMP_AABB_MIN.set(particle.posX - size, particle.posY - size, particle.posZ - size);
            TEMP_AABB_MAX.set(particle.posX + size, particle.posY + size, particle.posZ + size);

            // Test visibility
            boolean visible = EntityCulling.instance.culling.isAABBVisible(TEMP_AABB_MIN, TEMP_AABB_MAX, TEMP_CAMERA);

            // Update wrapper state for consistency with CullTask
            cullable.setCulled(!visible);

            return visible;

        } catch (Exception e) {
            // If anything goes wrong, default to rendering
            return true;
        }
    }
}
