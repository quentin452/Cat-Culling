package com.logisticscraft.occlusionculling;

import com.logisticscraft.occlusionculling.cache.ArrayOcclusionCache;
import com.logisticscraft.occlusionculling.cache.OcclusionCache;
import com.logisticscraft.occlusionculling.util.MathUtilities;
import com.logisticscraft.occlusionculling.util.Vec3d;
import java.util.Arrays;
import java.util.BitSet;

public class OcclusionCullingInstance {
  private static final int ON_MIN_X = 1;
  private static final int ON_MAX_X = 2;
  private static final int ON_MIN_Y = 4;
  private static final int ON_MAX_Y = 8;
  private static final int ON_MIN_Z = 16;
  private static final int ON_MAX_Z = 32;
  private final int reach;
  private final double aabbExpansion;
  private final DataProvider provider;
  private final OcclusionCache cache;
  private final BitSet skipList = new BitSet();
  private final Vec3d[] targetPoints = new Vec3d[15];
  private final Vec3d targetPos = new Vec3d(0.0, 0.0, 0.0);
  private final int[] cameraPos = new int[3];
  private final boolean[] dotselectors = new boolean[14];
  private boolean allowRayChecks = false;
  private final int[] lastHitBlock = new int[3];

  public OcclusionCullingInstance(int maxDistance, DataProvider provider) {
    this(maxDistance, provider, new ArrayOcclusionCache(maxDistance), 0.5);
  }

  public OcclusionCullingInstance(
      int maxDistance, DataProvider provider, OcclusionCache cache, double aabbExpansion) {
    this.reach = maxDistance;
    this.provider = provider;
    this.cache = cache;
    this.aabbExpansion = aabbExpansion;
    for (int i = 0; i < this.targetPoints.length; ++i) {
      this.targetPoints[i] = new Vec3d(0.0, 0.0, 0.0);
    }
  }

  public boolean isAABBVisible(Vec3d aabbMin, Vec3d aabbMax, Vec3d viewerPosition) {
    try {
      int x;
      int maxX = MathUtilities.floor(aabbMax.x + this.aabbExpansion);
      int maxY = MathUtilities.floor(aabbMax.y + this.aabbExpansion);
      int maxZ = MathUtilities.floor(aabbMax.z + this.aabbExpansion);
      int minX = MathUtilities.floor(aabbMin.x - this.aabbExpansion);
      int minY = MathUtilities.floor(aabbMin.y - this.aabbExpansion);
      int minZ = MathUtilities.floor(aabbMin.z - this.aabbExpansion);
      this.cameraPos[0] = MathUtilities.floor(viewerPosition.x);
      this.cameraPos[1] = MathUtilities.floor(viewerPosition.y);
      this.cameraPos[2] = MathUtilities.floor(viewerPosition.z);
      Relative relX = Relative.from(minX, maxX, this.cameraPos[0]);
      Relative relY = Relative.from(minY, maxY, this.cameraPos[1]);
      Relative relZ = Relative.from(minZ, maxZ, this.cameraPos[2]);
      if (relX == Relative.INSIDE && relY == Relative.INSIDE && relZ == Relative.INSIDE) {
        return true;
      }
      this.skipList.clear();
      int id = 0;
      for (x = minX; x <= maxX; ++x) {
        for (int y = minY; y <= maxY; ++y) {
          for (int z = minZ; z <= maxZ; ++z) {
            int cachedValue = this.getCacheValue(x, y, z);
            if (cachedValue == 1) {
              return true;
            }
            if (cachedValue != 0) {
              this.skipList.set(id);
            }
            ++id;
          }
        }
      }
      this.allowRayChecks = false;
      id = 0;
      for (x = minX; x <= maxX; ++x) {
        byte visibleOnFaceX = 0;
        byte faceEdgeDataX = 0;
        faceEdgeDataX = (byte) (faceEdgeDataX | (x == minX ? 1 : 0));
        faceEdgeDataX = (byte) (faceEdgeDataX | (x == maxX ? 2 : 0));
        visibleOnFaceX = (byte) (visibleOnFaceX | (x == minX && relX == Relative.POSITIVE ? 1 : 0));
        visibleOnFaceX = (byte) (visibleOnFaceX | (x == maxX && relX == Relative.NEGATIVE ? 2 : 0));
        for (int y = minY; y <= maxY; ++y) {
          byte faceEdgeDataY = faceEdgeDataX;
          byte visibleOnFaceY = visibleOnFaceX;
          faceEdgeDataY = (byte) (faceEdgeDataY | (y == minY ? 4 : 0));
          faceEdgeDataY = (byte) (faceEdgeDataY | (y == maxY ? 8 : 0));
          visibleOnFaceY =
              (byte) (visibleOnFaceY | (y == minY && relY == Relative.POSITIVE ? 4 : 0));
          visibleOnFaceY =
              (byte) (visibleOnFaceY | (y == maxY && relY == Relative.NEGATIVE ? 8 : 0));
          for (int z = minZ; z <= maxZ; ++z) {
            byte faceEdgeData = faceEdgeDataY;
            byte visibleOnFace = visibleOnFaceY;
            faceEdgeData = (byte) (faceEdgeData | (z == minZ ? 16 : 0));
            faceEdgeData = (byte) (faceEdgeData | (z == maxZ ? 32 : 0));
            visibleOnFace =
                (byte) (visibleOnFace | (z == minZ && relZ == Relative.POSITIVE ? 16 : 0));
            visibleOnFace =
                (byte) (visibleOnFace | (z == maxZ && relZ == Relative.NEGATIVE ? 32 : 0));
            if (this.skipList.get(id)) {
              ++id;
              continue;
            }
            if (visibleOnFace != 0) {
              this.targetPos.set(x, y, z);
              if (this.isVoxelVisible(
                  viewerPosition, this.targetPos, faceEdgeData, visibleOnFace)) {
                return true;
              }
            }
            ++id;
          }
        }
      }
      return false;
    } catch (Throwable t) {
      t.printStackTrace();
      return true;
    }
  }

