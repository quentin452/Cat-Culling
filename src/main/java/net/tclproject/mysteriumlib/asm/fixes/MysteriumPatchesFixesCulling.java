package net.tclproject.mysteriumlib.asm.fixes;

import static net.minecraft.client.renderer.entity.RendererLivingEntity.NAME_TAG_RANGE;
import static net.minecraft.client.renderer.entity.RendererLivingEntity.NAME_TAG_RANGE_SNEAK;

import com.logisticscraft.occlusionculling.util.Vec3d;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.EntityCullingBase;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;
import net.tclproject.entityculling.handlers.CullableParticleWrapper;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import org.lwjgl.opengl.GL11;

public class MysteriumPatchesFixesCulling {

  @Fix(returnSetting = EnumReturnSetting.ON_TRUE)
  public static boolean func_147549_a(
      TileEntityRendererDispatcher tileEntityRendererDispatcher,
      TileEntity p_147549_1_,
      double p_147549_2_,
      double p_147549_4_,
      double p_147549_6_,
      float p_147549_8_) {
    CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(p_147549_1_);
    //        System.out.println(cullable.isForcedVisible() + "," +
    //        cullable.isCulled() + "," +
    // p_147549_1_.getBlockType().getUnlocalizedName());
    if (!cullable.isForcedVisible() && cullable.isCulled()) {
      EntityCulling.instance.skippedBlockEntities++;
      return true;
    }
    EntityCulling.instance.renderedBlockEntities++;
    return false;
  }

  @Fix(returnSetting = EnumReturnSetting.ON_TRUE)
  public static boolean func_147939_a(
      RenderManager rm,
      Entity entity,
      double p_doRenderEntity_2_,
      double d1,
      double d2,
      float tickDelta,
      float p_doRenderEntity_9_,
      boolean p_doRenderEntity_10_) {
    CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(entity);
    if (!cullable.isForcedVisible() && cullable.isCulled()) {
      Render entityRenderer = rm.getEntityRenderObject(entity);
      if (entity instanceof EntityLivingBase) {
        if (Config.renderNametagsThroughWalls && shouldShowName((EntityLivingBase) entity)) {
          renderNameTag((EntityLivingBase) entity, p_doRenderEntity_2_, d1, d2);
          // entityRenderer.doRender(entity, entity.posX, entity.posY,
          // entity.posZ, tickDelta, tickDelta);
        }
      }
      EntityCulling.instance.skippedEntities++;
      return true;
    }
    EntityCulling.instance.renderedEntities++;
    cullable.setOutOfCamera(false);
    return false;
  }

  private static void renderNameTag(
      EntityLivingBase entity, double p_77033_2_, double p_77033_4_, double p_77033_6_) {
    float f = 1.6F;
    float f1 = 0.016666668F * f;
    double d3 = entity.getDistanceSqToEntity(RenderManager.instance.livingPlayer);
    float f2 = entity.isSneaking() ? NAME_TAG_RANGE_SNEAK : NAME_TAG_RANGE;
    GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

    if (d3 < (double) (f2 * f2)) {
      String s = entity.func_145748_c_().getFormattedText();

      if (entity.isSneaking()) {
        FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
        setupRenderForNametag(
            entity, (float) p_77033_2_, (float) p_77033_4_, (float) p_77033_6_, f1);
        GL11.glTranslatef(0.0F, 0.25F / f1, 0.0F);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        tessellator.startDrawingQuads();
        int i = fontrenderer.getStringWidth(s) / 2;
        tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
        tessellator.addVertex((double) (-i - 1), -1.0D, 0.0D);
        tessellator.addVertex((double) (-i - 1), 8.0D, 0.0D);
        tessellator.addVertex((double) (i + 1), 8.0D, 0.0D);
        tessellator.addVertex((double) (i + 1), -1.0D, 0.0D);
        tessellator.draw();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDepthMask(true);
        fontrenderer.drawString(s, -fontrenderer.getStringWidth(s) / 2, 0, 553648127);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
      } else {
        func_96449_a(entity, p_77033_2_, p_77033_4_, p_77033_6_, s, f1, d3);
      }
    }
  }

