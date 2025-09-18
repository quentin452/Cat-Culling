package net.tclproject.mysteriumlib.math;

import java.util.Random;

public class SimplexNoise {
  private static final Grad[] grad3 =
      new Grad[] {
        new Grad(1.0, 1.0, 0.0), new Grad(-1.0, 1.0, 0.0),
        new Grad(1.0, -1.0, 0.0), new Grad(-1.0, -1.0, 0.0),
        new Grad(1.0, 0.0, 1.0), new Grad(-1.0, 0.0, 1.0),
        new Grad(1.0, 0.0, -1.0), new Grad(-1.0, 0.0, -1.0),
        new Grad(0.0, 1.0, 1.0), new Grad(0.0, -1.0, 1.0),
        new Grad(0.0, 1.0, -1.0), new Grad(0.0, -1.0, -1.0)
      };
  private static final Grad[] grad4 =
      new Grad[] {
        new Grad(0.0, 1.0, 1.0, 1.0), new Grad(0.0, 1.0, 1.0, -1.0),
        new Grad(0.0, 1.0, -1.0, 1.0), new Grad(0.0, 1.0, -1.0, -1.0),
        new Grad(0.0, -1.0, 1.0, 1.0), new Grad(0.0, -1.0, 1.0, -1.0),
        new Grad(0.0, -1.0, -1.0, 1.0), new Grad(0.0, -1.0, -1.0, -1.0),
        new Grad(1.0, 0.0, 1.0, 1.0), new Grad(1.0, 0.0, 1.0, -1.0),
        new Grad(1.0, 0.0, -1.0, 1.0), new Grad(1.0, 0.0, -1.0, -1.0),
        new Grad(-1.0, 0.0, 1.0, 1.0), new Grad(-1.0, 0.0, 1.0, -1.0),
        new Grad(-1.0, 0.0, -1.0, 1.0), new Grad(-1.0, 0.0, -1.0, -1.0),
        new Grad(1.0, 1.0, 0.0, 1.0), new Grad(1.0, 1.0, 0.0, -1.0),
        new Grad(1.0, -1.0, 0.0, 1.0), new Grad(1.0, -1.0, 0.0, -1.0),
        new Grad(-1.0, 1.0, 0.0, 1.0), new Grad(-1.0, 1.0, 0.0, -1.0),
        new Grad(-1.0, -1.0, 0.0, 1.0), new Grad(-1.0, -1.0, 0.0, -1.0),
        new Grad(1.0, 1.0, 1.0, 0.0), new Grad(1.0, 1.0, -1.0, 0.0),
        new Grad(1.0, -1.0, 1.0, 0.0), new Grad(1.0, -1.0, -1.0, 0.0),
        new Grad(-1.0, 1.0, 1.0, 0.0), new Grad(-1.0, 1.0, -1.0, 0.0),
        new Grad(-1.0, -1.0, 1.0, 0.0), new Grad(-1.0, -1.0, -1.0, 0.0)
      };
  private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
  private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
  private static final double F3 = 0.3333333333333333;
  private static final double G3 = 0.16666666666666666;
  private static final double F4 = (Math.sqrt(5.0) - 1.0) / 4.0;
  private static final double G4 = (5.0 - Math.sqrt(5.0)) / 20.0;
  protected final short[] doubledPermutationTable;
  protected final short[] variatedPermutationTable;
  protected final Random random;

  private static int fastfloor(double x) {
    int xi = (int) x;
    return x < (double) xi ? xi - 1 : xi;
  }

  private static double dot(Grad g, double x, double y) {
    return g.x * x + g.y * y;
  }

  private static double dot(Grad g, double x, double y, double z) {
    return g.x * x + g.y * y + g.z * z;
  }

  private static double dot(Grad g, double x, double y, double z, double w) {
    return g.x * x + g.y * y + g.z * z + g.w * w;
  }

  public SimplexNoise(Random random) {
    byte[] bytes = new byte[1024];
    this.random = random;
    random.nextBytes(bytes);
    this.doubledPermutationTable = new short[bytes.length * 2];
    this.variatedPermutationTable = new short[this.doubledPermutationTable.length];
    for (int i = 0; i < bytes.length; ++i) {
      short value;
      this.doubledPermutationTable[i] = value = (short) (bytes[i] & 0xFF);
      this.variatedPermutationTable[i] = (short) (value % 12);
    }
    System.arraycopy(
        this.doubledPermutationTable, 0, this.doubledPermutationTable, bytes.length, bytes.length);
    System.arraycopy(
        this.variatedPermutationTable,
        0,
        this.variatedPermutationTable,
        bytes.length,
        bytes.length);
  }

