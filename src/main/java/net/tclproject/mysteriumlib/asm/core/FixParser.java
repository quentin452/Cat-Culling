package net.tclproject.mysteriumlib.asm.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnType;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.annotations.FixOrder;
import net.tclproject.mysteriumlib.asm.annotations.LocalVariable;
import net.tclproject.mysteriumlib.asm.annotations.ReturnedValue;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class FixParser {
  private TargetClassTransformer transformer;
  private String fixesClassName;
  private String currentFixMethodName;
  private String currentFixMethodDescriptor;
  private boolean currentMethodIsPublicAndStatic;
  private HashMap<String, Object> annotationValues;
  private HashMap<Integer, Integer> argumentAnnotations = new HashMap();
  private boolean inFixAnnotation;
  private static final String fixDescriptor = Type.getDescriptor(Fix.class);
  private static final String localVariableDescriptor = Type.getDescriptor(LocalVariable.class);
  private static final String returnedValueDescriptor = Type.getDescriptor(ReturnedValue.class);

  public FixParser(TargetClassTransformer transformer) {
    this.transformer = transformer;
  }

  protected void parseForFixes(String className) {
    this.transformer.logger.debug("Parsing class with fix methods " + className);
    try {
      this.transformer.metaReader.acceptVisitor(className, (ClassVisitor) new FixClassVisitor());
    } catch (IOException e) {
      this.transformer.logger.severe("Can not parse class with fix methods " + className, e);
    }
  }

  protected void parseForFixes(byte[] classBytes) {
    FixClassVisitor fixMethodSearchClassVisitor = new FixClassVisitor();
    try {
      this.transformer.metaReader.acceptVisitor(
          classBytes, (ClassVisitor) fixMethodSearchClassVisitor);
      this.transformer.logger.debug(
          "Parsing class with fix methods " + fixMethodSearchClassVisitor.fixesClassName);
    } catch (Exception e) {
      this.transformer.logger.severe(
          fixMethodSearchClassVisitor.fixesClassName != ""
              ? "Can not parse class with fix methods " + fixMethodSearchClassVisitor.fixesClassName
              : "Can not create a class visitor to search a class for fix " + "methods.",
          e);
    }
  }

  private void warnInvalidFix(String message) {
    this.transformer.logger.warning(
        "Found invalid fix " + this.fixesClassName + "#" + this.currentFixMethodName);
    this.transformer.logger.warning(message);
  }

  private void createAndRegisterFix(String clsName) {
    ASMFix.Builder builder;
    block29:
    {
      Type methodType;
      block28:
      {
        builder = ASMFix.newBuilder();
        methodType = Type.getMethodType(this.currentFixMethodDescriptor);
        Type[] argumentTypes = methodType.getArgumentTypes();
        if (!this.currentMethodIsPublicAndStatic) {
          this.warnInvalidFix("Fix method must be public and static.");
          return;
        }
        if (argumentTypes.length < 1) {
          this.warnInvalidFix(
              "Fix method has no arguments. First argument of a fix method must "
                  + "be a of the type of the target class.");
          return;
        }
        if (argumentTypes[0].getSort() != 10) {
          this.warnInvalidFix(
              "First argument of the fix method is not an object. First argument "
                  + "of a fix method must be of the type of the target class.");
          return;
        }
        builder.setTargetClass(argumentTypes[0].getClassName());
        if (this.annotationValues.containsKey("targetMethod")) {
          builder.setTargetMethod((String) this.annotationValues.get("targetMethod"));
        } else {
          builder.setTargetMethod(this.currentFixMethodName);
        }
        builder.setFixesClass(clsName);
        builder.setFixMethod(this.currentFixMethodName);
        builder.addThisToFixMethodParameters();
        boolean insertOnExit = Boolean.TRUE.equals(this.annotationValues.get("insertOnExit"));
        int currentParameterId = 1;
        for (int i = 1; i < argumentTypes.length; ++i) {
          Type currentArgumentType = argumentTypes[i];
          if (this.argumentAnnotations.containsKey(i)) {
            int stackIndexToBePassed = this.argumentAnnotations.get(i);
            if (stackIndexToBePassed == -1) {
              builder.setTargetMethodReturnType(currentArgumentType);
              builder.addReturnedValueToFixMethodParameters();
              continue;
            }
            builder.addFixMethodParameter(currentArgumentType, stackIndexToBePassed);
            continue;
          }
          builder.addTargetMethodParameters(currentArgumentType);
          builder.addFixMethodParameter(currentArgumentType, currentParameterId);
          currentParameterId +=
              currentArgumentType == Type.LONG_TYPE || currentArgumentType == Type.DOUBLE_TYPE
                  ? 2
                  : 1;
        }
        if (insertOnExit) {
          builder.setInjectorFactory(ASMFix.ON_EXIT_FACTORY);
        }
        if (this.annotationValues.containsKey("insertOnLine")) {
          int lineToBeInsertedOn = (Integer) this.annotationValues.get("insertOnLine");
          builder.setInjectorFactory(new FixInserterFactory.OnLineNumber(lineToBeInsertedOn));
        }
        if (this.annotationValues.containsKey("returnedType")) {
          builder.setTargetMethodReturnType((String) this.annotationValues.get("returnedType"));
        }
        EnumReturnSetting EnumReturnSetting2 = EnumReturnSetting.NEVER;
        if (this.annotationValues.containsKey("returnSetting")) {
          EnumReturnSetting2 =
              EnumReturnSetting.valueOf((String) this.annotationValues.get("returnSetting"));
          builder.setReturnSetting(EnumReturnSetting2);
        }
        if (EnumReturnSetting2 != EnumReturnSetting.NEVER) {
          Object primitiveConstant = this.getAlwaysReturnedValue();
          if (primitiveConstant != null) {
            builder.setReturnType(EnumReturnType.PRIMITIVE_CONSTANT);
            builder.setPrimitiveAlwaysReturned(primitiveConstant);
          } else if (Boolean.TRUE.equals(this.annotationValues.get("nullReturned"))) {
            builder.setReturnType(EnumReturnType.NULL);
          } else if (this.annotationValues.containsKey("anotherMethodReturned")) {
            builder.setReturnType(EnumReturnType.ANOTHER_METHOD_RETURN_VALUE);
            builder.setReturnMethod((String) this.annotationValues.get("anotherMethodReturned"));
          } else if (methodType.getReturnType() != Type.VOID_TYPE) {
            builder.setReturnType(EnumReturnType.FIX_METHOD_RETURN_VALUE);
          }
        }
        builder.setFixMethodReturnType(methodType.getReturnType());
        if (EnumReturnSetting2 == EnumReturnSetting.ON_TRUE
            && methodType.getReturnType() != Type.BOOLEAN_TYPE) {
          this.warnInvalidFix(
              "Fix method must return boolean if returnSetting is ON_TRUE. (if "
                  + "we only return our custom value/ the original value if the fix "
                  + "method returns true, how do we know if it's true if it's not a "
                  + "boolean?)");
          return;
        }
        if (EnumReturnSetting2 == EnumReturnSetting.ON_NULL) break block28;
        if (EnumReturnSetting2 != EnumReturnSetting.ON_NOT_NULL) break block29;
      }
      if (methodType.getReturnType().getSort() != 10 && methodType.getReturnType().getSort() != 9) {
        this.warnInvalidFix(
            "Fix method must return object if returnSetting is ON_NULL or "
                + "ON_NOT_NULL. (if we only return our custom value/ the original "
                + "value if the fix method returns a null/ non null object, how do "
                + "we know if it's a null/ not null object if it's not an object?)");
        return;
      }
    }
    if (this.annotationValues.containsKey("order")) {
      builder.setPriority(FixOrder.valueOf((String) this.annotationValues.get("order")));
    }
    if (this.annotationValues.containsKey("createNewMethod")) {
      builder.setCreateMethod(Boolean.TRUE.equals(this.annotationValues.get("createNewMethod")));
    }
    if (this.annotationValues.containsKey("isFatal")) {
      builder.setFatal(Boolean.TRUE.equals(this.annotationValues.get("isFatal")));
    }
    this.transformer.registerFix(builder.build());
  }

  private Object getAlwaysReturnedValue() {
    for (Map.Entry<String, Object> entry : this.annotationValues.entrySet()) {
      if (!entry.getKey().endsWith("AlwaysReturned")) continue;
      return entry.getValue();
    }
    return null;
  }

  private class FixAnnotationVisitor extends AnnotationVisitor {
    public FixAnnotationVisitor() {
      super(327680);
    }

    @Override
    public void visit(String name, Object value) {
      if (FixParser.this.inFixAnnotation) {
        FixParser.this.annotationValues.put(name, value);
      }
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
      this.visit(name, value);
    }

    @Override
    public void visitEnd() {
      FixParser.this.inFixAnnotation = false;
    }
  }

  private class FixMethodVisitor extends MethodVisitor {
    String clsName;

    public FixMethodVisitor(String className) {
      super(327680);
      this.clsName = className;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
      if (fixDescriptor.equals(descriptor)) {
        FixParser.this.annotationValues = new HashMap();
        FixParser.this.inFixAnnotation = true;
      }
      return new FixAnnotationVisitor();
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(
        final int indexOfArgument, String descriptor, boolean visible) {
      if (returnedValueDescriptor.equals(descriptor)) {
        FixParser.this.argumentAnnotations.put(indexOfArgument, -1);
      }
      if (localVariableDescriptor.equals(descriptor)) {
        return new AnnotationVisitor(327680) {
          @Override
          public void visit(String name, Object value) {
            FixParser.this.argumentAnnotations.put(indexOfArgument, (Integer) value);
          }
        };
      }
      return null;
    }

    @Override
    public void visitEnd() {
      if (FixParser.this.annotationValues != null) {
        FixParser.this.createAndRegisterFix(this.clsName);
      }
      FixParser.this.argumentAnnotations.clear();
      FixParser.this.currentFixMethodName = null;
      FixParser.this.currentFixMethodDescriptor = null;
      FixParser.this.currentMethodIsPublicAndStatic = false;
      FixParser.this.annotationValues = null;
    }
  }

  private class FixClassVisitor extends ClassVisitor {
    String fixesClassName;

    public FixClassVisitor() {
      super(327680);
      this.fixesClassName = "";
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.fixesClassName = name.replace('/', '.');
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String desc, String signature, String[] exceptions) {
      FixParser.this.currentFixMethodName = name;
      FixParser.this.currentFixMethodDescriptor = desc;
      FixParser.this.currentMethodIsPublicAndStatic = (access & 1) != 0 && (access & 8) != 0;
      return new FixMethodVisitor(this.fixesClassName);
    }
  }
}
