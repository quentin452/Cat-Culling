package fr.iamacat.catculling.handlers;

import net.minecraft.client.particle.EntityFX;

import fr.iamacat.catculling.EntityCullingBase;

public class CullableParticleWrapper {

    private long lasttime = 0;
    private boolean culled = false;
    private boolean outOfCamera = false;
    private static long cachedTime = System.currentTimeMillis();
    private static long lastCacheUpdate = 0;
    private static final long CACHE_UPDATE_INTERVAL = 50; // Update cache every 50ms

    public EntityFX particle;

    public CullableParticleWrapper(EntityFX particle) {
        this.particle = particle;
    }

    public boolean hasParticle() {
        return particle != null;
    }

    public void setTimeout() {
        lasttime = getCachedTime() + 1000;
    }

    public boolean isForcedVisible() {
        return lasttime > getCachedTime();
    }
    
    // Cache System.currentTimeMillis() to avoid expensive system calls
    private static long getCachedTime() {
        long now = System.currentTimeMillis();
        if (now - lastCacheUpdate > CACHE_UPDATE_INTERVAL) {
            cachedTime = now;
            lastCacheUpdate = now;
        }
        return cachedTime;
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
        return outOfCamera;
    }
}