  public Random getRandom() {
    return this.random;
  }

  public NoiseStretch generateNoiseStretcher(
      double stretchX, double stretchZ, double offsetX, double offsetZ) {
    return new NoiseStretch(this, stretchX, stretchZ, offsetX, offsetZ);
  }

  public NoiseStretch generateNoiseStretcher(
      double stretchX,
      double stretchY,
      double stretchZ,
      double offsetX,
      double offsetY,
      double offsetZ) {
    return new NoiseStretch(this, stretchX, stretchY, stretchZ, offsetX, offsetY, offsetZ);
  }

  public double noise(double xin, double yin) {
    double n2;
    double n1;
    double n0;
    int j1;
    int i1;
    double Y0;
    double y0;
    int j;
    double t;
    double s = (xin + yin) * F2;
    int i = SimplexNoise.fastfloor(xin + s);
    double X0 = (double) i - (t = (double) (i + (j = SimplexNoise.fastfloor(yin + s))) * G2);
    double x0 = xin - X0;
    if (x0 > (y0 = yin - (Y0 = (double) j - t))) {
      i1 = 1;
      j1 = 0;
    } else {
      i1 = 0;
      j1 = 1;
    }
    double x1 = x0 - (double) i1 + G2;
    double y1 = y0 - (double) j1 + G2;
    double x2 = x0 - 1.0 + 2.0 * G2;
    double y2 = y0 - 1.0 + 2.0 * G2;
    int ii = i & 0x3FF;
    int jj = j & 0x3FF;
    short gi0 = this.variatedPermutationTable[ii + this.doubledPermutationTable[jj]];
    short gi1 = this.variatedPermutationTable[ii + i1 + this.doubledPermutationTable[jj + j1]];
    short gi2 = this.variatedPermutationTable[ii + 1 + this.doubledPermutationTable[jj + 1]];
    double t0 = 0.5 - x0 * x0 - y0 * y0;
    if (t0 < 0.0) {
      n0 = 0.0;
    } else {
      t0 *= t0;
      n0 = t0 * t0 * SimplexNoise.dot(grad3[gi0], x0, y0);
    }
    double t1 = 0.5 - x1 * x1 - y1 * y1;
    if (t1 < 0.0) {
      n1 = 0.0;
    } else {
      t1 *= t1;
      n1 = t1 * t1 * SimplexNoise.dot(grad3[gi1], x1, y1);
    }
    double t2 = 0.5 - x2 * x2 - y2 * y2;
    if (t2 < 0.0) {
      n2 = 0.0;
    } else {
      t2 *= t2;
      n2 = t2 * t2 * SimplexNoise.dot(grad3[gi2], x2, y2);
    }
    return 70.0 * (n0 + n1 + n2);
  }

