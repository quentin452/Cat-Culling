package net.tclproject.entityculling;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.tclproject.entityculling.handlers.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = EntityCulling.MODID,
    version = EntityCulling.VERSION,
    name = EntityCulling.NAME,
    acceptableRemoteVersions = "*")
public class EntityCulling extends EntityCullingBase {
  public static final String MODID = "entityculling";
  public static final String NAME = "Entity Culling Unofficial";
  public static final String VERSION = "1.0.1";

  @Mod.Instance("entityculling")
  public static EntityCulling instance;

  private boolean onServer = false;

  public static final Logger logger = LogManager.getLogger("EntityCulling");

  @Override
  public void initModloader() {}

  @EventHandler
  public void onPostInit(FMLPostInitializationEvent event) {
    ClientRegistry.registerKeyBinding(keybind);
    MinecraftForge.EVENT_BUS.register(this);
    FMLCommonHandler.instance().bus().register(this);
  }

  @SubscribeEvent
  public void doClientTick(TickEvent.ClientTickEvent event) {
    this.clientTick();
  }

  @SubscribeEvent
  public void doWorldTick(TickEvent.WorldTickEvent event) {
    this.worldTick();
  }

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    Config.load(event);
    try {
      Class clientClass = net.minecraft.client.Minecraft.class;
    } catch (Throwable ex) {
      System.out.println("EntityCulling Mod installed on a Server. Going to sleep.");
      onServer = true;
      return;
    }
    onInitialize();
  }
}