  protected static void func_96449_a(
      EntityLivingBase p_96449_1_,
      double p_96449_2_,
      double p_96449_4_,
      double p_96449_6_,
      String p_96449_8_,
      float p_96449_9_,
      double p_96449_10_) {
    if (p_96449_1_.isPlayerSleeping()) {
      func_147906_a(p_96449_1_, p_96449_8_, p_96449_2_, p_96449_4_ - 1.5D, p_96449_6_, 64);
    } else {
      func_147906_a(p_96449_1_, p_96449_8_, p_96449_2_, p_96449_4_, p_96449_6_, 64);
    }
  }

  protected static void func_147906_a(
      Entity p_147906_1_,
      String p_147906_2_,
      double p_147906_3_,
      double p_147906_5_,
      double p_147906_7_,
      int p_147906_9_) {
    double d3 = p_147906_1_.getDistanceSqToEntity(RenderManager.instance.livingPlayer);

    if (d3 <= (double) (p_147906_9_ * p_147906_9_)) {
      FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
      float f = 1.6F;
      float f1 = 0.016666668F * f;
      setupRenderForNametag(
          p_147906_1_, (float) p_147906_3_, (float) p_147906_5_, (float) p_147906_7_, f1);
      GL11.glDepthMask(false);
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      OpenGlHelper.glBlendFunc(770, 771, 1, 0);
      Tessellator tessellator = Tessellator.instance;
      byte b0 = 0;
      GL11.glDisable(GL11.GL_TEXTURE_2D);
      tessellator.startDrawingQuads();
      int j = fontrenderer.getStringWidth(p_147906_2_) / 2;
      tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
      tessellator.addVertex((double) (-j - 1), (double) (-1 + b0), 0.0D);
      tessellator.addVertex((double) (-j - 1), (double) (8 + b0), 0.0D);
      tessellator.addVertex((double) (j + 1), (double) (8 + b0), 0.0D);
      tessellator.addVertex((double) (j + 1), (double) (-1 + b0), 0.0D);
      tessellator.draw();
      GL11.glEnable(GL11.GL_TEXTURE_2D);
      fontrenderer.drawString(
          p_147906_2_, -fontrenderer.getStringWidth(p_147906_2_) / 2, b0, 553648127);
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      GL11.glDepthMask(true);
      fontrenderer.drawString(p_147906_2_, -fontrenderer.getStringWidth(p_147906_2_) / 2, b0, -1);
      GL11.glEnable(GL11.GL_LIGHTING);
      GL11.glDisable(GL11.GL_BLEND);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
    }
  }

  private static void setupRenderForNametag(
      Entity p_147906_1_, float p_147906_3_, float p_147906_5_, float p_147906_7_, float f1) {
    GL11.glPushMatrix();
    GL11.glTranslatef(p_147906_3_ + 0.0F, p_147906_5_ + p_147906_1_.height + 0.5F, p_147906_7_);
    GL11.glNormal3f(0.0F, 1.0F, 0.0F);
    GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
    GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
    GL11.glScalef(-f1, -f1, f1);
    GL11.glDisable(GL11.GL_LIGHTING);
  }

