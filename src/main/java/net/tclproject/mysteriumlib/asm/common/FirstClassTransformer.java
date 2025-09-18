package net.tclproject.mysteriumlib.asm.common;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import java.util.HashMap;
import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.FixInserterClassVisitor;
import net.tclproject.mysteriumlib.asm.core.TargetClassTransformer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

public class FirstClassTransformer extends TargetClassTransformer implements IClassTransformer {
  public static FirstClassTransformer instance = new FirstClassTransformer();
  boolean registeredBuiltinFixes;

  public FirstClassTransformer() {
    this.metaReader = CustomLoadingPlugin.getMetaReader();
    if (instance != null) {
      this.fixesMap.putAll(instance.getFixesMap());
      instance.getFixesMap().clear();
    } else {
      this.registerClassWithFixes(BuiltinFixes.class.getName());
    }
    instance = this;
  }

  public byte[] transform(String name, String deobfName, byte[] bytes) {
    return this.transform(deobfName, bytes);
  }

  @Override
  public FixInserterClassVisitor createInserterClassVisitor(
      ClassWriter classWriter, List<ASMFix> fixes) {
    return new FixInserterClassVisitor(this, classWriter, fixes) {

      @Override
      protected boolean isTheTarget(ASMFix fix, String name, String descriptor) {
        return super.isTheTarget(fix, name, FirstClassTransformer.obfuscateDescriptor(descriptor));
      }
    };
  }

  public HashMap<String, List<ASMFix>> getFixesMap() {
    return this.fixesMap;
  }

  static String obfuscateDescriptor(String descriptor) {
    if (!CustomLoadingPlugin.isObfuscated()) {
      return descriptor;
    }
    Type methodType = Type.getMethodType(descriptor);
    Type mappedReturnType = FirstClassTransformer.map(methodType.getReturnType());
    Type[] argTypes = methodType.getArgumentTypes();
    Type[] mappedArgTypes = new Type[argTypes.length];
    for (int i = 0; i < mappedArgTypes.length; ++i) {
      mappedArgTypes[i] = FirstClassTransformer.map(argTypes[i]);
    }
    return Type.getMethodDescriptor(mappedReturnType, mappedArgTypes);
  }

  static Type map(Type type) {
    if (!CustomLoadingPlugin.isObfuscated()) {
      return type;
    }
    if (type.getSort() < 9) {
      return type;
    }
    if (type.getSort() == 9) {
      boolean isPrimitiveArray;
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < type.getDimensions(); ++i) {
        sb.append("[");
      }
      boolean bl = isPrimitiveArray = type.getSort() < 9;
      if (!isPrimitiveArray) {
        sb.append("L");
      }
      sb.append(FirstClassTransformer.map(type.getElementType()).getInternalName());
      if (!isPrimitiveArray) {
        sb.append(";");
      }
      return Type.getType(sb.toString());
    }
    if (type.getSort() == 10) {
      String unmappedName = FMLDeobfuscatingRemapper.INSTANCE.map(type.getInternalName());
      return Type.getType("L" + unmappedName + ";");
    }
    throw new IllegalArgumentException("Can not map method type!");
  }
}
