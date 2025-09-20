package fr.iamacat.catculling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.cache.NoOpOcclusionCache;

import fr.iamacat.catculling.handlers.Config;

public abstract class EntityCullingBase {

    public OcclusionCullingInstance culling;
    public boolean debugHitboxes = false;
    public static boolean enabled = true; // public static to make it faster for the jvm
    public CullTask cullTask;
    private Thread cullThread;
    protected KeyBinding keybind = new KeyBinding("key.catculling.toggle", 0, "CatCullingBase");
    protected boolean pressed = false;

    // stats
    public int renderedBlockEntities = 0;
    public int skippedBlockEntities = 0;
    public int renderedEntities = 0;
    public int skippedEntities = 0;
    public int renderedParticles = 0;
    public int skippedParticles = 0;
    // public int tickedEntities = 0;
    // public int skippedEntityTicks = 0;

    public void onInitialize() {
        if (Config.aggressiveMode) {
            culling = new OcclusionCullingInstance(Config.tracingDistance, new Provider(), new NoOpOcclusionCache(), 0);
        } else {
            culling = new OcclusionCullingInstance(Config.tracingDistance, new Provider());
        }
        cullTask = new CullTask(culling, Config.blockEntityWhitelist);

        cullThread = new Thread(cullTask, "CullThread");
        cullThread.setUncaughtExceptionHandler((thread, ex) -> {
            System.out.println("The CullingThread has crashed! Please report the " + "following stacktrace!");
            ex.printStackTrace();
        });
        cullThread.start();
        initModloader();
    }

    public void worldTick() {
        cullTask.requestCull = true;
    }

    public void clientTick() {
        if (keybind.isPressed()) {
            if (pressed) return;
            pressed = true;
            enabled = !enabled;
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            if (enabled) {
                if (player != null) {
                    // list.add("[Culling] Ticked Entities: " + lastTickedEntities + "
                    // Skipped: " + lastSkippedEntityTicks);
                    player.addChatMessage(new ChatComponentText("Culling on"));
                    CatCullingBase.instance.renderedBlockEntities = 0;
                    CatCullingBase.instance.skippedBlockEntities = 0;
                    CatCullingBase.instance.renderedEntities = 0;
                    CatCullingBase.instance.skippedEntities = 0;
                    CatCullingBase.instance.renderedParticles = 0;
                    CatCullingBase.instance.skippedParticles = 0;
                }
            } else {
                if (player != null) {
                    player.addChatMessage(
                        new ChatComponentText(
                            "[Culling] Last pass: " + CatCullingBase.instance.cullTask.lastTime + "ms"));
                    player.addChatMessage(
                        new ChatComponentText(
                            "[Culling] Rendered Block Entities: " + CatCullingBase.instance.renderedBlockEntities
                                + " Skipped: "
                                + CatCullingBase.instance.skippedBlockEntities));
                    player.addChatMessage(
                        new ChatComponentText(
                            "[Culling] Rendered Entities: " + CatCullingBase.instance.renderedEntities
                                + " Skipped: "
                                + CatCullingBase.instance.skippedEntities));
                    player.addChatMessage(
                        new ChatComponentText(
                            "[Culling] Rendered Particles: " + CatCullingBase.instance.renderedParticles
                                + " Skipped: "
                                + CatCullingBase.instance.skippedParticles));

                    player.addChatMessage(new ChatComponentText("Culling off"));
                }
            }
        } else {
            pressed = false;
        }
        cullTask.requestCull = true;
    }

    public abstract void initModloader();
}
