package net.tclproject.mysteriumlib.asm.common;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer;
import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.MetaReader;
import net.tclproject.mysteriumlib.asm.core.TargetClassTransformer;
import org.apache.logging.log4j.Level;

@IFMLLoadingPlugin.TransformerExclusions(value = {"net.tclproject"})
public class CustomLoadingPlugin implements IFMLLoadingPlugin {
  private static DeobfuscationTransformer deobfuscationTransformer;
  private static boolean checkedObfuscation;
  private static boolean obfuscated;
  private static MetaReader mcMetaReader;
  public static boolean foundThaumcraft;
  public static boolean foundDragonAPI;
  public static boolean isDevEnvironment;
  public static File debugOutputLocation;

  public static TargetClassTransformer getTransformer() {
    return FirstClassTransformer.instance.registeredBuiltinFixes
        ? CustomClassTransformer.instance
        : FirstClassTransformer.instance;
  }

  public static void registerFix(ASMFix fix) {
    CustomLoadingPlugin.getTransformer().registerFix(fix);
  }

  public static void registerClassWithFixes(String className) {
    CustomLoadingPlugin.getTransformer().registerClassWithFixes(className);
  }

  public static MetaReader getMetaReader() {
    return mcMetaReader;
  }

  static DeobfuscationTransformer getDeobfuscationTransformer() {
    if (CustomLoadingPlugin.isObfuscated() && deobfuscationTransformer == null) {
      deobfuscationTransformer = new DeobfuscationTransformer();
    }
    return deobfuscationTransformer;
  }

  public static boolean isObfuscated() {
    if (!checkedObfuscation) {
      try {
        Field deobfuscatedField = CoreModManager.class.getDeclaredField("deobfuscatedEnvironment");
        deobfuscatedField.setAccessible(true);
        obfuscated = !deobfuscatedField.getBoolean(null);
      } catch (Exception e) {
        FMLLog.log(
            (String) "Mysterium Patches",
            (Level) Level.ERROR,
            (String) "Error occured when checking obfuscation.",
            (Object[]) new Object[0]);
        FMLLog.log(
            (String) "Mysterium Patches",
            (Level) Level.ERROR,
            (String)
                "THIS IS MOST LIKELY HAPPENING BECAUSE OF MOD CONFLICTS. PLEASE CONTACT ME TO LET ME KNOW.",
            (Object[]) new Object[0]);
        FMLLog.log(
            (String) "Mysterium Patches",
            (Level) Level.ERROR,
            (String) e.getMessage(),
            (Object[]) new Object[0]);
      }
      checkedObfuscation = true;
    }
    return obfuscated;
  }

  public String getAccessTransformerClass() {
    return null;
  }

  public String[] getASMTransformerClass() {
    return null;
  }

  public String getModContainerClass() {
    return null;
  }

  public String getSetupClass() {
    return null;
  }

  public void injectData(Map<String, Object> data) {
    debugOutputLocation = new File(data.get("mcLocation").toString(), "bg edited classes");
    if (((ArrayList) data.get("coremodList")).contains("DragonAPIASMHandler")) {
      foundDragonAPI = true;
    }
    File loc = (File) data.get("mcLocation");
    isDevEnvironment = (Boolean) data.get("runtimeDeobfuscationEnabled") == false;
    File mcFolder = new File(loc.getAbsolutePath() + File.separatorChar + "mods");
    File mcVersionFolder = new File(mcFolder.getAbsolutePath() + File.separatorChar + "1.7.10");
    ArrayList<File> subfiles = new ArrayList<>();
    if (mcFolder.listFiles() != null) {
      subfiles = new ArrayList<>(Arrays.asList(mcFolder.listFiles()));
      if (mcVersionFolder.listFiles() != null) {
        subfiles.addAll(Arrays.asList(mcVersionFolder.listFiles()));
      }
    }
    for (File file : subfiles) {
      String name = file.getName();
      if (name != null && !(name = name.toLowerCase()).endsWith(".jar") && !name.endsWith(".zip"))
        continue;
    }
    this.registerFixes();
  }

  public void registerFixes() {}

  static {
    foundThaumcraft = false;
    foundDragonAPI = false;
    isDevEnvironment = false;
    mcMetaReader = new MinecraftMetaReader();
  }
}
