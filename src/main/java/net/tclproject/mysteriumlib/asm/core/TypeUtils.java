package net.tclproject.mysteriumlib.asm.core;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class TypeUtils {
  private static final Map<String, Type> primitives = new HashMap<String, Type>(9);

  public static Type getType(String name) {
    return TypeUtils.getArrayType(name, 0);
  }

  public static Type getArrayType(String name) {
    return TypeUtils.getArrayType(name, 1);
  }

  public static Type getArrayType(String name, int arrayDimensions) {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < arrayDimensions; ++i) {
      stringBuilder.append("[");
    }
    Type primitive = primitives.get(name);
    if (primitive == null) {
      stringBuilder.append("L");
      stringBuilder.append(name.replace(".", "/"));
      stringBuilder.append(";");
    } else {
      stringBuilder.append(primitive.getDescriptor());
    }
    return Type.getType(stringBuilder.toString());
  }

  public static Object getStackMapFormat(Type type) {
    if (type == Type.BOOLEAN_TYPE
        || type == Type.BYTE_TYPE
        || type == Type.SHORT_TYPE
        || type == Type.CHAR_TYPE
        || type == Type.INT_TYPE) {
      return Opcodes.INTEGER;
    }
    if (type == Type.FLOAT_TYPE) {
      return Opcodes.FLOAT;
    }
    if (type == Type.DOUBLE_TYPE) {
      return Opcodes.DOUBLE;
    }
    if (type == Type.LONG_TYPE) {
      return Opcodes.LONG;
    }
    return type.getInternalName();
  }

  static {
    primitives.put("void", Type.VOID_TYPE);
    primitives.put("boolean", Type.BOOLEAN_TYPE);
    primitives.put("byte", Type.BYTE_TYPE);
    primitives.put("short", Type.SHORT_TYPE);
    primitives.put("char", Type.CHAR_TYPE);
    primitives.put("int", Type.INT_TYPE);
    primitives.put("float", Type.FLOAT_TYPE);
    primitives.put("long", Type.LONG_TYPE);
    primitives.put("double", Type.DOUBLE_TYPE);
  }
}
