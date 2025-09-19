package net.tclproject.entityculling.mixins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.EntityCullingBase;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableParticleWrapper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.logisticscraft.occlusionculling.util.Vec3d;

@Mixin(EntityFX.class)
public class EntityFXMixin {

    // Reusable Vec3d instances to avoid allocations
    private static final Vec3d TEMP_CAMERA = new Vec3d(0, 0, 0);
    private static final Vec3d TEMP_AABB_MIN = new Vec3d(0, 0, 0);
    private static final Vec3d TEMP_AABB_MAX = new Vec3d(0, 0, 0);

    @Inject(method = "renderParticle", at = @At("HEAD"), cancellable = true)
    private void entityculling$cullParticle(Tessellator tessellator, float partialTicks, float cosYaw, float cosPitch,
        float sinYaw, float sinSinPitch, float cosSinPitch, CallbackInfo ci) {
        EntityFX particle = (EntityFX) (Object) this;

        // Check if particle culling is enabled
        if (!EntityCullingBase.enabled) {
            EntityCulling.instance.renderedParticles++;
            return; // Continue normal rendering
        }

        // Get wrapper for this particle
        CullableParticleWrapper cullable = CullableEntityRegistry.getWrapper(particle);

        // Check if forced visible
        if (cullable.isForcedVisible()) {
            EntityCulling.instance.renderedParticles++;
            return;
        }

        // Calculate visibility on-the-fly to avoid delay from CullTask updates
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null || EntityCulling.instance.culling == null) {
                EntityCulling.instance.renderedParticles++;
                return;
            }

            // Skip culling if in spectator mode or no-clip
            if (mc.thePlayer.noClip || mc.gameSettings.thirdPersonView != 0) {
                EntityCulling.instance.renderedParticles++;
                return;
            }

            // Get camera position
            Vec3 cameraPos = mc.renderViewEntity.getPosition(partialTicks);
            TEMP_CAMERA.set(cameraPos.xCoord, cameraPos.yCoord, cameraPos.zCoord);

            // Check distance limit
            double dx = particle.posX - cameraPos.xCoord;
            double dy = particle.posY - cameraPos.yCoord;
            double dz = particle.posZ - cameraPos.zCoord;
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (distanceSq > Config.tracingDistance * Config.tracingDistance) {
                EntityCulling.instance.renderedParticles++;
                return; // Too far to bother culling
            }

            // Create bounding box for particle (slightly larger to avoid edge cases)
            double size = 0.3; // Slightly larger than CullTask to avoid leaks
            TEMP_AABB_MIN.set(particle.posX - size, particle.posY - size, particle.posZ - size);
            TEMP_AABB_MAX.set(particle.posX + size, particle.posY + size, particle.posZ + size);

            // Test visibility
            boolean visible = EntityCulling.instance.culling.isAABBVisible(TEMP_AABB_MIN, TEMP_AABB_MAX, TEMP_CAMERA);

            if (!visible) {
                // Update wrapper state for consistency with CullTask
                cullable.setCulled(true);
                EntityCulling.instance.skippedParticles++;
                ci.cancel(); // Skip rendering
                return;
            }

            // Particle is visible
            cullable.setCulled(false);
            EntityCulling.instance.renderedParticles++;

        } catch (Exception e) {
            // If anything goes wrong, default to rendering
            EntityCulling.instance.renderedParticles++;
        }
    }
}