  public double noise(double xin, double yin, double zin) {
    double n3;
    double n2;
    double n1;
    double n0;
    int k2;
    int j2;
    int i2;
    int k1;
    int j1;
    int i1;
    double s = (xin + yin + zin) * 0.3333333333333333;
    int i = SimplexNoise.fastfloor(xin + s);
    int j = SimplexNoise.fastfloor(yin + s);
    int k = SimplexNoise.fastfloor(zin + s);
    double t = (double) (i + j + k) * 0.16666666666666666;
    double X0 = (double) i - t;
    double Y0 = (double) j - t;
    double Z0 = (double) k - t;
    double x0 = xin - X0;
    double y0 = yin - Y0;
    double z0 = zin - Z0;
    if (x0 >= y0) {
      if (y0 >= z0) {
        i1 = 1;
        j1 = 0;
        k1 = 0;
        i2 = 1;
        j2 = 1;
        k2 = 0;
      } else if (x0 >= z0) {
        i1 = 1;
        j1 = 0;
        k1 = 0;
        i2 = 1;
        j2 = 0;
        k2 = 1;
      } else {
        i1 = 0;
        j1 = 0;
        k1 = 1;
        i2 = 1;
        j2 = 0;
        k2 = 1;
      }
    } else if (y0 < z0) {
      i1 = 0;
      j1 = 0;
      k1 = 1;
      i2 = 0;
      j2 = 1;
      k2 = 1;
    } else if (x0 < z0) {
      i1 = 0;
      j1 = 1;
      k1 = 0;
      i2 = 0;
      j2 = 1;
      k2 = 1;
    } else {
      i1 = 0;
      j1 = 1;
      k1 = 0;
      i2 = 1;
      j2 = 1;
      k2 = 0;
    }
    double x1 = x0 - (double) i1 + 0.16666666666666666;
    double y1 = y0 - (double) j1 + 0.16666666666666666;
    double z1 = z0 - (double) k1 + 0.16666666666666666;
    double x2 = x0 - (double) i2 + 0.3333333333333333;
    double y2 = y0 - (double) j2 + 0.3333333333333333;
    double z2 = z0 - (double) k2 + 0.3333333333333333;
    double x3 = x0 - 1.0 + 0.5;
    double y3 = y0 - 1.0 + 0.5;
    double z3 = z0 - 1.0 + 0.5;
    int ii = i & 0xFF;
    int jj = j & 0xFF;
    int kk = k & 0xFF;
    short gi0 =
        this.variatedPermutationTable[
            ii + this.doubledPermutationTable[jj + this.doubledPermutationTable[kk]]];
    short gi1 =
        this.variatedPermutationTable[
            ii
                + i1
                + this.doubledPermutationTable[jj + j1 + this.doubledPermutationTable[kk + k1]]];
    short gi2 =
        this.variatedPermutationTable[
            ii
                + i2
                + this.doubledPermutationTable[jj + j2 + this.doubledPermutationTable[kk + k2]]];
    short gi3 =
        this.variatedPermutationTable[
            ii + 1 + this.doubledPermutationTable[jj + 1 + this.doubledPermutationTable[kk + 1]]];
    double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
    if (t0 < 0.0) {
      n0 = 0.0;
    } else {
      t0 *= t0;
      n0 = t0 * t0 * SimplexNoise.dot(grad3[gi0], x0, y0, z0);
    }
    double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
    if (t1 < 0.0) {
      n1 = 0.0;
    } else {
      t1 *= t1;
      n1 = t1 * t1 * SimplexNoise.dot(grad3[gi1], x1, y1, z1);
    }
    double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
    if (t2 < 0.0) {
      n2 = 0.0;
    } else {
      t2 *= t2;
      n2 = t2 * t2 * SimplexNoise.dot(grad3[gi2], x2, y2, z2);
    }
    double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
    if (t3 < 0.0) {
      n3 = 0.0;
    } else {
      t3 *= t3;
      n3 = t3 * t3 * SimplexNoise.dot(grad3[gi3], x3, y3, z3);
    }
    return 32.0 * (n0 + n1 + n2 + n3);
  }

