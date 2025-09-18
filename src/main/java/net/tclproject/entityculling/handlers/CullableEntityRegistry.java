package net.tclproject.entityculling.handlers;

import com.google.common.collect.MapMaker;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;

public class CullableEntityRegistry {
  private static ConcurrentMap<TileEntity, CullableEntityWrapper> tileWrappers =
      new MapMaker().weakKeys().concurrencyLevel(3).makeMap();
  private static ConcurrentMap<Entity, CullableEntityWrapper> entityWrappers =
      new MapMaker().weakKeys().concurrencyLevel(3).makeMap();

  public static CullableEntityWrapper getWrapper(Entity e) {
    if (!entityWrappers.containsKey(e)) {
      entityWrappers.put(e, new CullableEntityWrapper(e));
    }
    return (CullableEntityWrapper) entityWrappers.get(e);
  }

  public static CullableEntityWrapper getWrapper(TileEntity e) {
    if (!tileWrappers.containsKey(e)) {
      tileWrappers.put(e, new CullableEntityWrapper(e));
    }
    return (CullableEntityWrapper) tileWrappers.get(e);
  }
}
