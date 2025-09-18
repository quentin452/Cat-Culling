package net.tclproject.entityculling;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.tclproject.entityculling.handlers.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = "entityculling",
    version = "1.0.1",
    name = "Entity Culling Unofficial",
    acceptableRemoteVersions = "*")
public class EntityCulling extends EntityCullingBase {
  public static final String MODID = "entityculling";
  public static final String NAME = "Entity Culling Unofficial";
  public static final String VERSION = "1.0.1";

  @Mod.Instance(value = "entityculling")
  public static EntityCulling instance;

  private boolean onServer = false;
  public static final Logger logger;

  @Override
  public void initModloader() {}

  @Mod.EventHandler
  public void onPostInit(FMLPostInitializationEvent event) {
    ClientRegistry.registerKeyBinding((KeyBinding) this.keybind);
    MinecraftForge.EVENT_BUS.register((Object) this);
    FMLCommonHandler.instance().bus().register((Object) this);
  }

  @SubscribeEvent
  public void doClientTick(TickEvent.ClientTickEvent event) {
    this.clientTick();
  }

  @SubscribeEvent
  public void doWorldTick(TickEvent.WorldTickEvent event) {
    this.worldTick();
  }

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    Config.load(event);
    try {
      Class<Minecraft> clazz = Minecraft.class;
    } catch (Throwable ex) {
      System.out.println("EntityCulling Mod installed on a Server. Going to sleep.");
      this.onServer = true;
      return;
    }
    this.onInitialize();
  }

  static {
    logger = LogManager.getLogger("EntityCulling");
  }
}
