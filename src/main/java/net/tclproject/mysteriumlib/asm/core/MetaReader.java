package net.tclproject.mysteriumlib.asm.core;

import cpw.mods.fml.common.FMLLog;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class MetaReader {
  private static Method findLoadedClass;

  public List<String> getLocalVariables(
      byte[] classBytes, final String methodName, Type... argumentTypes) {
    final ArrayList<String> localVariables = new ArrayList<String>();
    String methodDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, argumentTypes);
    final String methodDescriptorWithoutReturnType =
        methodDescriptor.substring(0, methodDescriptor.length() - 1);
    ClassVisitor classVisitor =
        new ClassVisitor(327680) {

          @Override
          public MethodVisitor visitMethod(
              final int access,
              String name,
              String descriptor,
              String signature,
              String[] exceptions) {
            if (methodName.equals(name)
                && descriptor.startsWith(methodDescriptorWithoutReturnType)) {
              return new MethodVisitor(327680) {

                @Override
                public void visitLocalVariable(
                    String name,
                    String descriptor,
                    String signature,
                    Label start,
                    Label end,
                    int index) {
                  String typeName = Type.getType(descriptor).getClassName();
                  int fixedIndex = index + ((access & 8) == 0 ? 0 : 1);
                  localVariables.add(fixedIndex + ": " + typeName + " " + name);
                }
              };
            }
            return null;
          }
        };
    this.acceptVisitor(classBytes, classVisitor);
    return localVariables;
  }

  public List<String> getLocalVariables(String className, String methodName, Type... argTypes)
      throws IOException {
    return this.getLocalVariables(this.classToBytes(className), methodName, argTypes);
  }

  public void printLocalVariables(byte[] classBytes, String methodName, Type... argumentTypes) {
    List<String> locals = this.getLocalVariables(classBytes, methodName, argumentTypes);
    for (String str : locals) {
      System.out.println(str);
    }
  }

  public void printLocalVariables(String className, String methodName, Type... argumentTypes)
      throws IOException {
    this.printLocalVariables(this.classToBytes(className), methodName, argumentTypes);
  }

  public static InputStream classToStream(String name) {
    String classResourceName = '/' + name.replace('.', '/') + ".class";
    return MetaReader.class.getResourceAsStream(classResourceName);
  }

  public byte[] classToBytes(String name) throws IOException {
    String classLocationName = '/' + name.replace('.', '/') + ".class";
    return IOUtils.toByteArray(
        (InputStream) MetaReader.class.getResourceAsStream(classLocationName));
  }

  public void acceptVisitor(byte[] classBytes, ClassVisitor visitor) {
    new ClassReader(classBytes).accept(visitor, 0);
  }

  public void acceptVisitor(String name, ClassVisitor visitor) throws IOException {
    this.acceptVisitor(this.classToBytes(name), visitor);
  }

  public static void acceptVisitor(InputStream classStream, ClassVisitor visitor) {
    try {
      ClassReader reader = new ClassReader(classStream);
      reader.accept(visitor, 0);
      classStream.close();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public MethodReference findMethod(String owner, String methodName, String descriptor) {
    ArrayList<String> superClasses = this.getSuperClasses(owner);
    for (int i = superClasses.size() - 1; i > 0; --i) {
      String className = superClasses.get(i);
      MethodReference methodReference = this.getMethodReference(className, methodName, descriptor);
      if (methodReference == null) continue;
      return methodReference;
    }
    return null;
  }

  public MethodReference getMethodReference(
      String className, String methodName, String descriptor) {
    try {
      return this.getMethodReferenceASM(className, methodName, descriptor);
    } catch (Exception e) {
      return this.getMethodReferenceReflect(className, methodName, descriptor);
    }
  }

  public MethodReference getMethodReferenceASM(
      String className, String methodName, String descriptor) throws IOException {
    FindMethodClassVisitor cv = new FindMethodClassVisitor(methodName, descriptor);
    this.acceptVisitor(className, (ClassVisitor) cv);
    if (cv.found) {
      return new MethodReference(className, cv.targetName, cv.targetDescriptor);
    }
    return null;
  }

  public MethodReference getMethodReferenceReflect(
      String className, String methodName, String descriptor) {
    Class loadedClass = this.getLoadedClass(className);
    if (loadedClass != null) {
      for (Method m : loadedClass.getDeclaredMethods()) {
        if (!this.checkSameMethod(methodName, descriptor, m.getName(), Type.getMethodDescriptor(m)))
          continue;
        return new MethodReference(className, m.getName(), Type.getMethodDescriptor(m));
      }
    }
    return null;
  }

  public boolean checkSameMethod(
      String sourceName, String sourceDesc, String targetName, String targetDesc) {
    return sourceName.equals(targetName) && sourceDesc.equals(targetDesc);
  }

  public ArrayList<String> getSuperClasses(String name) {
    ArrayList<String> superClasses = new ArrayList<String>(1);
    superClasses.add(name);
    while ((name = this.getSuperClass(name)) != null) {
      superClasses.add(name);
    }
    Collections.reverse(superClasses);
    return superClasses;
  }

  public Class getLoadedClass(String name) {
    if (findLoadedClass != null) {
      try {
        ClassLoader classLoader = MetaReader.class.getClassLoader();
        return (Class) findLoadedClass.invoke(classLoader, name.replace('/', '.'));
      } catch (Exception e) {
        FMLLog.log(
            (String) "Mysterium Patches",
            (Level) Level.ERROR,
            (String) "Error occured when getting a class from a name.",
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
    return null;
  }

  public String getSuperClass(String name) {
    try {
      return this.getSuperClassASM(name);
    } catch (Exception e) {
      return this.getSuperClassReflect(name);
    }
  }

  public String getSuperClassASM(String name) throws IOException {
    CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
    this.acceptVisitor(name, (ClassVisitor) cv);
    return cv.superClassName;
  }

  public String getSuperClassReflect(String name) {
    Class loadedClass = this.getLoadedClass(name);
    if (loadedClass != null) {
      if (loadedClass.getSuperclass() == null) {
        return null;
      }
      return loadedClass.getSuperclass().getName().replace('.', '/');
    }
    return "java/lang/Object";
  }

  static {
    try {
      findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
      findLoadedClass.setAccessible(true);
    } catch (NoSuchMethodException e) {
      FMLLog.log(
          (String) "Mysterium Patches",
          (Level) Level.ERROR,
          (String) "Error occured when making findLoadedClass in ClassLoader usable.",
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

  public static class MethodReference {
    public final String owner;
    public final String name;
    public final String descriptor;

    public MethodReference(String owner, String name, String descriptor) {
      this.owner = owner;
      this.name = name;
      this.descriptor = descriptor;
    }

    public Type getReturnType() {
      return Type.getMethodType(this.descriptor);
    }

    public String toString() {
      return "MethodReference{owner='"
          + this.owner
          + '\''
          + ", name='"
          + this.name
          + '\''
          + ", desc='"
          + this.descriptor
          + '\''
          + '}';
    }
  }

  protected class FindMethodClassVisitor extends ClassVisitor {
    public String targetName;
    public String targetDescriptor;
    public boolean found;

    public FindMethodClassVisitor(String name, String desctiptor) {
      super(327680);
      this.targetName = name;
      this.targetDescriptor = desctiptor;
    }

    @Override
    public MethodVisitor visitMethod(
        int access, String name, String desctiptor, String signature, String[] exceptions) {
      if ((access & 2) == 0
          && MetaReader.this.checkSameMethod(
              name, desctiptor, this.targetName, this.targetDescriptor)) {
        this.found = true;
        this.targetName = name;
        this.targetDescriptor = desctiptor;
      }
      return null;
    }
  }

  protected class CheckSuperClassVisitor extends ClassVisitor {
    String superClassName;

    public CheckSuperClassVisitor() {
      super(327680);
    }

    @Override
    public void visit(
        int version,
        int access,
        String name,
        String signature,
        String superName,
        String[] interfaces) {
      this.superClassName = superName;
    }
  }
}
