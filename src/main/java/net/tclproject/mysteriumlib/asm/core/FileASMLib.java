package net.tclproject.mysteriumlib.asm.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class FileASMLib {
  File originalClasses = new File("classes");
  File fixesDir = new File("fixes");

  public static void main(String[] args) throws IOException {
    new FileASMLib().transform();
  }

  void transform() throws IOException {
    TargetClassTransformer transformer = new TargetClassTransformer();
    for (File file : FileASMLib.getFiles(".class", this.fixesDir)) {
      transformer.registerClassWithFixes(FileUtils.readFileToByteArray((File) file));
    }
    for (File file : FileASMLib.getFiles(".class", this.originalClasses)) {
      byte[] bytes = IOUtils.toByteArray((InputStream) new FileInputStream(file));
      String className = "";
      byte[] byArray = transformer.transform(className, bytes);
    }
  }

  private static List<File> getFiles(String extension, File directory) throws IOException {
    ArrayList<File> files = new ArrayList<File>();
    File[] filesArray = directory.listFiles();
    if (filesArray != null) {
      for (File file : directory.listFiles()) {
        if (file.isDirectory()) {
          files.addAll(FileASMLib.getFiles(extension, file));
          continue;
        }
        if (!file.getName().toLowerCase().endsWith(extension)) continue;
        files.add(file);
      }
    }
    return files;
  }
}
