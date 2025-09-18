package net.tclproject.mysteriumlib.asm.common;

import cpw.mods.fml.common.Loader;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.tclproject.mysteriumlib.asm.annotations.Fix;

public class BuiltinFixes {
  @Fix
  public static void injectData(Loader loader, Object... data) {
    ClassLoader classLoader = BuiltinFixes.class.getClassLoader();
    if (classLoader instanceof LaunchClassLoader) {
      ((LaunchClassLoader) classLoader).registerTransformer(CustomClassTransformer.class.getName());
    } else {
      System.out.println(
          "MysteriumASM Lib was not loaded by LaunchClassLoader. Fixes for minecraft code will not have any effect.");
    }
  }
}
