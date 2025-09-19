package net.tclproject.entityculling;

import com.logisticscraft.occlusionculling.OcclusionCullingInstance;
import com.logisticscraft.occlusionculling.util.Vec3d;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.tclproject.entityculling.handlers.Config;
import net.tclproject.entityculling.handlers.CullableEntityRegistry;
import net.tclproject.entityculling.handlers.CullableEntityWrapper;
import net.tclproject.entityculling.handlers.CullableParticleWrapper;

public class CullTask implements Runnable {

  private volatile boolean running = true;

  public boolean requestCull = false;

  private final OcclusionCullingInstance culling;

  // Reflection field for accessing EffectRenderer.fxLayers
  private static Field fxLayersField;
  private final Minecraft client = Minecraft.getMinecraft();
  private final int sleepDelay = Config.sleepDelay;
  private final int hitboxLimit = Config.hitboxLimit;
  private final ArrayList<String> unCullable;
  public long lastTime = 0;

  // reused preallocated vars
  private Vec3d lastPos = new Vec3d(0, 0, 0);
  private Vec3d aabbMin = new Vec3d(0, 0, 0);
  private Vec3d aabbMax = new Vec3d(0, 0, 0);

  public CullTask(OcclusionCullingInstance culling, String[] unCullable) {
    this.culling = culling;
    this.unCullable = new ArrayList<String>(Arrays.asList(unCullable));
    ;
  }

