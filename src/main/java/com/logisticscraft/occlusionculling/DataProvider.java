package com.logisticscraft.occlusionculling;

import com.logisticscraft.occlusionculling.util.Vec3d;

public interface DataProvider {
  public boolean prepareChunk(int var1, int var2);

  public boolean isOpaqueFullCube(int var1, int var2, int var3);

  public default void cleanup() {}

  public default void checkingPosition(Vec3d[] targetPoints, int size, Vec3d viewerPosition) {}
}
