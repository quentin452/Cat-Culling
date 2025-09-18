package net.tclproject.mysteriumlib.asm.core;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class FixInserter extends AdviceAdapter {
  protected final ASMFix fix;
  protected final FixInserterClassVisitor classVisitor;
  public final String methodName;
  public final Type methodType;
  public final boolean isStatic;

  protected FixInserter(
      MethodVisitor mv,
      int access,
      String name,
      String descriptor,
      ASMFix fix,
      FixInserterClassVisitor classVisitor) {
    super(327680, mv, access, name, descriptor);
    this.fix = fix;
    this.classVisitor = classVisitor;
    this.isStatic = (access & 8) != 0;
    this.methodName = name;
    this.methodType = Type.getMethodType(descriptor);
  }

  protected final void insertFix() {
    if (!this.classVisitor.visitingFix) {
      this.classVisitor.visitingFix = true;
      this.fix.insertFix(this);
      this.classVisitor.visitingFix = false;
    }
  }

  public static class OnLineNumberInserter extends FixInserter {
    private int lineNumber;

    public OnLineNumberInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv,
        int lineNumber) {
      super(mv, access, name, desc, fix, cv);
      this.lineNumber = lineNumber;
    }

    @Override
    public void visitLineNumber(int lineVisiting, Label start) {
      super.visitLineNumber(lineVisiting, start);
      if (lineVisiting == this.lineNumber) {
        this.insertFix();
      }
    }
  }

  public static class OnExitInserter extends FixInserter {
    public boolean insertOnThrows;

    public OnExitInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv) {
      super(mv, access, name, desc, fix, cv);
      this.insertOnThrows = false;
    }

    public OnExitInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv,
        boolean insertOnThrows) {
      super(mv, access, name, desc, fix, cv);
      this.insertOnThrows = insertOnThrows;
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != 191 || this.insertOnThrows) {
        this.insertFix();
      }
    }
  }

  public static class OnEnterInserter extends FixInserter {
    public OnEnterInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv) {
      super(mv, access, name, desc, fix, cv);
    }

    @Override
    protected void onMethodEnter() {
      this.insertFix();
    }
  }
}
