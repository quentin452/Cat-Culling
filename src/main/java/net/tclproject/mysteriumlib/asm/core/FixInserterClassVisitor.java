package net.tclproject.mysteriumlib.asm.core;

import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

public class FixInserterClassVisitor extends ClassVisitor {
  List<ASMFix> fixes;
  List<ASMFix> insertedFixes = new ArrayList<ASMFix>(1);
  boolean visitingFix;
  TargetClassTransformer transformer;
  String superName;

  public FixInserterClassVisitor(
      TargetClassTransformer transformer, ClassWriter cv, List<ASMFix> fixs) {
    super(327680, cv);
    this.fixes = fixs;
    this.transformer = transformer;
  }

  @Override
  public void visit(
      int version,
      int access,
      String name,
      String signature,
      String superName,
      String[] interfaces) {
    this.superName = superName;
    super.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    for (ASMFix fix : this.fixes) {
      if (!this.isTheTarget(fix, name, desc) || this.insertedFixes.contains(fix)) continue;
      mv = fix.getInjectorFactory().createFixInserter(mv, access, name, desc, fix, this);
      this.insertedFixes.add(fix);
    }
    return mv;
  }

  @Override
  public void visitEnd() {
    for (ASMFix fix : this.fixes) {
      if (!fix.getCreateMethod() || this.insertedFixes.contains(fix)) continue;
      fix.createMethod(this);
    }
    super.visitEnd();
  }

  protected boolean isTheTarget(ASMFix fix, String name, String desc) {
    return fix.isTheTarget(name, desc);
  }
}
