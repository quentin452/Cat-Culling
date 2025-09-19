package net.tclproject.entityculling.handlers;

import com.google.common.collect.MapMaker;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public class CullableEntityRegistry {
  //    public static Map<Entity, CullableEntityWrapper> entityWrappers = new
  //    HashMap<>(); public static Map<TileEntity, CullableEntityWrapper>
  //    tileWrappers = new HashMap<>();

  private static ConcurrentMap<TileEntity, WeakReference<CullableEntityWrapper>>
      tileWrappers = new MapMaker().weakKeys().concurrencyLevel(3).makeMap();
  private static ConcurrentMap<Entity, WeakReference<CullableEntityWrapper>>
      entityWrappers = new MapMaker().weakKeys().concurrencyLevel(3).makeMap();

  public static CullableEntityWrapper getWrapper(Entity e) {
    WeakReference<CullableEntityWrapper> ref =
        entityWrappers.computeIfAbsent(e, entity -> {
          CullableEntityWrapper wrapper = new CullableEntityWrapper(entity);
          return new WeakReference<>(wrapper);
        });
    CullableEntityWrapper wrapper = ref.get();
    if (wrapper == null) {
      // The reference was collected, create a new wrapper
      wrapper = new CullableEntityWrapper(e);
      entityWrappers.put(e, new WeakReference<>(wrapper));
    }
    return wrapper;
  }

  public static CullableEntityWrapper getWrapper(TileEntity e) {
    WeakReference<CullableEntityWrapper> ref =
        tileWrappers.computeIfAbsent(e, tileEntity -> {
          CullableEntityWrapper wrapper = new CullableEntityWrapper(tileEntity);
          return new WeakReference<>(wrapper);
        });
    CullableEntityWrapper wrapper = ref.get();
    if (wrapper == null) {
      // The reference was collected, create a new wrapper
      wrapper = new CullableEntityWrapper(e);
      tileWrappers.put(e, new WeakReference<>(wrapper));
    }
    return wrapper;
  }

  // Manual cleanup method for cases where weak references don't work properly
  public static void cleanupWrappers() {
    entityWrappers.clear();
    tileWrappers.clear();
  }

  // Get current map sizes for monitoring
  public static int getEntityWrappersCount() { return entityWrappers.size(); }

  public static int getTileWrappersCount() { return tileWrappers.size(); }

  //    public static void cleanupWrappers() { // test if weak keys don't work
  //    properly
  //        entityWrappers.keySet().removeIf(v ->
  // !Minecraft.getMinecraft().theWorld.loadedEntityList.contains(v));
  //        tileWrappers.keySet().removeIf(v ->
  // !Minecraft.getMinecraft().theWorld.loadedTileEntityList.contains(v));
  //    }
}