  private boolean isVoxelVisible(
      Vec3d viewerPosition, Vec3d position, byte faceData, byte visibleOnFace) {
    int targetSize = 0;
    Arrays.fill(this.dotselectors, false);
    if ((visibleOnFace & 1) == 1) {
      this.dotselectors[0] = true;
      if ((faceData & 0xFFFFFFFE) != 0) {
        this.dotselectors[1] = true;
        this.dotselectors[4] = true;
        this.dotselectors[5] = true;
      }
      this.dotselectors[8] = true;
    }
    if ((visibleOnFace & 4) == 4) {
      this.dotselectors[0] = true;
      if ((faceData & 0xFFFFFFFB) != 0) {
        this.dotselectors[3] = true;
        this.dotselectors[4] = true;
        this.dotselectors[7] = true;
      }
      this.dotselectors[9] = true;
    }
    if ((visibleOnFace & 0x10) == 16) {
      this.dotselectors[0] = true;
      if ((faceData & 0xFFFFFFEF) != 0) {
        this.dotselectors[1] = true;
        this.dotselectors[4] = true;
        this.dotselectors[5] = true;
      }
      this.dotselectors[10] = true;
    }
    if ((visibleOnFace & 2) == 2) {
      this.dotselectors[4] = true;
      if ((faceData & 0xFFFFFFFD) != 0) {
        this.dotselectors[5] = true;
        this.dotselectors[6] = true;
        this.dotselectors[7] = true;
      }
      this.dotselectors[11] = true;
    }
    if ((visibleOnFace & 8) == 8) {
      this.dotselectors[1] = true;
      if ((faceData & 0xFFFFFFF7) != 0) {
        this.dotselectors[2] = true;
        this.dotselectors[5] = true;
        this.dotselectors[6] = true;
      }
      this.dotselectors[12] = true;
    }
    if ((visibleOnFace & 0x20) == 32) {
      this.dotselectors[2] = true;
      if ((faceData & 0xFFFFFFDF) != 0) {
        this.dotselectors[3] = true;
        this.dotselectors[6] = true;
        this.dotselectors[7] = true;
      }
      this.dotselectors[13] = true;
    }
    if (this.dotselectors[0]) {
      this.targetPoints[targetSize++].setAdd(position, 0.05, 0.05, 0.05);
    }
    if (this.dotselectors[1]) {
      this.targetPoints[targetSize++].setAdd(position, 0.05, 0.95, 0.05);
    }
    if (this.dotselectors[2]) {
      this.targetPoints[targetSize++].setAdd(position, 0.05, 0.95, 0.95);
    }
    if (this.dotselectors[3]) {
      this.targetPoints[targetSize++].setAdd(position, 0.05, 0.05, 0.95);
    }
    if (this.dotselectors[4]) {
      this.targetPoints[targetSize++].setAdd(position, 0.95, 0.05, 0.05);
    }
    if (this.dotselectors[5]) {
      this.targetPoints[targetSize++].setAdd(position, 0.95, 0.95, 0.05);
    }
    if (this.dotselectors[6]) {
      this.targetPoints[targetSize++].setAdd(position, 0.95, 0.95, 0.95);
    }
    if (this.dotselectors[7]) {
      this.targetPoints[targetSize++].setAdd(position, 0.95, 0.05, 0.95);
    }
    if (this.dotselectors[8]) {
      this.targetPoints[targetSize++].setAdd(position, 0.05, 0.5, 0.5);
    }
    if (this.dotselectors[9]) {
      this.targetPoints[targetSize++].setAdd(position, 0.5, 0.05, 0.5);
    }
    if (this.dotselectors[10]) {
      this.targetPoints[targetSize++].setAdd(position, 0.5, 0.5, 0.05);
    }
    if (this.dotselectors[11]) {
      this.targetPoints[targetSize++].setAdd(position, 0.95, 0.5, 0.5);
    }
    if (this.dotselectors[12]) {
      this.targetPoints[targetSize++].setAdd(position, 0.5, 0.95, 0.5);
    }
    if (this.dotselectors[13]) {
      this.targetPoints[targetSize++].setAdd(position, 0.5, 0.5, 0.95);
    }
    return this.isVisible(viewerPosition, this.targetPoints, targetSize);
  }

