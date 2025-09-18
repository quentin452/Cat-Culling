package net.tclproject.entityculling.handlers;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.tclproject.entityculling.EntityCullingBase;

public class CullableEntityWrapper {
  private long lasttime = 0L;
  private boolean culled = false;
  private boolean outOfCamera = false;
  public Entity entity;
  public TileEntity tileEntity;

  public CullableEntityWrapper(Entity e) {
    this.entity = e;
    this.tileEntity = null;
  }

  public CullableEntityWrapper(TileEntity e) {
    this.tileEntity = e;
    this.entity = null;
  }

  public boolean hasTile() {
    return this.tileEntity != null;
  }

  public boolean hasEntity() {
    return this.entity != null;
  }

  public void setTimeout() {
    this.lasttime = System.currentTimeMillis() + 1000L;
  }

  public boolean isForcedVisible() {
    return this.lasttime > System.currentTimeMillis();
  }

  public void setCulled(boolean value) {
    this.culled = value;
    if (!value) {
      this.setTimeout();
    }
  }

  public boolean isCulled() {
    if (!EntityCullingBase.enabled) {
      return false;
    }
    return this.culled;
  }

  public void setOutOfCamera(boolean value) {
    this.outOfCamera = value;
  }

  public boolean isOutOfCamera() {
    if (!EntityCullingBase.enabled) {
      return false;
    }
    return this.outOfCamera;
  }
}
