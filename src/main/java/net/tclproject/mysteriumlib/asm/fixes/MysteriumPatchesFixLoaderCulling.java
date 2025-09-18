package net.tclproject.mysteriumlib.asm.fixes;

import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;

public class MysteriumPatchesFixLoaderCulling extends CustomLoadingPlugin {
  @Override
  public String[] getASMTransformerClass() {
    return new String[] {FirstClassTransformer.class.getName()};
  }

  @Override
  public void registerFixes() {
    MysteriumPatchesFixLoaderCulling.registerClassWithFixes(
        "net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixesCulling");
  }
}
