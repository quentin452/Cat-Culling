package net.tclproject.mysteriumlib.asm.common;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.launchwrapper.IClassTransformer;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.FixInserterClassVisitor;
import net.tclproject.mysteriumlib.asm.core.TargetClassTransformer;
import org.objectweb.asm.ClassWriter;

public class CustomClassTransformer extends TargetClassTransformer implements IClassTransformer {
  static CustomClassTransformer instance;
  private Map<Integer, String> methodsMap;
  private static List<IClassTransformer> postTransformers;

  public CustomClassTransformer() {
    instance = this;
    if (CustomLoadingPlugin.isObfuscated()) {
      try {
        long timeStart = System.currentTimeMillis();
        this.methodsMap = this.loadMethods();
        long time = System.currentTimeMillis() - timeStart;
        this.logger.debug("Methods dictionary loaded in " + time + " ms");
      } catch (IOException e) {
        this.logger.severe("Can not load obfuscated method names", e);
      }
    }
    this.metaReader = CustomLoadingPlugin.getMetaReader();
    this.fixesMap.putAll(FirstClassTransformer.instance.getFixesMap());
    FirstClassTransformer.instance.getFixesMap().clear();
    FirstClassTransformer.instance.registeredBuiltinFixes = true;
  }

  private HashMap<Integer, String> loadMethods() throws IOException {
    InputStream resourceStream = this.getClass().getResourceAsStream("/methods.bin");
    if (resourceStream == null) {
      throw new IOException("Methods dictionary not found.");
    }
    DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
    int numMethods = input.readInt();
    HashMap<Integer, String> map = new HashMap<Integer, String>(numMethods);
    for (int i = 0; i < numMethods; ++i) {
      map.put(input.readInt(), input.readUTF());
    }
    input.close();
    return map;
  }

  public byte[] transform(String name, String deobfName, byte[] bytecode) {
    bytecode = this.transform(deobfName, bytecode);
    for (int i = 0; i < postTransformers.size(); ++i) {
      bytecode = postTransformers.get(i).transform(name, deobfName, bytecode);
    }
    return bytecode;
  }

  @Override
  public FixInserterClassVisitor createInserterClassVisitor(
      ClassWriter classWriter, List<ASMFix> fixes) {
    return new FixInserterClassVisitor(this, classWriter, fixes) {

      @Override
      protected boolean isTheTarget(ASMFix fix, String name, String descriptor) {
        String deobfName;
        if (CustomLoadingPlugin.isObfuscated()
            && (deobfName =
                    (String)
                        CustomClassTransformer.this.methodsMap.get(
                            CustomClassTransformer.getMethodIndex(name)))
                != null
            && super.isTheTarget(fix, deobfName, descriptor)) {
          return true;
        }
        return super.isTheTarget(fix, name, descriptor);
      }
    };
  }

  public Map<Integer, String> getMethodNames() {
    return this.methodsMap;
  }

  public static int getMethodIndex(String srgName) {
    if (srgName.startsWith("func_")) {
      int first = srgName.indexOf(95);
      int second = srgName.indexOf(95, first + 1);
      return Integer.valueOf(srgName.substring(first + 1, second));
    }
    return -1;
  }

  public static void registerPostTransformer(IClassTransformer transformer) {
    postTransformers.add(transformer);
  }

  static {
    postTransformers = new ArrayList<IClassTransformer>();
  }
}