  protected static boolean shouldShowName(EntityLivingBase entity) {
    boolean shouldShow =
        Minecraft.isGuiEnabled()
            && entity != RenderManager.instance.livingPlayer
            && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)
            && entity.riddenByEntity == null;
    if (entity instanceof EntityLiving) {
      EntityLiving entityLiving = ((EntityLiving) entity);
      return shouldShow
          && (entityLiving.getAlwaysRenderNameTagForRender()
              || entityLiving.hasCustomNameTag()
                  && entityLiving == RenderManager.instance.field_147941_i);
    }
    return shouldShow;
  }

  //	private static final MethodHandle dormantChunkCacheGet =
  // createDormantChunkCacheGet();
  //
  //	private static MethodHandle createDormantChunkCacheGet() {
  //		try {
  //			Field field2 =
  // ForgeChunkManager.class.getDeclaredField("dormantChunkCache");
  //			field2.setAccessible(true);
  //			return
  // MethodHandles.publicLookup().unreflectGetter(field2); 		} catch
  // (Exception e) { 			TEResetFix.logger.error("Cannot get
  // dormantChunkCache. The mod will not work properly and you should report
  // this as a bug.", e);
  //			return null;
  //		}
  //	}

  //	@Fix(insertOnExit=true)
  //	public static void fetchDormantChunk(ForgeChunkManager fcm, long coords,
  // World world)
  //	{
  //		try {
  //			Map<World, Cache<Long, Chunk>> dormantChunkCache = null;
  //			try {
  //				dormantChunkCache = (Map<World, Cache<Long,
  // Chunk>>) dormantChunkCacheGet.invokeExact(); 			} catch
  // (Throwable e) {
  //				TEResetFix.logger.error("Cannot invoke
  // dormantChunkCache! The mod will not work properly and
  // you should report this as a bug.", e);
  //			}
  //			Cache<Long, Chunk> cache = dormantChunkCache.get(world);
  //			if (cache == null) return;
  //			cache.invalidate(coords);
  //		} catch (Exception e) {
  //			TEResetFix.logger.error("Something went wrong. The mod
  // will not work properly and you should
  // report this as a bug.", e);
  //		}
  //	}

  // Particle culling patch for individual particle rendering
  @Fix(returnSetting = EnumReturnSetting.ON_TRUE)
  public static boolean func_70539_a(
      EntityFX particle,
      Tessellator tessellator,
      float partialTicks,
      float cosYaw,
      float cosPitch,
      float sinYaw,
      float sinSinPitch,
      float cosSinPitch) {
    // Check if particle culling is enabled
    if (!EntityCullingBase.enabled) {
      EntityCulling.instance.renderedParticles++;
      return false; // Continue normal rendering
    }

    // Get wrapper for this particle
    CullableParticleWrapper cullable = CullableEntityRegistry.getWrapper(particle);

    // Check if forced visible
    if (cullable.isForcedVisible()) {
      EntityCulling.instance.renderedParticles++;
      return false;
    }

    // Calculate visibility on-the-fly to avoid delay from CullTask updates
    try {
      Minecraft mc = Minecraft.getMinecraft();
      if (mc.thePlayer == null || mc.theWorld == null || EntityCulling.instance.culling == null) {
        EntityCulling.instance.renderedParticles++;
        return false;
      }

      // Skip culling if in spectator mode or no-clip
      if (mc.thePlayer.noClip || mc.gameSettings.thirdPersonView != 0) {
        EntityCulling.instance.renderedParticles++;
        return false;
      }

      // Get camera position
      Vec3 cameraPos = mc.renderViewEntity.getPosition(partialTicks);
      Vec3d camera = new Vec3d(cameraPos.xCoord, cameraPos.yCoord, cameraPos.zCoord);

      // Check distance limit
      double dx = particle.posX - cameraPos.xCoord;
      double dy = particle.posY - cameraPos.yCoord;
      double dz = particle.posZ - cameraPos.zCoord;
      double distanceSq = dx * dx + dy * dy + dz * dz;

      if (distanceSq > Config.tracingDistance * Config.tracingDistance) {
        EntityCulling.instance.renderedParticles++;
        return false; // Too far to bother culling
      }

      // Create bounding box for particle (slightly larger to avoid edge cases)
      double size = 0.3; // Slightly larger than CullTask to avoid leaks
      Vec3d aabbMin = new Vec3d(particle.posX - size, particle.posY - size, particle.posZ - size);
      Vec3d aabbMax = new Vec3d(particle.posX + size, particle.posY + size, particle.posZ + size);

      // Test visibility
      boolean visible = EntityCulling.instance.culling.isAABBVisible(aabbMin, aabbMax, camera);

      if (!visible) {
        // Update wrapper state for consistency with CullTask
        cullable.setCulled(true);
        EntityCulling.instance.skippedParticles++;
        return true; // Skip rendering
      }

      // Particle is visible
      cullable.setCulled(false);
      EntityCulling.instance.renderedParticles++;
      return false;

    } catch (Exception e) {
      // If anything goes wrong, default to rendering
      EntityCulling.instance.renderedParticles++;
      return false;
    }
  }
}
