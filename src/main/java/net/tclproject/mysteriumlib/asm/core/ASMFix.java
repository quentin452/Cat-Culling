package net.tclproject.mysteriumlib.asm.core;

import java.util.ArrayList;
import java.util.List;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnType;
import net.tclproject.mysteriumlib.asm.annotations.FixOrder;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class ASMFix implements Cloneable, Comparable<ASMFix> {
  public List<Type> targetMethodArguments = new ArrayList<Type>(2);
  public List<Integer> transmittableVariableIndexes = new ArrayList<Integer>(2);
  public List<Type> fixMethodArguments = new ArrayList<Type>(2);
  public Type fixMethodReturnType = Type.VOID_TYPE;
  public Type targetMethodReturnType;
  public FixOrder priority = FixOrder.USUAL;
  EnumReturnType EnumReturnType = net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.VOID;
  EnumReturnSetting EnumReturnSetting =
      net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.NEVER;
  public FixInserterFactory injectorFactory = ON_ENTER_FACTORY;
  public static final FixInserterFactory ON_ENTER_FACTORY = FixInserterFactory.OnEnter.INSTANCE;
  public static final FixInserterFactory ON_EXIT_FACTORY = FixInserterFactory.OnExit.INSTANCE;
  private Object primitiveAlwaysReturned;
  public String targetClassName;
  public String targetMethodName;
  public String classWithFixes;
  public String fixMethodName;
  public String targetMethodDescriptor;
  public String fixMethodDescriptor;
  public String returnMethodName;
  public String returnMethodDescriptor;
  public boolean hasReturnedValueParameter;
  public boolean createMethod;
  public boolean isFatal;

  public String getTargetClassName() {
    return this.targetClassName;
  }

  public String getTargetClassInternalName() {
    return this.targetClassName.replace('.', '/');
  }

  public String getClassWithFixesInternalName() {
    return this.classWithFixes.replace('.', '/');
  }

  public boolean isTheTarget(String name, String descriptor) {
    return (this.targetMethodReturnType == null
                && descriptor.startsWith(this.targetMethodDescriptor)
            || descriptor.equals(this.targetMethodDescriptor))
        && name.equals(this.targetMethodName);
  }

  public boolean getCreateMethod() {
    return this.createMethod;
  }

  public boolean isMandatory() {
    return this.isFatal;
  }

  public FixInserterFactory getInjectorFactory() {
    return this.injectorFactory;
  }

  public boolean hasFixMethod() {
    return this.fixMethodName != null && this.classWithFixes != null;
  }

  public void createMethod(FixInserterClassVisitor classVisitor) {
    FixInserter inserter;
    MetaReader.MethodReference superMethod =
        classVisitor.transformer.metaReader.findMethod(
            this.getTargetClassInternalName(), this.targetMethodName, this.targetMethodDescriptor);
    MethodVisitor methodVisitor =
        classVisitor.visitMethod(
            1,
            superMethod == null ? this.targetMethodName : superMethod.name,
            this.targetMethodDescriptor,
            null,
            null);
    if (methodVisitor instanceof FixInserter) {
      inserter = (FixInserter) methodVisitor;
      inserter.visitCode();
      inserter.visitLabel(new Label());
      if (superMethod == null) {
        this.insertPushDefaultReturnValue(inserter, this.targetMethodReturnType);
      } else {
        this.insertSuperCall(inserter, superMethod);
      }
    } else {
      throw new IllegalArgumentException(
          "A fix inserter hasn't been created for this method, which means "
              + "the method isn't to be fixed. Likely, something is broken.");
    }
    this.insertReturn(inserter, this.targetMethodReturnType);
    inserter.visitLabel(new Label());
    inserter.visitMaxs(0, 0);
    inserter.visitEnd();
  }

  public void insertFix(FixInserter inserter) {
    Type targetMethodReturnType = inserter.methodType.getReturnType();
    int returnLocalIndex = -1;
    if (this.hasReturnedValueParameter) {
      returnLocalIndex = inserter.newLocal(targetMethodReturnType);
      inserter.visitVarInsn(targetMethodReturnType.getOpcode(54), returnLocalIndex);
    }
    int fixResultLocalIndex = -1;
    if (this.hasFixMethod()) {
      this.insertInvokeStatic(
          inserter, returnLocalIndex, this.fixMethodName, this.fixMethodDescriptor);
      if (this.EnumReturnType
              == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.FIX_METHOD_RETURN_VALUE
          || this.EnumReturnSetting.conditionRequiredToReturn) {
        fixResultLocalIndex = inserter.newLocal(this.fixMethodReturnType);
        inserter.visitVarInsn(this.fixMethodReturnType.getOpcode(54), fixResultLocalIndex);
      }
    }
    if (this.EnumReturnSetting
        != net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.NEVER) {
      Label label = inserter.newLabel();
      if (this.EnumReturnSetting
          != net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.ALWAYS) {
        inserter.visitVarInsn(this.fixMethodReturnType.getOpcode(21), fixResultLocalIndex);
        if (this.EnumReturnSetting
            == net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.ON_TRUE) {
          inserter.visitJumpInsn(153, label);
        } else if (this.EnumReturnSetting
            == net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.ON_NULL) {
          inserter.visitJumpInsn(199, label);
        } else if (this.EnumReturnSetting
            == net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.ON_NOT_NULL) {
          inserter.visitJumpInsn(198, label);
        }
      }
      if (this.EnumReturnType == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.NULL) {
        inserter.visitInsn(1);
      } else if (this.EnumReturnType
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.PRIMITIVE_CONSTANT) {
        inserter.visitLdcInsn(this.primitiveAlwaysReturned);
      } else if (this.EnumReturnType
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.FIX_METHOD_RETURN_VALUE) {
        inserter.visitVarInsn(this.fixMethodReturnType.getOpcode(21), fixResultLocalIndex);
      } else if (this.EnumReturnType
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType
              .ANOTHER_METHOD_RETURN_VALUE) {
        String returnMethodDescription = this.returnMethodDescriptor;
        if (returnMethodDescription.endsWith(")")) {
          returnMethodDescription =
              returnMethodDescription + targetMethodReturnType.getDescriptor();
        }
        this.insertInvokeStatic(
            inserter, returnLocalIndex, this.returnMethodName, returnMethodDescription);
      }
      this.insertReturn(inserter, targetMethodReturnType);
      inserter.visitLabel(label);
    }
    if (this.hasReturnedValueParameter) {
      this.insertLoad(inserter, targetMethodReturnType, returnLocalIndex);
    }
  }

  public void insertLoad(FixInserter inserter, Type parameterType, int variableIndex) {
    int opcode =
        parameterType == Type.INT_TYPE
                || parameterType == Type.BYTE_TYPE
                || parameterType == Type.CHAR_TYPE
                || parameterType == Type.BOOLEAN_TYPE
                || parameterType == Type.SHORT_TYPE
            ? 21
            : (parameterType == Type.LONG_TYPE
                ? 22
                : (parameterType == Type.FLOAT_TYPE
                    ? 23
                    : (parameterType == Type.DOUBLE_TYPE ? 24 : 25)));
    inserter.visitVarInsn(opcode, variableIndex);
  }

  public void insertSuperCall(FixInserter inserter, MetaReader.MethodReference method) {
    int variableIndex = 0;
    for (int i = 0; i <= this.targetMethodArguments.size(); ++i) {
      Type argumentType =
          i == 0 ? TypeUtils.getType(this.targetClassName) : this.targetMethodArguments.get(i - 1);
      this.insertLoad(inserter, argumentType, variableIndex);
      if (argumentType.getSort() == 8 || argumentType.getSort() == 7) {
        variableIndex += 2;
        continue;
      }
      ++variableIndex;
    }
    inserter.visitMethodInsn(183, method.owner, method.name, method.descriptor, false);
  }

  public void insertPushDefaultReturnValue(FixInserter inserter, Type targetMethodReturnType) {
    switch (targetMethodReturnType.getSort()) {
      case 0:
        {
          break;
        }
      case 1:
      case 2:
      case 3:
      case 4:
      case 5:
        {
          inserter.visitInsn(3);
          break;
        }
      case 6:
        {
          inserter.visitInsn(11);
          break;
        }
      case 7:
        {
          inserter.visitInsn(9);
          break;
        }
      case 8:
        {
          inserter.visitInsn(14);
          break;
        }
      default:
        {
          inserter.visitInsn(1);
        }
    }
  }

  public void insertReturn(FixInserter inserter, Type targetMethodReturnType) {
    if (targetMethodReturnType == Type.INT_TYPE
        || targetMethodReturnType == Type.SHORT_TYPE
        || targetMethodReturnType == Type.BOOLEAN_TYPE
        || targetMethodReturnType == Type.BYTE_TYPE
        || targetMethodReturnType == Type.CHAR_TYPE) {
      inserter.visitInsn(172);
    } else if (targetMethodReturnType == Type.LONG_TYPE) {
      inserter.visitInsn(173);
    } else if (targetMethodReturnType == Type.FLOAT_TYPE) {
      inserter.visitInsn(174);
    } else if (targetMethodReturnType == Type.DOUBLE_TYPE) {
      inserter.visitInsn(175);
    } else if (targetMethodReturnType == Type.VOID_TYPE) {
      inserter.visitInsn(177);
    } else {
      inserter.visitInsn(176);
    }
  }

  public void insertInvokeStatic(
      FixInserter inserter, int indexOfReturnArgument, String name, String descriptor) {
    for (int i = 0; i < this.fixMethodArguments.size(); ++i) {
      Type parameterType = this.fixMethodArguments.get(i);
      int variableIndex = this.transmittableVariableIndexes.get(i);
      if (inserter.isStatic) {
        if (variableIndex == 0) {
          inserter.visitInsn(1);
          continue;
        }
        if (variableIndex > 0) {
          --variableIndex;
        }
      }
      if (variableIndex == -1) {
        variableIndex = indexOfReturnArgument;
      }
      this.insertLoad(inserter, parameterType, variableIndex);
    }
    inserter.visitMethodInsn(184, this.getClassWithFixesInternalName(), name, descriptor, false);
  }

  public String getFullTargetMethodName() {
    return this.targetClassName + '#' + this.targetMethodName + this.targetMethodDescriptor;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ASMFix: ");
    sb.append(this.targetClassName).append('#').append(this.targetMethodName);
    sb.append(this.targetMethodDescriptor);
    sb.append(" -> ");
    sb.append(this.classWithFixes).append('#').append(this.fixMethodName);
    sb.append(this.fixMethodDescriptor);
    sb.append(", EnumReturnSetting=" + (Object) ((Object) this.EnumReturnSetting));
    sb.append(", EnumReturnType=" + (Object) ((Object) this.EnumReturnType));
    if (this.EnumReturnType
        == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.PRIMITIVE_CONSTANT) {
      sb.append(", Constant=" + this.primitiveAlwaysReturned);
    }
    sb.append(", InjectorFactory: " + this.injectorFactory.getClass().getName());
    sb.append(", CreateMethod = " + this.createMethod);
    return sb.toString();
  }

  @Override
  public int compareTo(ASMFix fix) {
    if (this.injectorFactory.priorityReversed && fix.injectorFactory.priorityReversed) {
      return this.priority.ordinal() > fix.priority.ordinal() ? -1 : 1;
    }
    if (!this.injectorFactory.priorityReversed && !fix.injectorFactory.priorityReversed) {
      return this.priority.ordinal() > fix.priority.ordinal() ? 1 : -1;
    }
    return this.injectorFactory.priorityReversed ? 1 : -1;
  }

  public static Builder newBuilder() {
    ASMFix aSMFix = new ASMFix();
    aSMFix.getClass();
    return aSMFix.new Builder();
  }

  public class Builder extends ASMFix {
    private Builder() {}

    public Builder setTargetClass(String name) {
      ASMFix.this.targetClassName = name;
      return this;
    }

    public Builder setTargetMethod(String name) {
      ASMFix.this.targetMethodName = name;
      return this;
    }

    public Builder addTargetMethodParameters(Type... argumentTypes) {
      for (Type type : argumentTypes) {
        ASMFix.this.targetMethodArguments.add(type);
      }
      return this;
    }

    public Builder addTargetMethodParameters(String... argumentTypeNames) {
      Type[] types = new Type[argumentTypeNames.length];
      for (int i = 0; i < argumentTypeNames.length; ++i) {
        types[i] = TypeUtils.getType(argumentTypeNames[i]);
      }
      return this.addTargetMethodParameters(types);
    }

    public Builder setTargetMethodReturnType(Type returnType) {
      ASMFix.this.targetMethodReturnType = returnType;
      return this;
    }

    public Builder setTargetMethodReturnType(String returnType) {
      return this.setTargetMethodReturnType(TypeUtils.getType(returnType));
    }

    public Builder setFixesClass(String name) {
      ASMFix.this.classWithFixes = name;
      return this;
    }

    public Builder setFixMethod(String name) {
      ASMFix.this.fixMethodName = name;
      return this;
    }

    public Builder addFixMethodParameter(Type parameterType, int variableIndex) {
      if (!ASMFix.this.hasFixMethod()) {
        throw new IllegalStateException(
            "Fix method is not specified, can't append argument to its " + "arguments list.");
      }
      ASMFix.this.fixMethodArguments.add(parameterType);
      ASMFix.this.transmittableVariableIndexes.add(variableIndex);
      return this;
    }

    public Builder addFixMethodParameter(String parameterTypeName, int variableIndex) {
      return this.addFixMethodParameter(TypeUtils.getType(parameterTypeName), variableIndex);
    }

    public Builder addThisToFixMethodParameters() {
      if (!ASMFix.this.hasFixMethod()) {
        throw new IllegalStateException(
            "Fix method is not specified, can't append argument to its " + "arguments list.");
      }
      ASMFix.this.fixMethodArguments.add(TypeUtils.getType(ASMFix.this.targetClassName));
      ASMFix.this.transmittableVariableIndexes.add(0);
      return this;
    }

    public Builder addReturnedValueToFixMethodParameters() {
      if (!ASMFix.this.hasFixMethod()) {
        throw new IllegalStateException(
            "Fix method is not specified, can't append argument to its " + "arguments list.");
      }
      if (ASMFix.this.targetMethodReturnType == Type.VOID_TYPE) {
        throw new IllegalStateException(
            "Target method's return type is void so it doesn't make sense to "
                + "transmit it's return value to the fix method, as frankly, there "
                + "is none.");
      }
      ASMFix.this.fixMethodArguments.add(ASMFix.this.targetMethodReturnType);
      ASMFix.this.transmittableVariableIndexes.add(-1);
      ASMFix.this.hasReturnedValueParameter = true;
      return this;
    }

    public Builder setReturnSetting(EnumReturnSetting setting) {
      Type returnType;
      if (setting.conditionRequiredToReturn && ASMFix.this.fixMethodName == null) {
        throw new IllegalArgumentException(
            "Fix method isn't specified, can't use a return condition that " + "depends on it.");
      }
      ASMFix.this.EnumReturnSetting = setting;
      switch (setting) {
        case NEVER:
        case ALWAYS:
          {
            returnType = Type.VOID_TYPE;
            break;
          }
        case ON_TRUE:
          {
            returnType = Type.BOOLEAN_TYPE;
            break;
          }
        default:
          {
            returnType = Type.getType(Object.class);
          }
      }
      ASMFix.this.fixMethodReturnType = returnType;
      return this;
    }

    public Builder setReturnType(EnumReturnType type) {
      this.classWithFixes = ASMFix.this.classWithFixes;
      this.fixMethodName = ASMFix.this.fixMethodName;
      if (ASMFix.this.EnumReturnSetting
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting.NEVER) {
        throw new IllegalStateException(
            "Current return condition is never, so it does not make sense to "
                + "specify the return value.");
      }
      Type returnType = ASMFix.this.targetMethodReturnType;
      if (type != net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.VOID
          && returnType == Type.VOID_TYPE) {
        throw new IllegalArgumentException(
            "Target method return type is void, so it does not make sense to "
                + "return anything else.");
      }
      if (type == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.VOID
          && returnType != Type.VOID_TYPE) {
        throw new IllegalArgumentException(
            "Target method return type is not void, so it is impossible to " + "return void.");
      }
      if (type == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.PRIMITIVE_CONSTANT
          && returnType != null
          && !this.isPrimitive(returnType)) {
        throw new IllegalArgumentException(
            "Target method return type isn't a primitive, so it is " + "impossible to return one.");
      }
      if (type == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.NULL
          && returnType != null
          && this.isPrimitive(returnType)) {
        throw new IllegalArgumentException(
            "Target method return type is a primitive, so it is impossible " + "to return null.");
      }
      if (type == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.FIX_METHOD_RETURN_VALUE
          && !this.hasFixMethod()) {
        throw new IllegalArgumentException(
            "Fix method is not specified, can't use it's return value.");
      }
      ASMFix.this.EnumReturnType = type;
      if (type
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.FIX_METHOD_RETURN_VALUE) {
        ASMFix.this.fixMethodReturnType = ASMFix.this.targetMethodReturnType;
      }
      return this;
    }

    public Type getFixMethodReturnType() {
      return this.fixMethodReturnType;
    }

    public void setFixMethodReturnType(Type type) {
      ASMFix.this.fixMethodReturnType = type;
    }

    public boolean isPrimitive(Type type) {
      return type.getSort() > 0 && type.getSort() < 9;
    }

    public Builder setPrimitiveAlwaysReturned(Object object) {
      if (ASMFix.this.EnumReturnType
          != net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.PRIMITIVE_CONSTANT) {
        throw new IllegalStateException(
            "Return type is not PRIMITIVE_CONSTANT, so it doesn't make sense "
                + "to specify that constant.");
      }
      Type returnType = ASMFix.this.targetMethodReturnType;
      if (returnType == Type.BOOLEAN_TYPE && !(object instanceof Boolean)
          || returnType == Type.CHAR_TYPE && !(object instanceof Character)
          || returnType == Type.BYTE_TYPE && !(object instanceof Byte)
          || returnType == Type.SHORT_TYPE && !(object instanceof Short)
          || returnType == Type.INT_TYPE && !(object instanceof Integer)
          || returnType == Type.LONG_TYPE && !(object instanceof Long)
          || returnType == Type.FLOAT_TYPE && !(object instanceof Float)
          || returnType == Type.DOUBLE_TYPE && !(object instanceof Double)) {
        throw new IllegalArgumentException(
            "Given object class does not match the target method's return " + "type.");
      }
      ASMFix.this.primitiveAlwaysReturned = object;
      return this;
    }

    public Builder setReturnMethod(String name) {
      if (ASMFix.this.EnumReturnType
          != net.tclproject.mysteriumlib.asm.annotations.EnumReturnType
              .ANOTHER_METHOD_RETURN_VALUE) {
        throw new IllegalStateException(
            "Return type is not ANOTHER_METHOD_RETURN_VALUE, so it does not "
                + "make sence to specify that method.");
      }
      ASMFix.this.returnMethodName = name;
      return this;
    }

    public Builder setInjectorFactory(FixInserterFactory factory) {
      ASMFix.this.injectorFactory = factory;
      return this;
    }

    public Builder setPriority(FixOrder priority) {
      ASMFix.this.priority = priority;
      return this;
    }

    public Builder setCreateMethod(boolean createMethod) {
      ASMFix.this.createMethod = createMethod;
      return this;
    }

    public Builder setFatal(boolean isMandatory) {
      ASMFix.this.isFatal = isMandatory;
      return this;
    }

    private String getMethodDescriptor(Type returnType, List<Type> argumentTypes) {
      Type[] paramTypesArray = argumentTypes.toArray(new Type[0]);
      if (returnType == null) {
        String voidDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, paramTypesArray);
        return voidDescriptor.substring(0, voidDescriptor.length() - 1);
      }
      return Type.getMethodDescriptor(returnType, paramTypesArray);
    }

    public ASMFix build() {
      ASMFix fix = ASMFix.this;
      if (fix.createMethod && fix.targetMethodReturnType == null) {
        fix.targetMethodReturnType = fix.fixMethodReturnType;
      }
      fix.targetMethodDescriptor =
          this.getMethodDescriptor(fix.targetMethodReturnType, fix.targetMethodArguments);
      if (fix.hasFixMethod()) {
        fix.fixMethodDescriptor =
            Type.getMethodDescriptor(
                fix.fixMethodReturnType, fix.fixMethodArguments.toArray(new Type[0]));
      }
      if (fix.EnumReturnType
          == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType
              .ANOTHER_METHOD_RETURN_VALUE) {
        fix.returnMethodDescriptor =
            this.getMethodDescriptor(fix.targetMethodReturnType, fix.fixMethodArguments);
      }
      try {
        fix = (ASMFix) ASMFix.this.clone();
      } catch (CloneNotSupportedException cloneNotSupportedException) {
        // empty catch block
      }
      if (fix.targetClassName == null) {
        throw new IllegalStateException(
            "Target class name is not specified. Call setTargetClassName() " + "before build().");
      }
      if (fix.targetMethodName == null) {
        throw new IllegalStateException(
            "Target method name is not specified. Call setTargetMethodName() " + "before build().");
      }
      if (fix.EnumReturnType
              == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType.PRIMITIVE_CONSTANT
          && fix.primitiveAlwaysReturned == null) {
        throw new IllegalStateException(
            "Return type is PRIMITIVE_CONSTANT, but the constant is not "
                + "specified. Call setReturnType() before build().");
      }
      if (fix.EnumReturnType
              == net.tclproject.mysteriumlib.asm.annotations.EnumReturnType
                  .ANOTHER_METHOD_RETURN_VALUE
          && fix.returnMethodName == null) {
        throw new IllegalStateException(
            "Return type is ANOTHER_METHOD_RETURN_VALUE, but the method is "
                + "not specified. Call setReturnMethod() before build().");
      }
      if (!(fix.injectorFactory instanceof FixInserterFactory.OnExit)
          && fix.hasReturnedValueParameter) {
        throw new IllegalStateException(
            "Can not pass the returned value to the fix method because the "
                + "fix is not inserted on exit.");
      }
      return fix;
    }
  }
}
