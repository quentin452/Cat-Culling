package net.tclproject.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;

public class CullTask implements Runnable {
  public boolean requestCull = false;
  private final OcclusionCullingInstance culling;
  private final Minecraft client = Minecraft.getMinecraft();
  private final int sleepDelay = Config.sleepDelay;
  private final int hitboxLimit = Config.hitboxLimit;
  private final ArrayList<String> unCullable;
  public long lastTime = 0L;
  private Vec3d lastPos = new Vec3d(0.0, 0.0, 0.0);
  private Vec3d aabbMin = new Vec3d(0.0, 0.0, 0.0);
  private Vec3d aabbMax = new Vec3d(0.0, 0.0, 0.0);

  public CullTask(OcclusionCullingInstance culling, String[] unCullable) {
    this.culling = culling;
    this.unCullable = new ArrayList<String>(Arrays.asList(unCullable));
  }

  @Override
  public void run() {
    while (this.client != null) {
      try {
        Thread.sleep(this.sleepDelay);
        if (!EntityCullingBase.enabled
            || this.client.theWorld == null
            || this.client.thePlayer == null
            || this.client.thePlayer.ticksExisted <= 10
            || this.client.renderViewEntity == null) continue;
        Vec3 cameraMC = null;
        cameraMC =
            Config.debugMode
                ? this.getPositionEyes((Entity) this.client.thePlayer, 0.0f)
                : this.getCameraPos();
        if (!this.requestCull
            && cameraMC.xCoord == this.lastPos.x
            && cameraMC.yCoord == this.lastPos.y
            && cameraMC.zCoord == this.lastPos.z) continue;
        long start = System.currentTimeMillis();
        this.requestCull = false;
        this.lastPos.set(cameraMC.xCoord, cameraMC.yCoord, cameraMC.zCoord);
        Vec3d camera = this.lastPos;
        this.culling.resetCache();
        boolean noCulling =
            this.client.thePlayer.noClip || this.client.gameSettings.thirdPersonView != 0;
        Iterator iterator = this.client.theWorld.loadedTileEntityList.iterator();
        while (iterator.hasNext()) {
          boolean visible;
          AxisAlignedBB boundingBox;
          CullableEntityWrapper cullable;
          TileEntity entry;
          try {
            entry = (TileEntity) iterator.next();
          } catch (NullPointerException | ConcurrentModificationException ex) {
            break;
          }
          if (this.unCullable.contains(entry.getBlockType().getUnlocalizedName())
              || (cullable = CullableEntityRegistry.getWrapper(entry)).isForcedVisible()) continue;
          if (noCulling) {
            cullable.setCulled(false);
            continue;
          }
          if (!(entry.getDistanceFrom(cameraMC.xCoord, cameraMC.yCoord, cameraMC.zCoord) < 4096.0)
              || this.setBoxAndCheckLimits(cullable, boundingBox = entry.getRenderBoundingBox()))
            continue;
          if (Config.debugMode) {
            System.out.println(
                "Currently processing tileentity " + entry.getBlockType().getUnlocalizedName());
          }
          cullable.setCulled(
              !(visible = this.culling.isAABBVisible(this.aabbMin, this.aabbMax, camera)));
        }
        Entity entity = null;
        Iterator iterable = this.client.theWorld.getLoadedEntityList().iterator();
        while (iterable.hasNext()) {
          boolean visible;
          CullableEntityWrapper cullable;
          try {
            entity = (Entity) iterable.next();
          } catch (NullPointerException | ConcurrentModificationException ex) {
            break;
          }
          if (entity == null
              || (cullable = CullableEntityRegistry.getWrapper(entity)).isForcedVisible()) continue;
          if (noCulling) {
            cullable.setCulled(false);
            continue;
          }
          if (CullTask.getPositionVector(entity).squareDistanceTo(cameraMC)
              > (double) (Config.tracingDistance * Config.tracingDistance)) {
            cullable.setCulled(false);
            continue;
          }
          AxisAlignedBB boundingBox = entity.boundingBox;
          if (this.setBoxAndCheckLimits(cullable, boundingBox)) continue;
          if (Config.debugMode) {
            System.out.println("Currently processing entity " + entity.getCommandSenderName());
          }
          cullable.setCulled(
              !(visible = this.culling.isAABBVisible(this.aabbMin, this.aabbMax, camera)));
        }
        this.lastTime = System.currentTimeMillis() - start;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.out.println("Shutting down culling task!");
  }

  private boolean setBoxAndCheckLimits(CullableEntityWrapper cullable, AxisAlignedBB boundingBox) {
    if (boundingBox.maxX - boundingBox.minX > (double) this.hitboxLimit
        || boundingBox.maxY - boundingBox.minY > (double) this.hitboxLimit
        || boundingBox.maxZ - boundingBox.minZ > (double) this.hitboxLimit) {
      cullable.setCulled(false);
      return true;
    }
    this.aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
    this.aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
    return false;
  }

  public static Vec3 getPositionVector(Entity e) {
    return Vec3.createVectorHelper((double) e.posX, (double) e.posY, (double) e.posZ);
  }

  @SideOnly(value = Side.CLIENT)
  public Vec3 getPositionEyes(Entity e, float partialTicks) {
    if (partialTicks == 1.0f) {
      return Vec3.createVectorHelper(
          (double) e.posX, (double) (e.posY + (double) e.getEyeHeight()), (double) e.posZ);
    }
    double d0 = e.prevPosX + (e.posX - e.prevPosX) * (double) partialTicks;
    double d1 =
        e.prevPosY + (e.posY - e.prevPosY) * (double) partialTicks + (double) e.getEyeHeight();
    double d2 = e.prevPosZ + (e.posZ - e.prevPosZ) * (double) partialTicks;
    return Vec3.createVectorHelper((double) d0, (double) d1, (double) d2);
  }

  private Vec3 getCameraPos() {
    return this.getPositionEyes((Entity) this.client.renderViewEntity, 0.0f);
  }
}