  private boolean rayIntersection(int[] b, Vec3d rayOrigin, Vec3d rayDir) {
    Vec3d rInv = new Vec3d(1.0, 1.0, 1.0).div(rayDir);
    double t1 = ((double) b[0] - rayOrigin.x) * rInv.x;
    double t2 = ((double) (b[0] + 1) - rayOrigin.x) * rInv.x;
    double t3 = ((double) b[1] - rayOrigin.y) * rInv.y;
    double t4 = ((double) (b[1] + 1) - rayOrigin.y) * rInv.y;
    double t5 = ((double) b[2] - rayOrigin.z) * rInv.z;
    double t6 = ((double) (b[2] + 1) - rayOrigin.z) * rInv.z;
    double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
    double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));
    if (tmax > 0.0) {
      return false;
    }
    return !(tmin > tmax);
  }

  private boolean isVisible(Vec3d start, Vec3d[] targets, int size) {
    int x = this.cameraPos[0];
    int y = this.cameraPos[1];
    int z = this.cameraPos[2];
    for (int v = 0; v < size; ++v) {
      double t_next_z;
      int z_inc;
      double t_next_y;
      int y_inc;
      double t_next_x;
      int x_inc;
      Vec3d target = targets[v];
      double relativeX = start.x - target.getX();
      double relativeY = start.y - target.getY();
      double relativeZ = start.z - target.getZ();
      if (this.allowRayChecks
          && this.rayIntersection(
              this.lastHitBlock, start, new Vec3d(relativeX, relativeY, relativeZ).normalize()))
        continue;
      double dimensionX = Math.abs(relativeX);
      double dimensionY = Math.abs(relativeY);
      double dimensionZ = Math.abs(relativeZ);
      double dimFracX = 1.0 / dimensionX;
      double dimFracY = 1.0 / dimensionY;
      double dimFracZ = 1.0 / dimensionZ;
      int intersectCount = 1;
      if (dimensionX == 0.0) {
        x_inc = 0;
        t_next_x = dimFracX;
      } else if (target.x > start.x) {
        x_inc = 1;
        intersectCount += MathUtilities.floor(target.x) - x;
        t_next_x = (float) (((double) (x + 1) - start.x) * dimFracX);
      } else {
        x_inc = -1;
        intersectCount += x - MathUtilities.floor(target.x);
        t_next_x = (float) ((start.x - (double) x) * dimFracX);
      }
      if (dimensionY == 0.0) {
        y_inc = 0;
        t_next_y = dimFracY;
      } else if (target.y > start.y) {
        y_inc = 1;
        intersectCount += MathUtilities.floor(target.y) - y;
        t_next_y = (float) (((double) (y + 1) - start.y) * dimFracY);
      } else {
        y_inc = -1;
        intersectCount += y - MathUtilities.floor(target.y);
        t_next_y = (float) ((start.y - (double) y) * dimFracY);
      }
      if (dimensionZ == 0.0) {
        z_inc = 0;
        t_next_z = dimFracZ;
      } else if (target.z > start.z) {
        z_inc = 1;
        intersectCount += MathUtilities.floor(target.z) - z;
        t_next_z = (float) (((double) (z + 1) - start.z) * dimFracZ);
      } else {
        z_inc = -1;
        intersectCount += z - MathUtilities.floor(target.z);
        t_next_z = (float) ((start.z - (double) z) * dimFracZ);
      }
      boolean finished =
          this.stepRay(
              start,
              x,
              y,
              z,
              dimFracX,
              dimFracY,
              dimFracZ,
              intersectCount,
              x_inc,
              y_inc,
              z_inc,
              t_next_y,
              t_next_x,
              t_next_z);
      this.provider.cleanup();
      if (finished) {
        this.cacheResult(targets[0], true);
        return true;
      }
      this.allowRayChecks = true;
    }
    this.cacheResult(targets[0], false);
    return false;
  }

  private boolean stepRay(
      Vec3d start,
      int currentX,
      int currentY,
      int currentZ,
      double distInX,
      double distInY,
      double distInZ,
      int n,
      int x_inc,
      int y_inc,
      int z_inc,
      double t_next_y,
      double t_next_x,
      double t_next_z) {
    while (n > 1) {
      int cVal = this.getCacheValue(currentX, currentY, currentZ);
      if (cVal == 2) {
        this.lastHitBlock[0] = currentX;
        this.lastHitBlock[1] = currentY;
        this.lastHitBlock[2] = currentZ;
        return false;
      }
      if (cVal == 0) {
        int chunkX = currentX >> 4;
        int chunkZ = currentZ >> 4;
        if (!this.provider.prepareChunk(chunkX, chunkZ)) {
          return false;
        }
        if (this.provider.isOpaqueFullCube(currentX, currentY, currentZ)) {
          this.cache.setLastHidden();
          this.lastHitBlock[0] = currentX;
          this.lastHitBlock[1] = currentY;
          this.lastHitBlock[2] = currentZ;
          return false;
        }
        this.cache.setLastVisible();
      }
      if (t_next_y < t_next_x && t_next_y < t_next_z) {
        currentY += y_inc;
        t_next_y += distInY;
      } else if (t_next_x < t_next_y && t_next_x < t_next_z) {
        currentX += x_inc;
        t_next_x += distInX;
      } else {
        currentZ += z_inc;
        t_next_z += distInZ;
      }
      --n;
    }
    return true;
  }

  private int getCacheValue(int x, int y, int z) {
    if (Math.abs(x -= this.cameraPos[0]) > this.reach - 2
        || Math.abs(y -= this.cameraPos[1]) > this.reach - 2
        || Math.abs(z -= this.cameraPos[2]) > this.reach - 2) {
      return -1;
    }
    return this.cache.getState(x + this.reach, y + this.reach, z + this.reach);
  }

  private void cacheResult(int x, int y, int z, boolean result) {
    int cx = x - this.cameraPos[0] + this.reach;
    int cy = y - this.cameraPos[1] + this.reach;
    int cz = z - this.cameraPos[2] + this.reach;
    if (result) {
      this.cache.setVisible(cx, cy, cz);
    } else {
      this.cache.setHidden(cx, cy, cz);
    }
  }

  private void cacheResult(Vec3d vector, boolean result) {
    int cx = MathUtilities.floor(vector.x) - this.cameraPos[0] + this.reach;
    int cy = MathUtilities.floor(vector.y) - this.cameraPos[1] + this.reach;
    int cz = MathUtilities.floor(vector.z) - this.cameraPos[2] + this.reach;
    if (result) {
      this.cache.setVisible(cx, cy, cz);
    } else {
      this.cache.setHidden(cx, cy, cz);
    }
  }

  public void resetCache() {
    this.cache.resetCache();
  }

  private static enum Relative {
    INSIDE,
    POSITIVE,
    NEGATIVE;

    public static Relative from(int min, int max, int pos) {
      if (max > pos && min > pos) {
        return POSITIVE;
      }
      if (min < pos && max < pos) {
        return NEGATIVE;
      }
      return INSIDE;
    }
  }
}