  @Override
  public void run() {
    while (running &&
           client != null) { // not correct, but the running field is hidden
      try {
        Thread.sleep(sleepDelay);

        if (EntityCullingBase.enabled && client.theWorld != null &&
            client.thePlayer != null && client.thePlayer.ticksExisted > 10 &&
            client.renderViewEntity != null) {
          Vec3 cameraMC = null;
          if (Config.debugMode) {
            cameraMC = getPositionEyes(client.thePlayer, 0);
          } else {
            cameraMC = getCameraPos();
          }
          if (requestCull ||
              !(cameraMC.xCoord == lastPos.x && cameraMC.yCoord == lastPos.y &&
                cameraMC.zCoord == lastPos.z)) {
            long start = System.currentTimeMillis();
            requestCull = false;
            lastPos.set(cameraMC.xCoord, cameraMC.yCoord, cameraMC.zCoord);
            Vec3d camera = lastPos;
            culling.resetCache();
            boolean noCulling = client.thePlayer.noClip ||
                                client.gameSettings.thirdPersonView !=
                                    0; // noClip is a 'spectator' check replacer
            // (EtFuturum Requiem compat)
            Iterator<TileEntity> iterator =
                client.theWorld.loadedTileEntityList.iterator();
            TileEntity entry;
            while (iterator.hasNext()) {
              try {
                entry = iterator.next();
              } catch (NullPointerException |
                       ConcurrentModificationException ex) {
                break; // We are not synced to the main thread, so NPE's/CME are
                // allowed here and
                // way less
                // overhead probably than trying to sync stuff up for no really
                // good reason
              }
              if (unCullable.contains(
                      entry.getBlockType().getUnlocalizedName())) {
                continue;
              }
              CullableEntityWrapper cullable =
                  CullableEntityRegistry.getWrapper(entry);
              if (!cullable.isForcedVisible()) {
                if (noCulling) {
                  cullable.setCulled(false);
                  continue;
                }

                if (entry.getDistanceFrom(cameraMC.xCoord, cameraMC.yCoord,
                                          cameraMC.zCoord) <
                    64 * 64) { // 64 is the fixed max tile view distance
                  AxisAlignedBB boundingBox = entry.getRenderBoundingBox();
                  //									aabbMin.set(entry.xCoord,
                  // entry.yCoord, entry.zCoord); // to account
                  // for larger-than-1-block TEs. possibly undo if this has
                  // unintended consequences
                  //								    aabbMax.set(entry.xCoord+1d,
                  // entry.yCoord+1d, entry.zCoord+1d);
                  if (setBoxAndCheckLimits(cullable, boundingBox))
                    continue;
                  if (Config.debugMode) {
                    System.out.println(
                        "Currently processing tileentity " +
                        entry.getBlockType().getUnlocalizedName());
                  }
                  boolean visible =
                      culling.isAABBVisible(aabbMin, aabbMax, camera);
                  //									System.out.println(visible
                  // + "," +
                  // entry.getBlockType().getUnlocalizedName());
                  cullable.setCulled(!visible);
                }
              }
            }
            Entity entity = null;
            Iterator<Entity> iterable =
                client.theWorld.getLoadedEntityList().iterator();
            while (iterable.hasNext()) {
              try {
                entity = iterable.next();
              } catch (NullPointerException |
                       ConcurrentModificationException ex) {
                break; // We are not synced to the main thread, so NPE's/CME are
                // allowed here and
                // way less
                // overhead probably than trying to sync stuff up for no really
                // good reason
              }
              if (entity == null) {
                continue; // Not sure how this could happen
              }
              CullableEntityWrapper cullable =
                  CullableEntityRegistry.getWrapper(entity);
              if (!cullable.isForcedVisible()) {
                if (noCulling) {
                  cullable.setCulled(false);
                  continue;
                }
                if (getPositionVector(entity).squareDistanceTo(cameraMC) >
                    Config.tracingDistance * Config.tracingDistance) {
                  cullable.setCulled(false); // If your entity view distance is
                  // larger than tracingDistance just
                  // render it
                  continue;
                }
                AxisAlignedBB boundingBox = entity.boundingBox;
                if (setBoxAndCheckLimits(cullable, boundingBox))
                  continue;
                if (Config.debugMode) {
                  System.out.println("Currently processing entity " +
                                     entity.getCommandSenderName());
                }
                boolean visible =
                    culling.isAABBVisible(aabbMin, aabbMax, camera);
                cullable.setCulled(!visible);
              }
            }

            // Process particles
            if (client.effectRenderer != null) {
              try {

                // Initialize reflection field if needed
                if (fxLayersField == null) {
                  // Essayons d'abord par le nom
                  try {
                    fxLayersField =
                        client.effectRenderer.getClass().getDeclaredField(
                            "fxLayers");
                    fxLayersField.setAccessible(true);
                  } catch (NoSuchFieldException e) {
                    // Si on ne trouve pas par le nom, cherchons par le type
                    for (Field field :
                         client.effectRenderer.getClass().getDeclaredFields()) {
                      if (field.getType().isArray() &&
                          field.getType().getComponentType() == List.class) {
                        fxLayersField = field;
                        fxLayersField.setAccessible(true);
                        break;
                      }
                    }
                    if (fxLayersField == null) {
                      throw new RuntimeException(
                          "Could not find fxLayers field in EffectRenderer");
                    }
                  }
                }

                // Get all particle lists from EffectRenderer using reflection
                @SuppressWarnings("unchecked")
                List<EntityFX>[] fxLayers =
                    (List<EntityFX>[])fxLayersField.get(client.effectRenderer);

                for (int layer = 0; layer < fxLayers.length; layer++) {
                  List<EntityFX> particles = fxLayers[layer];
                  if (particles != null && !particles.isEmpty()) {
                    Iterator<EntityFX> particleIterator = particles.iterator();
                    while (particleIterator.hasNext()) {
                      EntityFX particle = null;
                      try {
                        particle = particleIterator.next();
                      } catch (NullPointerException |
                               ConcurrentModificationException ex) {
                        break; // We are not synced to the main thread
                      }
                      if (particle == null || particle.isDead) {
                        continue;
                      }
                      CullableParticleWrapper cullable =
                          CullableEntityRegistry.getWrapper(particle);
                      if (Config.debugMode) {
                        System.out.println("Currently processing particle at " +
                                           particle.posX + "," + particle.posY +
                                           "," + particle.posZ);
                      }
                      if (!cullable.isForcedVisible()) {
                        if (noCulling) {
                          cullable.setCulled(false);
                          continue;
                        }
                        double dx = particle.posX - cameraMC.xCoord;
                        double dy = particle.posY - cameraMC.yCoord;
                        double dz = particle.posZ - cameraMC.zCoord;
                        double distanceSq = dx * dx + dy * dy + dz * dz;
                        if (distanceSq >
                            Config.tracingDistance * Config.tracingDistance) {
                          cullable.setCulled(
                              false); // Too far to bother culling
                          continue;
                        }
                        // Create a small bounding box for the particle
                        double size = 0.2; // Most particles are small
                        aabbMin.set(particle.posX - size, particle.posY - size,
                                    particle.posZ - size);
                        aabbMax.set(particle.posX + size, particle.posY + size,
                                    particle.posZ + size);

                        boolean visible =
                            culling.isAABBVisible(aabbMin, aabbMax, camera);
                        cullable.setCulled(!visible);
                      }
                    }
                  }
                }
              } catch (Exception e) {
                // If reflection fails, skip particle processing for this frame
                System.err.println("Failed to access particle layers: " +
                                   e.getMessage());
              }
            }

            lastTime = (System.currentTimeMillis() - start);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    System.out.println("Shutting down culling task!");
  }

  private boolean setBoxAndCheckLimits(CullableEntityWrapper cullable,
                                       AxisAlignedBB boundingBox) {
    if (boundingBox.maxX - boundingBox.minX > hitboxLimit ||
        boundingBox.maxY - boundingBox.minY > hitboxLimit ||
        boundingBox.maxZ - boundingBox.minZ > hitboxLimit) {
      cullable.setCulled(false); // To big to bother to cull
      return true;
    }
    aabbMin.set(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
    aabbMax.set(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
    return false;
  }

  public static Vec3 getPositionVector(Entity e) {
    return Vec3.createVectorHelper(e.posX, e.posY, e.posZ);
  }

  @SideOnly(Side.CLIENT)
  public Vec3 getPositionEyes(Entity e, float partialTicks) {
    if (partialTicks == 1.0F) {
      return Vec3.createVectorHelper(e.posX, e.posY + (double)e.getEyeHeight(),
                                     e.posZ);
    } else {
      double d0 = e.prevPosX + (e.posX - e.prevPosX) * (double)partialTicks;
      double d1 = e.prevPosY + (e.posY - e.prevPosY) * (double)partialTicks +
                  (double)e.getEyeHeight();
      double d2 = e.prevPosZ + (e.posZ - e.prevPosZ) * (double)partialTicks;
      return Vec3.createVectorHelper(d0, d1, d2);
    }
  }

  // 1.7.x doesn't know where the heck the camera is either
  private Vec3 getCameraPos() {
    return getPositionEyes(client.renderViewEntity, 0);
    // doesnt work correctly
    //        Entity entity = client.getRenderViewEntity();
    //        float f = entity.getEyeHeight();
    //        double d0 = entity.posX;
    //        double d1 = entity.posY + f;
    //        double d2 = entity.posZ;
    //        double d3 = 4.0F;
    //        float f1 = entity.rotationYaw;
    //        float f2 = entity.rotationPitch;
    //        if (client.gameSettings.thirdPersonView == 2)
    //            f2 += 180.0F;
    //        double d4 = (-MathHelper.sin(f1 / 180.0F * 3.1415927F) *
    //        MathHelper.cos(f2 / 180.0F *
    // 3.1415927F)) * d3;
    //        double d5 = (MathHelper.cos(f1 / 180.0F * 3.1415927F) *
    //        MathHelper.cos(f2 / 180.0F *
    // 3.1415927F)) * d3;
    //        double d6 = -MathHelper.sin(f2 / 180.0F * 3.1415927F) * d3;
    //        for (int i = 0; i < 8; i++) {
    //            float f3 = ((i & 0x1) * 2 - 1);
    //            float f4 = ((i >> 1 & 0x1) * 2 - 1);
    //            float f5 = ((i >> 2 & 0x1) * 2 - 1);
    //            f3 *= 0.1F;
    //            f4 *= 0.1F;
    //            f5 *= 0.1F;
    //            MovingObjectPosition movingobjectposition =
    //            client.theWorld.rayTraceBlocks(
    //                    new Vec3(d0 + f3, d1 + f4, d2 + f5),
    //                    new Vec3(d0 - d4 + f3 + f5, d1 - d6 + f4, d2 - d5 +
    //                    f5));
    //            if (movingobjectposition != null) {
    //                double d7 = movingobjectposition.hitVec.distanceTo(new
    //                Vec3(d0, d1, d2)); if (d7 < d3)
    //                    d3 = d7;
    //            }
    //        }
    //        float pitchRadian = f2 * (3.1415927F / 180); // X rotation
    //        float yawRadian   = f1   * (3.1415927F / 180); // Y rotation
    //        double newPosX = d0 - d3 *  MathHelper.sin( yawRadian ) *
    //        MathHelper.cos( pitchRadian
    // );
    //        double newPosY = d1 - d3 * -MathHelper.sin( pitchRadian );
    //        double newPosZ = d2 - d3 *  MathHelper.cos( yawRadian ) *
    //        MathHelper.cos( pitchRadian
    // );
    //        Vec3 vec = new Vec3(newPosX, newPosY, newPosZ);
    //        System.out.println(newPosX + " " + newPosY + " " + newPosZ);
    //        return vec;
  }

  /** Stop the culling thread safely. */
  public void shutdown() { this.running = false; }
}
