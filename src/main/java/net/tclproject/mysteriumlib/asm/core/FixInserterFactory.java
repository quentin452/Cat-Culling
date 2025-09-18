package net.tclproject.mysteriumlib.asm.core;

import org.objectweb.asm.MethodVisitor;

public abstract class FixInserterFactory {
  protected boolean priorityReversed = false;

  abstract FixInserter createFixInserter(
      MethodVisitor var1,
      int var2,
      String var3,
      String var4,
      ASMFix var5,
      FixInserterClassVisitor var6);

  public static class OnLineNumber extends FixInserterFactory {
    private int lineNumber;

    public OnLineNumber(int lineNumber) {
      this.lineNumber = lineNumber;
    }

    @Override
    public FixInserter createFixInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv) {
      return new FixInserter.OnLineNumberInserter(mv, access, name, desc, fix, cv, this.lineNumber);
    }
  }

  public static class OnExit extends FixInserterFactory {
    public static final OnExit INSTANCE = new OnExit();
    public boolean insertOnThrows;

    public OnExit() {
      this.priorityReversed = true;
      this.insertOnThrows = false;
    }

    public OnExit(boolean insertOnThrows) {
      this.insertOnThrows = insertOnThrows;
      this.priorityReversed = true;
    }

    @Override
    public FixInserter createFixInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv) {
      return new FixInserter.OnExitInserter(mv, access, name, desc, fix, cv, this.insertOnThrows);
    }
  }

  public static class OnEnter extends FixInserterFactory {
    public static final OnEnter INSTANCE = new OnEnter();

    @Override
    public FixInserter createFixInserter(
        MethodVisitor mv,
        int access,
        String name,
        String desc,
        ASMFix fix,
        FixInserterClassVisitor cv) {
      return new FixInserter.OnEnterInserter(mv, access, name, desc, fix, cv);
    }
  }
}
