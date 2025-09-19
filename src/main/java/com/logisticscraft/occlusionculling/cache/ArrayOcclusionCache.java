package com.logisticscraft.occlusionculling.cache;

import java.util.Arrays;

public class ArrayOcclusionCache implements OcclusionCache {

  private final int reachX2;
  private final byte[] cache;
  private int positionKey;
  private int entry;
  private int offset;

  public ArrayOcclusionCache(int reach) {
    this.reachX2 = reach * 2;
    this.cache = new byte[(reachX2 * reachX2 * reachX2) / 8];
  }

  @Override
  public void resetCache() {
    Arrays.fill(cache, (byte) 0);
  }

  @Override
  public void setVisible(int x, int y, int z) {
    positionKey = x + y * reachX2 + z * reachX2 * reachX2;
    entry = positionKey / 8;
    offset = positionKey % 8;
    cache[entry] |= 1 << offset;
  }

  @Override
  public void setHidden(int x, int y, int z) {
    // With 1-bit cache, we don't cache hidden state to save memory
    // Hidden blocks will remain as unchecked (0) and be re-evaluated as needed
  }

  @Override
  public int getState(int x, int y, int z) {
    positionKey = x + y * reachX2 + z * reachX2 * reachX2;
    entry = positionKey / 8;
    offset = positionKey % 8;
    return (cache[entry] >> offset) & 1;
  }

  @Override
  public void setLastVisible() {
    cache[entry] |= 1 << offset;
  }

  @Override
  public void setLastHidden() {
    // With 1-bit cache, we don't cache hidden state to save memory
    // Hidden blocks will remain as unchecked (0) and be re-evaluated as needed
  }
}
