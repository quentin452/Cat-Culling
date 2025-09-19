package net.tclproject.entityculling;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import com.logisticscraft.occlusionculling.DataProvider;

public class Provider implements DataProvider {

    private final Minecraft client = Minecraft.getMinecraft();
    private WorldClient world = null;

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        world = client.theWorld;
        return world != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        if (world == null) {
            return false; // Safe fallback when world is null
        }
        return world.getBlock(x, y, z)
            .isOpaqueCube();
    }

    @Override
    public void cleanup() {
        world = null;
    }
}
