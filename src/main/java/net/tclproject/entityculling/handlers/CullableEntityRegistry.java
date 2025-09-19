package net.tclproject.entityculling.handlers;

import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public class CullableEntityRegistry {
  //    public static Map<Entity, CullableEntityWrapper> entityWrappers = new HashMap<>();
  //    public static Map<TileEntity, CullableEntityWrapper> tileWrappers = new HashMap<>();
  private static ConcurrentMap<TileEntity, CullableEntityWrapper> tileWrappers =
      new MapMaker().weakKeys().weakValues().concurrencyLevel(3).makeMap();
  private static ConcurrentMap<Entity, CullableEntityWrapper> entityWrappers =
      new MapMaker().weakKeys().weakValues().concurrencyLevel(3).makeMap();

  public static CullableEntityWrapper getWrapper(Entity e) {
    if (!entityWrappers.containsKey(e)) entityWrappers.put(e, new CullableEntityWrapper(e));
    return entityWrappers.get(e);
  }

  public static CullableEntityWrapper getWrapper(TileEntity e) {
    if (!tileWrappers.containsKey(e)) tileWrappers.put(e, new CullableEntityWrapper(e));
    return tileWrappers.get(e);
  }

  //    public static void cleanupWrappers() { // test if weak keys don't work properly
  //        entityWrappers.keySet().removeIf(v ->
  // !Minecraft.getMinecraft().theWorld.loadedEntityList.contains(v));
  //        tileWrappers.keySet().removeIf(v ->
  // !Minecraft.getMinecraft().theWorld.loadedTileEntityList.contains(v));
  //    }


  /** Clear all wrapper caches to prevent classloader/world retention on disconnect. */
  public static void clear() {
    try { tileWrappers.clear(); } catch (Throwable ignored) {}
    try { entityWrappers.clear(); } catch (Throwable ignored) {}
  }
}
