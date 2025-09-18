package net.tclproject.entityculling;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

public class Provider implements DataProvider {
  private final Minecraft client = Minecraft.getMinecraft();
  private WorldClient world = null;

  @Override
  public boolean prepareChunk(int chunkX, int chunkZ) {
    this.world = this.client.theWorld;
    return this.world != null;
  }

  @Override
  public boolean isOpaqueFullCube(int x, int y, int z) {
    return this.world.getBlock(x, y, z).isOpaqueCube();
  }

  @Override
  public void cleanup() {
    this.world = null;
  }
}
