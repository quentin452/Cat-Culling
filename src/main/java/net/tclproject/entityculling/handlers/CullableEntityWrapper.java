package net.tclproject.entityculling.handlers;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.tclproject.entityculling.EntityCullingBase;

public class CullableEntityWrapper {

  private long lasttime = 0;
  private boolean culled = false;
  private boolean outOfCamera = false;

  public Entity entity;
  public TileEntity tileEntity;

  public CullableEntityWrapper(Entity e) {
    entity = e;
    tileEntity = null;
  }

  public CullableEntityWrapper(TileEntity e) {
    tileEntity = e;
    entity = null;
  }

  public boolean hasTile() {
    return tileEntity != null;
  }

  public boolean hasEntity() {
    return entity != null;
  }

  public void setTimeout() {
    lasttime = System.currentTimeMillis() + 1000;
  }

  public boolean isForcedVisible() {
    return lasttime > System.currentTimeMillis();
  }

  public void setCulled(boolean value) {
    this.culled = value;
    if (!value) {
      setTimeout();
    }
  }

  public boolean isCulled() {
    if (!EntityCullingBase.enabled) return false;
    return culled;
  }

  public void setOutOfCamera(boolean value) {
    this.outOfCamera = value;
  }

  public boolean isOutOfCamera() {
    if (!EntityCullingBase.enabled) return false;
    return outOfCamera;
  }
}
