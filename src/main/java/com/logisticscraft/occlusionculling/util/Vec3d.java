package com.logisticscraft.occlusionculling.util;

public class Vec3d {
  public double x;
  public double y;
  public double z;

  public Vec3d(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public double getX() {
    return this.x;
  }

  public double getY() {
    return this.y;
  }

  public double getZ() {
    return this.z;
  }

  public void set(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public void setAdd(Vec3d vec, double x, double y, double z) {
    this.x = vec.x + x;
    this.y = vec.y + y;
    this.z = vec.z + z;
  }

  public Vec3d div(Vec3d rayDir) {
    this.x /= rayDir.x;
    this.z /= rayDir.z;
    this.y /= rayDir.y;
    return this;
  }

  public Vec3d normalize() {
    double mag = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    this.x /= mag;
    this.y /= mag;
    this.z /= mag;
    return this;
  }

  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Vec3d)) {
      return false;
    }
    Vec3d vec3d = (Vec3d) other;
    if (Double.compare(vec3d.x, this.x) != 0) {
      return false;
    }
    if (Double.compare(vec3d.y, this.y) != 0) {
      return false;
    }
    return Double.compare(vec3d.z, this.z) == 0;
  }

  public int hashCode() {
    long l = Double.doubleToLongBits(this.x);
    int i = (int) (l ^ l >>> 32);
    l = Double.doubleToLongBits(this.y);
    i = 31 * i + (int) (l ^ l >>> 32);
    l = Double.doubleToLongBits(this.z);
    i = 31 * i + (int) (l ^ l >>> 32);
    return i;
  }

  public String toString() {
    return "(" + this.x + ", " + this.y + ", " + this.z + ")";
  }
}
