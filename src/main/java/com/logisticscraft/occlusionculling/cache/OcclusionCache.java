package com.logisticscraft.occlusionculling.cache;

public interface OcclusionCache {
  public void resetCache();

  public void setVisible(int var1, int var2, int var3);

  public void setHidden(int var1, int var2, int var3);

  public int getState(int var1, int var2, int var3);

  public void setLastHidden();

  public void setLastVisible();
}
