package net.tclproject.mysteriumlib.asm.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class TargetClassTransformer {
  MiscUtils utils = new MiscUtils();
  public MiscUtils.LogHelper logger = this.utils.new SystemLogHelper();
  protected HashMap<String, List<ASMFix>> fixesMap = new HashMap();
  private FixParser containerParser = new FixParser(this);
  protected MetaReader metaReader = new MetaReader();

  public void registerFix(ASMFix fix) {
    if (this.fixesMap.containsKey(fix.getTargetClassName())) {
      this.fixesMap.get(fix.getTargetClassName()).add(fix);
    } else {
      ArrayList<ASMFix> list = new ArrayList<ASMFix>(2);
      list.add(fix);
      this.fixesMap.put(fix.getTargetClassName(), list);
    }
  }

  public void registerClassWithFixes(String className) {
    this.containerParser.parseForFixes(className);
  }

  public void registerClassWithFixes(byte[] classBytes) {
    this.containerParser.parseForFixes(classBytes);
  }

  public byte[] transform(String className, byte[] classBytes) {
    List<ASMFix> fixes = this.fixesMap.get(className);
    if (fixes != null) {
      Collections.sort(fixes);
      this.logger.debug("Injecting fixes into class " + className + ".");
      try {
        int javaVersion = (classBytes[6] & 0xFF) << 8 | classBytes[7] & 0xFF;
        boolean java7 = javaVersion > 50;
        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = this.createClassWriter(java7 ? 2 : 1);
        FixInserterClassVisitor fixInserterVisitor =
            this.createInserterClassVisitor(classWriter, fixes);
        classReader.accept(fixInserterVisitor, java7 ? 4 : 8);
        classBytes = classWriter.toByteArray();
        for (ASMFix fix : fixInserterVisitor.insertedFixes) {
          this.logger.debug("Fixed method " + fix.getFullTargetMethodName());
        }
        fixes.removeAll(fixInserterVisitor.insertedFixes);
      } catch (Exception e) {
        this.logger.severe(
            "A problem has occurred during transformation of class " + className + ".");
        this.logger.severe("Fixes to be applied to this class:");
        for (ASMFix fix : fixes) {
          this.logger.severe(fix.toString());
        }
        this.logger.severe("Stack trace:", e);
      }
      for (ASMFix notInserted : fixes) {
        if (notInserted.isMandatory()) {
          throw new RuntimeException("Can not find the target method of fatal fix: " + notInserted);
        }
        this.logger.warning("Can not find the target method of fix: " + notInserted);
      }
    }
    return classBytes;
  }

  public FixInserterClassVisitor createInserterClassVisitor(
      ClassWriter classWriter, List<ASMFix> fixes) {
    return new FixInserterClassVisitor(this, classWriter, fixes);
  }

  public ClassWriter createClassWriter(int flags) {
    MiscUtils miscUtils = this.utils;
    miscUtils.getClass();
    return this.utils.new SafeCommonSuperClassWriter(this.metaReader, flags);
  }
}
