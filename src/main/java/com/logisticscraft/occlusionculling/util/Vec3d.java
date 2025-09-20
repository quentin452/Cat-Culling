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
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
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
        double magSq = x * x + y * y + z * z;
        if (magSq == 0.0) {
            return this; // Avoid division by zero
        }
        double invMag = 1.0 / Math.sqrt(magSq);
        this.x *= invMag;
        this.y *= invMag;
        this.z *= invMag;
        return this;
    }

    public Vec3d setAndDiv(double x, double y, double z, Vec3d divisor) {
        // Check for division by zero to avoid NaN values
        this.x = (divisor.x != 0.0) ? x / divisor.x : Double.MAX_VALUE;
        this.y = (divisor.y != 0.0) ? y / divisor.y : Double.MAX_VALUE;
        this.z = (divisor.z != 0.0) ? z / divisor.z : Double.MAX_VALUE;
        return this;
    }

    public Vec3d setAndNormalize(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        
        // Inline normalization for better performance
        double magSq = x * x + y * y + z * z;
        if (magSq == 0.0) {
            return this; // Avoid division by zero
        }
        double invMag = 1.0 / Math.sqrt(magSq);
        this.x *= invMag;
        this.y *= invMag;
        this.z *= invMag;
        return this;
    }
    
    // Additional utility methods for performance optimization
    public double distanceSquared(Vec3d other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        double dz = this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public double distanceSquared(double ox, double oy, double oz) {
        double dx = this.x - ox;
        double dy = this.y - oy;
        double dz = this.z - oz;
        return dx * dx + dy * dy + dz * dz;
    }
    
    public double magnitudeSquared() {
        return x * x + y * y + z * z;
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vec3d)) {
            return false;
        }
        Vec3d vec3d = (Vec3d) other;
        if (Double.compare(vec3d.x, x) != 0) {
            return false;
        }
        if (Double.compare(vec3d.y, y) != 0) {
            return false;
        }
        return Double.compare(vec3d.z, z) == 0;
    }

    @Override
    public int hashCode() {
        long l = Double.doubleToLongBits(x);
        int i = (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(y);
        i = 31 * i + (int) (l ^ l >>> 32);
        l = Double.doubleToLongBits(z);
        i = 31 * i + (int) (l ^ l >>> 32);
        return i;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
