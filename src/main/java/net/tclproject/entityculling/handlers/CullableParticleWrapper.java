package net.tclproject.entityculling.handlers;

import net.minecraft.client.particle.EntityFX;
import net.tclproject.entityculling.EntityCullingBase;

public class CullableParticleWrapper {

    private long lasttime = 0;
    private boolean culled = false;
    private boolean outOfCamera = false;

    public EntityFX particle;

    public CullableParticleWrapper(EntityFX particle) {
        this.particle = particle;
    }

    public boolean hasParticle() {
        return particle != null;
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
        return outOfCamera;
    }
}
