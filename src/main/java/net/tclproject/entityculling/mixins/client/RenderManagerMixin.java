package net.tclproject.entityculling.mixins.client;

import static net.minecraft.client.renderer.entity.RendererLivingEntity.NAME_TAG_RANGE;
import static net.minecraft.client.renderer.entity.RendererLivingEntity.NAME_TAG_RANGE_SNEAK;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.tclproject.entityculling.EntityCulling;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class RenderManagerMixin {

    @Inject(method = "func_147939_a", at = @At("HEAD"), cancellable = true)
    private void entityculling$cullEntity(Entity entity, double x, double y, double z, float tickDelta,
        float partialTicks, boolean debug, CallbackInfoReturnable<Boolean> ci) {
        CullableEntityWrapper cullable = CullableEntityRegistry.getWrapper(entity);

        if (!cullable.isForcedVisible() && cullable.isCulled()) {
            RenderManager renderManager = (RenderManager) (Object) this;
            Render entityRenderer = renderManager.getEntityRenderObject(entity);

            if (entity instanceof EntityLivingBase) {
                if (Config.renderNametagsThroughWalls && shouldShowName((EntityLivingBase) entity)) {
                    renderNameTag((EntityLivingBase) entity, x, y, z);
                }
            }

            EntityCulling.instance.skippedEntities++;
            ci.setReturnValue(false);
            return;
        }

        EntityCulling.instance.renderedEntities++;
        cullable.setOutOfCamera(false);
    }

    private static void renderNameTag(EntityLivingBase entity, double x, double y, double z) {
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        double d3 = entity.getDistanceSqToEntity(RenderManager.instance.livingPlayer);
        float f2 = entity.isSneaking() ? NAME_TAG_RANGE_SNEAK : NAME_TAG_RANGE;
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);

        if (d3 < (double) (f2 * f2)) {
            String s = entity.func_145748_c_()
                .getFormattedText();

            if (entity.isSneaking()) {
                FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
                setupRenderForNametag(entity, (float) x, (float) y, (float) z, f1);
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
                func_96449_a(entity, x, y, z, s, f1, d3);
            }
        }
    }

    private static void func_96449_a(EntityLivingBase entity, double x, double y, double z, String text, float scale,
        double distanceSq) {
        if (entity.isPlayerSleeping()) {
            func_147906_a(entity, text, x, y - 1.5D, z, 64);
        } else {
            func_147906_a(entity, text, x, y, z, 64);
        }
    }

    private static void func_147906_a(Entity entity, String text, double x, double y, double z, int maxDistance) {
        double d3 = entity.getDistanceSqToEntity(RenderManager.instance.livingPlayer);

        if (d3 <= (double) (maxDistance * maxDistance)) {
            FontRenderer fontrenderer = RenderManager.instance.getFontRenderer();
            float f = 1.6F;
            float f1 = 0.016666668F * f;
            setupRenderForNametag(entity, (float) x, (float) y, (float) z, f1);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(770, 771, 1, 0);
            Tessellator tessellator = Tessellator.instance;
            byte b0 = 0;
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            tessellator.startDrawingQuads();
            int j = fontrenderer.getStringWidth(text) / 2;
            tessellator.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.25F);
            tessellator.addVertex((double) (-j - 1), (double) (-1 + b0), 0.0D);
            tessellator.addVertex((double) (-j - 1), (double) (8 + b0), 0.0D);
            tessellator.addVertex((double) (j + 1), (double) (8 + b0), 0.0D);
            tessellator.addVertex((double) (j + 1), (double) (-1 + b0), 0.0D);
            tessellator.draw();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            fontrenderer.drawString(text, -fontrenderer.getStringWidth(text) / 2, b0, 553648127);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            fontrenderer.drawString(text, -fontrenderer.getStringWidth(text) / 2, b0, -1);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    private static void setupRenderForNametag(Entity entity, float x, float y, float z, float scale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x + 0.0F, y + entity.height + 0.5F, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(RenderManager.instance.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-scale, -scale, scale);
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    private static boolean shouldShowName(EntityLivingBase entity) {
        boolean shouldShow = Minecraft.isGuiEnabled() && entity != RenderManager.instance.livingPlayer
            && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)
            && entity.riddenByEntity == null;
        if (entity instanceof EntityLiving) {
            EntityLiving entityLiving = ((EntityLiving) entity);
            return shouldShow && (entityLiving.getAlwaysRenderNameTagForRender()
                || entityLiving.hasCustomNameTag() && entityLiving == RenderManager.instance.field_147941_i);
        }
        return shouldShow;
    }
}
