package com.logisticscraft.occlusionculling.cache;

/**
 * A no-operation occlusion cache that doesn't store any data, used to reduce memory usage.
 * This implementation always returns 0 (not checked) for getState, making the system fall back
 * to direct visibility checks.
 */
public class NoOpOcclusionCache implements OcclusionCache {

  @Override
  public void resetCache() {
    // No-op
  }

  @Override
  public void setVisible(int x, int y, int z) {
    // No-op
  }

  @Override
  public void setHidden(int x, int y, int z) {
    // No-op
  }

  @Override
  public int getState(int x, int y, int z) {
    return 0; // Always return "not checked" to force fresh checks
  }

  @Override
  public void setLastHidden() {
    // No-op
  }

  @Override
  public void setLastVisible() {
    // No-op
  }
}