package net.tclproject.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.tclproject.entityculling.handlers.Config;

public abstract class EntityCullingBase {
  public OcclusionCullingInstance culling;
  public boolean debugHitboxes = false;
  public static boolean enabled = true;
  public CullTask cullTask;
  private Thread cullThread;
  protected KeyBinding keybind = new KeyBinding("key.entityculling.toggle", 0, "EntityCulling");
  protected boolean pressed = false;
  public int renderedBlockEntities = 0;
  public int skippedBlockEntities = 0;
  public int renderedEntities = 0;
  public int skippedEntities = 0;

  public void onInitialize() {
    this.culling =
        Config.aggressiveMode
            ? new OcclusionCullingInstance(
                Config.tracingDistance,
                new Provider(),
                new ArrayOcclusionCache(Config.tracingDistance),
                0.0)
            : new OcclusionCullingInstance(Config.tracingDistance, new Provider());
    this.cullTask = new CullTask(this.culling, Config.blockEntityWhitelist);
    this.cullThread = new Thread((Runnable) this.cullTask, "CullThread");
    this.cullThread.setUncaughtExceptionHandler(
        (thread, ex) -> {
          System.out.println(
              "The CullingThread has crashed! Please report the following stacktrace!");
          ex.printStackTrace();
        });
    this.cullThread.start();
    this.initModloader();
  }

  public void worldTick() {
    this.cullTask.requestCull = true;
  }

  public void clientTick() {
    if (this.keybind.isPressed()) {
      if (this.pressed) {
        return;
      }
      this.pressed = true;
      enabled = !enabled;
      EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
      if (enabled) {
        if (player != null) {
          player.addChatMessage((IChatComponent) new ChatComponentText("Culling on"));
          EntityCulling.instance.renderedBlockEntities = 0;
          EntityCulling.instance.skippedBlockEntities = 0;
          EntityCulling.instance.renderedEntities = 0;
          EntityCulling.instance.skippedEntities = 0;
        }
      } else if (player != null) {
        player.addChatMessage(
            (IChatComponent)
                new ChatComponentText(
                    "[Culling] Last pass: " + EntityCulling.instance.cullTask.lastTime + "ms"));
        player.addChatMessage(
            (IChatComponent)
                new ChatComponentText(
                    "[Culling] Rendered Block Entities: "
                        + EntityCulling.instance.renderedBlockEntities
                        + " Skipped: "
                        + EntityCulling.instance.skippedBlockEntities));
        player.addChatMessage(
            (IChatComponent)
                new ChatComponentText(
                    "[Culling] Rendered Entities: "
                        + EntityCulling.instance.renderedEntities
                        + " Skipped: "
                        + EntityCulling.instance.skippedEntities));
        player.addChatMessage((IChatComponent) new ChatComponentText("Culling off"));
      }
    } else {
      this.pressed = false;
    }
    this.cullTask.requestCull = true;
  }

  public abstract void initModloader();
}