  public double noise(double x, double y, double z, double w) {
    double n4;
    double n3;
    double n2;
    double n1;
    double n0;
    double s = (x + y + z + w) * F4;
    int i = SimplexNoise.fastfloor(x + s);
    int j = SimplexNoise.fastfloor(y + s);
    int k = SimplexNoise.fastfloor(z + s);
    int l = SimplexNoise.fastfloor(w + s);
    double t = (double) (i + j + k + l) * G4;
    double X0 = (double) i - t;
    double Y0 = (double) j - t;
    double Z0 = (double) k - t;
    double W0 = (double) l - t;
    double x0 = x - X0;
    double y0 = y - Y0;
    double z0 = z - Z0;
    double w0 = w - W0;
    int rankx = 0;
    int ranky = 0;
    int rankz = 0;
    int rankw = 0;
    if (x0 > y0) {
      ++rankx;
    } else {
      ++ranky;
    }
    if (x0 > z0) {
      ++rankx;
    } else {
      ++rankz;
    }
    if (x0 > w0) {
      ++rankx;
    } else {
      ++rankw;
    }
    if (y0 > z0) {
      ++ranky;
    } else {
      ++rankz;
    }
    if (y0 > w0) {
      ++ranky;
    } else {
      ++rankw;
    }
    if (z0 > w0) {
      ++rankz;
    } else {
      ++rankw;
    }
    int i1 = rankx >= 3 ? 1 : 0;
    int j1 = ranky >= 3 ? 1 : 0;
    int k1 = rankz >= 3 ? 1 : 0;
    int l1 = rankw >= 3 ? 1 : 0;
    int i2 = rankx >= 2 ? 1 : 0;
    int j2 = ranky >= 2 ? 1 : 0;
    int k2 = rankz >= 2 ? 1 : 0;
    int l2 = rankw >= 2 ? 1 : 0;
    int i3 = rankx >= 1 ? 1 : 0;
    int j3 = ranky >= 1 ? 1 : 0;
    int k3 = rankz >= 1 ? 1 : 0;
    int l3 = rankw >= 1 ? 1 : 0;
    double x1 = x0 - (double) i1 + G4;
    double y1 = y0 - (double) j1 + G4;
    double z1 = z0 - (double) k1 + G4;
    double w1 = w0 - (double) l1 + G4;
    double x2 = x0 - (double) i2 + 2.0 * G4;
    double y2 = y0 - (double) j2 + 2.0 * G4;
    double z2 = z0 - (double) k2 + 2.0 * G4;
    double w2 = w0 - (double) l2 + 2.0 * G4;
    double x3 = x0 - (double) i3 + 3.0 * G4;
    double y3 = y0 - (double) j3 + 3.0 * G4;
    double z3 = z0 - (double) k3 + 3.0 * G4;
    double w3 = w0 - (double) l3 + 3.0 * G4;
    double x4 = x0 - 1.0 + 4.0 * G4;
    double y4 = y0 - 1.0 + 4.0 * G4;
    double z4 = z0 - 1.0 + 4.0 * G4;
    double w4 = w0 - 1.0 + 4.0 * G4;
    int ii = i & 0xFF;
    int jj = j & 0xFF;
    int kk = k & 0xFF;
    int ll = l & 0xFF;
    int gi0 =
        this.doubledPermutationTable[
                ii
                    + this.doubledPermutationTable[
                        jj + this.doubledPermutationTable[kk + this.doubledPermutationTable[ll]]]]
            % 32;
    int gi1 =
        this.doubledPermutationTable[
                ii
                    + i1
                    + this.doubledPermutationTable[
                        jj
                            + j1
                            + this.doubledPermutationTable[
                                kk + k1 + this.doubledPermutationTable[ll + l1]]]]
            % 32;
    int gi2 =
        this.doubledPermutationTable[
                ii
                    + i2
                    + this.doubledPermutationTable[
                        jj
                            + j2
                            + this.doubledPermutationTable[
                                kk + k2 + this.doubledPermutationTable[ll + l2]]]]
            % 32;
    int gi3 =
        this.doubledPermutationTable[
                ii
                    + i3
                    + this.doubledPermutationTable[
                        jj
                            + j3
                            + this.doubledPermutationTable[
                                kk + k3 + this.doubledPermutationTable[ll + l3]]]]
            % 32;
    int gi4 =
        this.doubledPermutationTable[
                ii
                    + 1
                    + this.doubledPermutationTable[
                        jj
                            + 1
                            + this.doubledPermutationTable[
                                kk + 1 + this.doubledPermutationTable[ll + 1]]]]
            % 32;
    double t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0 - w0 * w0;
    if (t0 < 0.0) {
      n0 = 0.0;
    } else {
      t0 *= t0;
      n0 = t0 * t0 * SimplexNoise.dot(grad4[gi0], x0, y0, z0, w0);
    }
    double t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1 - w1 * w1;
    if (t1 < 0.0) {
      n1 = 0.0;
    } else {
      t1 *= t1;
      n1 = t1 * t1 * SimplexNoise.dot(grad4[gi1], x1, y1, z1, w1);
    }
    double t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2 - w2 * w2;
    if (t2 < 0.0) {
      n2 = 0.0;
    } else {
      t2 *= t2;
      n2 = t2 * t2 * SimplexNoise.dot(grad4[gi2], x2, y2, z2, w2);
    }
    double t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3 - w3 * w3;
    if (t3 < 0.0) {
      n3 = 0.0;
    } else {
      t3 *= t3;
      n3 = t3 * t3 * SimplexNoise.dot(grad4[gi3], x3, y3, z3, w3);
    }
    double t4 = 0.6 - x4 * x4 - y4 * y4 - z4 * z4 - w4 * w4;
    if (t4 < 0.0) {
      n4 = 0.0;
    } else {
      t4 *= t4;
      n4 = t4 * t4 * SimplexNoise.dot(grad4[gi4], x4, y4, z4, w4);
    }
    return 27.0 * (n0 + n1 + n2 + n3 + n4);
  }

  private static class Grad {
    double x;
    double y;
    double z;
    double w;

    Grad(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    Grad(double x, double y, double z, double w) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.w = w;
    }
  }
}
