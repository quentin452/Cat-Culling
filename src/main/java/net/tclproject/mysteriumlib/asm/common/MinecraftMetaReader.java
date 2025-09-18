package net.tclproject.mysteriumlib.asm.common;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import java.io.IOException;
import java.lang.reflect.Method;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.tclproject.mysteriumlib.asm.core.MetaReader;
import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassVisitor;

public class MinecraftMetaReader extends MetaReader {
  private static Method runTransformers;

  @Override
  public byte[] classToBytes(String name) throws IOException {
    byte[] bytes = super.classToBytes(MinecraftMetaReader.getRelevantName(name.replace('.', '/')));
    return MinecraftMetaReader.deobfuscateClass(name, bytes);
  }

  @Override
  public boolean checkSameMethod(
      String obfuscatedName, String sourceDescriptor, String mcpName, String targetDescriptor) {
    return MinecraftMetaReader.checkSameMethod(obfuscatedName, mcpName)
        && sourceDescriptor.equals(targetDescriptor);
  }

  @Override
  public MetaReader.MethodReference getMethodReferenceASM(
      String ownerClass, String methodName, String descriptor) throws IOException {
    MetaReader.FindMethodClassVisitor classVisitor =
        new MetaReader.FindMethodClassVisitor(methodName, descriptor);
    byte[] bytes = MinecraftMetaReader.getTransformedBytes(ownerClass);
    this.acceptVisitor(bytes, (ClassVisitor) classVisitor);
    return classVisitor.found
        ? new MetaReader.MethodReference(
            ownerClass, classVisitor.targetName, classVisitor.targetDescriptor)
        : null;
  }

  public static byte[] deobfuscateClass(String className, byte[] bytes) {
    if (CustomLoadingPlugin.getDeobfuscationTransformer() != null) {
      bytes =
          CustomLoadingPlugin.getDeobfuscationTransformer().transform(className, className, bytes);
    }
    return bytes;
  }

  public static byte[] getTransformedBytes(String name) throws IOException {
    String className = MinecraftMetaReader.getRelevantName(name);
    byte[] bytes = Launch.classLoader.getClassBytes(className);
    if (bytes == null) {
      throw new RuntimeException("The byte representation of " + className + " cannot be found.");
    }
    try {
      bytes = (byte[]) runTransformers.invoke(Launch.classLoader, className, name, bytes);
    } catch (Exception e) {
      FMLLog.log(
          (String) "Mysterium Patches",
          (Level) Level.ERROR,
          (String) "Error occured when making runTransformers in LaunchClassLoader usable.",
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
    return bytes;
  }

  public static String getRelevantName(String deobfName) {
    if (CustomLoadingPlugin.isObfuscated()) {
      return FMLDeobfuscatingRemapper.INSTANCE.unmap(deobfName);
    }
    return deobfName;
  }

  public static boolean checkSameMethod(String srgName, String mcpName) {
    if (CustomLoadingPlugin.isObfuscated() && CustomClassTransformer.instance != null) {
      int methodId = CustomClassTransformer.getMethodIndex(srgName);
      String remappedName = CustomClassTransformer.instance.getMethodNames().get(methodId);
      if (remappedName != null && remappedName.equals(mcpName)) {
        return true;
      }
    }
    return srgName.equals(mcpName);
  }

  static {
    try {
      runTransformers =
          LaunchClassLoader.class.getDeclaredMethod(
              "runTransformers", String.class, String.class, byte[].class);
      runTransformers.setAccessible(true);
    } catch (Exception e) {
      FMLLog.log(
          (String) "Mysterium Patches",
          (Level) Level.ERROR,
          (String) "Error occured when making runTransformers in LaunchClassLoader usable.",
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
  }
}
