package net.tclproject.mysteriumlib.math;

import java.util.Iterator;
import joptsimple.internal.Objects;
import net.minecraft.world.ChunkCoordIntPair;

public class SpiralPatternGenerator implements Iterable {
  protected final ChunkCoordIntPair center;
  protected final int rangeMax;

  public SpiralPatternGenerator(ChunkCoordIntPair center) {
    this(center, Integer.MAX_VALUE);
  }

  public SpiralPatternGenerator(ChunkCoordIntPair center, int rangeMax) {
    Objects.ensureNotNull((Object) center);
    if (rangeMax < 0) {
      throw new IllegalArgumentException("The maximum range must be more than or equal to 0.");
    }
    this.center = center;
    this.rangeMax = rangeMax;
  }

  public Iterator iterator() {
    return new Iterator() {
      private int currentX;
      private int currentZ;
      protected SpiralDirection currentDirection;
      private int currentRange = 0;

      @Override
      public boolean hasNext() {
        return this.currentRange <= SpiralPatternGenerator.this.rangeMax;
      }

      public ChunkCoordIntPair next() {
        if (this.currentRange == 0) {
          ++this.currentRange;
          this.currentX = SpiralPatternGenerator.this.center.chunkXPos - this.currentRange;
          this.currentZ = SpiralPatternGenerator.this.center.chunkZPos - this.currentRange;
          this.currentDirection = SpiralDirection.right;
          return SpiralPatternGenerator.this.center;
        }
        ChunkCoordIntPair result = new ChunkCoordIntPair(this.currentX, this.currentZ);
        this.step();
        return result;
      }

      protected void step() {
        switch (this.currentDirection) {
          case right:
            {
              if (this.currentX
                  < SpiralPatternGenerator.this.center.chunkXPos + this.currentRange) {
                ++this.currentX;
              } else {
                this.currentDirection = SpiralDirection.down;
                this.step();
              }
            }
          case down:
            {
              if (this.currentZ
                  < SpiralPatternGenerator.this.center.chunkZPos + this.currentRange) {
                ++this.currentZ;
              } else {
                this.currentDirection = SpiralDirection.left;
                this.step();
              }
            }
          case left:
            {
              if (this.currentX
                  > SpiralPatternGenerator.this.center.chunkXPos - this.currentRange) {
                --this.currentX;
              } else {
                this.currentDirection = SpiralDirection.up;
                this.step();
              }
            }
          case up:
            {
              if (this.currentZ - 1
                  > SpiralPatternGenerator.this.center.chunkZPos - this.currentRange) {
                --this.currentZ;
                break;
              }
              this.currentDirection = SpiralDirection.right;
              ++this.currentRange;
              this.currentX = SpiralPatternGenerator.this.center.chunkXPos - this.currentRange;
              this.currentZ = SpiralPatternGenerator.this.center.chunkZPos - this.currentRange;
            }
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("Not supported at this moment in time.");
      }
    };
  }

  public ChunkCoordIntPair getCenter() {
    return this.center;
  }

  public int getRangeMax() {
    return this.rangeMax;
  }

  private static enum SpiralDirection {
    right,
    down,
    left,
    up;
  }
}
