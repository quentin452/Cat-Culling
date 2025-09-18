package net.tclproject.mysteriumlib.asm.fixes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import org.lwjgl.opengl.GL11;

public class MysteriumPatchesFixesCulling {
  @Fix(returnSetting = EnumReturnSetting.ON_TRUE)
  public static boolean renderTileEntityAt(
      TileEntityRendererDispatcher tileEntityRendererDispatcher,
      TileEntity p_147549_1_,
      double p_147549_2_,
      double p_147549_4_,
      double p_147549_6_,
      float p_147549_8_) {
    CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(p_147549_1_);
    if (!cullable.isForcedVisible() && cullable.isCulled()) {
      ++EntityCulling.instance.skippedBlockEntities;
      return true;
    }
    ++EntityCulling.instance.renderedBlockEntities;
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
      if (entity instanceof EntityLivingBase
          && Config.renderNametagsThroughWalls
          && MysteriumPatchesFixesCulling.shouldShowName((EntityLivingBase) entity)) {
        MysteriumPatchesFixesCulling.renderNameTag(
            (EntityLivingBase) entity, p_doRenderEntity_2_, d1, d2);
      }
      ++EntityCulling.instance.skippedEntities;
      return true;
    }
    ++EntityCulling.instance.renderedEntities;
    cullable.setOutOfCamera(false);
    return false;
  }

  private static void renderNameTag(
      EntityLivingBase entity, double p_77033_2_, double p_77033_4_, double p_77033_6_) {
    float f = 1.6f;
    float f1 = 0.016666668f * f;
    double d3 = entity.getDistanceSqToEntity((Entity) RenderManager.instance.livingPlayer);
    float f2 =
        entity.isSneaking()
            ? RendererLivingEntity.NAME_TAG_RANGE_SNEAK
            : RendererLivingEntity.NAME_TAG_RANGE;
    GL11.glAlphaFunc((int) 516, (float) 0.1f);
    if (d3 < (double) (f2 * f2)) {
      String s = entity.func_145748_c_().getFormattedText();
      if (entity.isSneaking()) {
        FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
        MysteriumPatchesFixesCulling.setupRenderForNametag(
            (Entity) entity, (float) p_77033_2_, (float) p_77033_4_, (float) p_77033_6_, f1);
        GL11.glTranslatef((float) 0.0f, (float) (0.25f / f1), (float) 0.0f);
        GL11.glDepthMask((boolean) false);
        GL11.glEnable((int) 3042);
        OpenGlHelper.glBlendFunc((int) 770, (int) 771, (int) 1, (int) 0);
        Tessellator tessellator = Tessellator.instance;
        GL11.glDisable((int) 3553);
        tessellator.startDrawingQuads();
        int i = fontrenderer.getStringWidth(s) / 2;
        tessellator.setColorRGBA_F(0.0f, 0.0f, 0.0f, 0.25f);
        tessellator.addVertex((double) (-i - 1), -1.0, 0.0);
        tessellator.addVertex((double) (-i - 1), 8.0, 0.0);
        tessellator.addVertex((double) (i + 1), 8.0, 0.0);
        tessellator.addVertex((double) (i + 1), -1.0, 0.0);
        tessellator.draw();
        GL11.glEnable((int) 3553);
        GL11.glDepthMask((boolean) true);
        fontrenderer.drawString(s, -fontrenderer.getStringWidth(s) / 2, 0, 0x20FFFFFF);
        GL11.glEnable((int) 2896);
        GL11.glDisable((int) 3042);
        GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
        GL11.glPopMatrix();
      } else {
        MysteriumPatchesFixesCulling.func_96449_a(
            entity, p_77033_2_, p_77033_4_, p_77033_6_, s, f1, d3);
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
      MysteriumPatchesFixesCulling.func_147906_a(
          (Entity) p_96449_1_, p_96449_8_, p_96449_2_, p_96449_4_ - 1.5, p_96449_6_, 64);
    } else {
      MysteriumPatchesFixesCulling.func_147906_a(
          (Entity) p_96449_1_, p_96449_8_, p_96449_2_, p_96449_4_, p_96449_6_, 64);
    }
  }

  protected static void func_147906_a(
      Entity p_147906_1_,
      String p_147906_2_,
      double p_147906_3_,
      double p_147906_5_,
      double p_147906_7_,
      int p_147906_9_) {
    double d3 = p_147906_1_.getDistanceSqToEntity((Entity) RenderManager.instance.livingPlayer);
    if (d3 <= (double) (p_147906_9_ * p_147906_9_)) {
      FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
      float f = 1.6f;
      float f1 = 0.016666668f * f;
      MysteriumPatchesFixesCulling.setupRenderForNametag(
          p_147906_1_, (float) p_147906_3_, (float) p_147906_5_, (float) p_147906_7_, f1);
      GL11.glDepthMask((boolean) false);
      GL11.glDisable((int) 2929);
      GL11.glEnable((int) 3042);
      OpenGlHelper.glBlendFunc((int) 770, (int) 771, (int) 1, (int) 0);
      Tessellator tessellator = Tessellator.instance;
      int b0 = 0;
      GL11.glDisable((int) 3553);
      tessellator.startDrawingQuads();
      int j = fontrenderer.getStringWidth(p_147906_2_) / 2;
      tessellator.setColorRGBA_F(0.0f, 0.0f, 0.0f, 0.25f);
      tessellator.addVertex((double) (-j - 1), (double) (-1 + b0), 0.0);
      tessellator.addVertex((double) (-j - 1), (double) (8 + b0), 0.0);
      tessellator.addVertex((double) (j + 1), (double) (8 + b0), 0.0);
      tessellator.addVertex((double) (j + 1), (double) (-1 + b0), 0.0);
      tessellator.draw();
      GL11.glEnable((int) 3553);
      fontrenderer.drawString(
          p_147906_2_, -fontrenderer.getStringWidth(p_147906_2_) / 2, b0, 0x20FFFFFF);
      GL11.glEnable((int) 2929);
      GL11.glDepthMask((boolean) true);
      fontrenderer.drawString(p_147906_2_, -fontrenderer.getStringWidth(p_147906_2_) / 2, b0, -1);
      GL11.glEnable((int) 2896);
      GL11.glDisable((int) 3042);
      GL11.glColor4f((float) 1.0f, (float) 1.0f, (float) 1.0f, (float) 1.0f);
      GL11.glPopMatrix();
    }
  }

  private static void setupRenderForNametag(
      Entity p_147906_1_, float p_147906_3_, float p_147906_5_, float p_147906_7_, float f1) {
    GL11.glPushMatrix();
    GL11.glTranslatef(
        (float) (p_147906_3_ + 0.0f),
        (float) (p_147906_5_ + p_147906_1_.height + 0.5f),
        (float) p_147906_7_);
    GL11.glNormal3f((float) 0.0f, (float) 1.0f, (float) 0.0f);
    GL11.glRotatef(
        (float) (-RenderManager.instance.playerViewY), (float) 0.0f, (float) 1.0f, (float) 0.0f);
    GL11.glRotatef(
        (float) RenderManager.instance.playerViewX, (float) 1.0f, (float) 0.0f, (float) 0.0f);
    GL11.glScalef((float) (-f1), (float) (-f1), (float) f1);
    GL11.glDisable((int) 2896);
  }

  protected static boolean shouldShowName(EntityLivingBase entity) {
    boolean shouldShow;
    boolean bl =
        shouldShow =
            Minecraft.isGuiEnabled()
                && entity != RenderManager.instance.livingPlayer
                && !entity.isInvisibleToPlayer((EntityPlayer) Minecraft.getMinecraft().thePlayer)
                && entity.riddenByEntity == null;
    if (entity instanceof EntityLiving) {
      EntityLiving entityLiving = (EntityLiving) entity;
      return shouldShow
          && (entityLiving.getAlwaysRenderNameTagForRender()
              || entityLiving.hasCustomNameTag()
                  && entityLiving == RenderManager.instance.field_147941_i);
    }
    return shouldShow;
  }
}
