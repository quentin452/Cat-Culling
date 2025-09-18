package net.tclproject.mysteriumlib.asm.core;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassWriter;

public class MiscUtils {
  public static void generateMethodsDictionary() throws Exception {
    List<String> lines = FileUtils.readLines(new File("methods.csv"));
    lines.remove(0);
    HashMap<Integer, String> map = new HashMap<Integer, String>();
    for (String str : lines) {
      String[] splitted = str.split(",");
      int first = splitted[0].indexOf(95);
      int second = splitted[0].indexOf(95, first + 1);
      int id = Integer.valueOf(splitted[0].substring(first + 1, second));
      map.put(id, splitted[1]);
    }
    DataOutputStream out = new DataOutputStream(new FileOutputStream("methods.bin"));
    out.writeInt(map.size());
    for (Map.Entry entry : map.entrySet()) {
      out.writeInt((Integer) entry.getKey());
      out.writeUTF((String) entry.getValue());
    }
    out.close();
  }

  public class MinecraftLogHelper implements LogHelper {
    private Logger logger;

    public MinecraftLogHelper(Logger logger) {
      this.logger = logger;
    }

    @Override
    public void debug(String message) {
      this.logger.log(Level.FINE, message);
    }

    public void detailed(String message) {
      this.logger.log(Level.FINEST, message);
    }

    public void configInfo(String message) {
      this.logger.log(Level.CONFIG, message);
    }

    @Override
    public void warning(String message) {
      this.logger.log(Level.WARNING, message);
    }

    @Override
    public void severe(String message) {
      this.logger.log(Level.SEVERE, message);
    }

    @Override
    public void severe(String message, Throwable cause) {
      this.logger.log(Level.SEVERE, message, cause);
    }

    @Override
    public void info(String message) {
      this.logger.log(Level.INFO, message);
    }

    @Override
    public void fatal(String message) {
      this.logger.log(Level.SEVERE, "[---------------[!!!FATAL!!!]---------------]");
      this.logger.log(Level.SEVERE, message);
      this.logger.log(Level.SEVERE, "[---------------[!!!FATAL!!!]---------------]");
    }

    @Override
    public void fatal(String message, Throwable cause) {
      this.logger.log(Level.SEVERE, "[---------------[!!!FATAL!!!]---------------]");
      this.logger.log(Level.SEVERE, message, cause);
      this.logger.log(Level.SEVERE, "[---------------[!!!FATAL!!!]---------------]");
    }
  }

  public class SystemLogHelper implements LogHelper {
    @Override
    public void debug(String message) {
      System.out.println("[DEBUG] " + message);
    }

    @Override
    public void warning(String message) {
      System.out.println("[WARNING] " + message);
    }

    @Override
    public void severe(String message) {
      System.out.println("[SEVERE] " + message);
    }

    @Override
    public void severe(String message, Throwable cause) {
      this.severe(message);
      cause.printStackTrace();
    }

    @Override
    public void info(String message) {
      System.out.println("[INFORMATION] " + message);
    }

    @Override
    public void fatal(String message) {
      System.out.println("[---------------[!!!FATAL!!!]---------------]");
      System.out.println(message);
      System.out.println("[---------------[!!!FATAL!!!]---------------]");
    }

    @Override
    public void fatal(String message, Throwable cause) {
      this.fatal(message);
      cause.printStackTrace();
    }
  }

  public static interface LogHelper {
    public void debug(String var1);

    public void info(String var1);

    public void warning(String var1);

    public void severe(String var1);

    public void severe(String var1, Throwable var2);

    public void fatal(String var1);

    public void fatal(String var1, Throwable var2);
  }

  public class SafeCommonSuperClassWriter extends ClassWriter {
    private final MetaReader metaReader;

    public SafeCommonSuperClassWriter(MetaReader metaReader, int flags) {
      super(flags);
      this.metaReader = metaReader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
      int i;
      ArrayList<String> superClasses1 = this.metaReader.getSuperClasses(type1);
      ArrayList<String> superClasses2 = this.metaReader.getSuperClasses(type2);
      int size = Math.min(superClasses1.size(), superClasses2.size());
      for (i = 0; i < size && superClasses1.get(i).equals(superClasses2.get(i)); ++i) {}
      if (i == 0) {
        return "java/lang/Object";
      }
      return superClasses1.get(i - 1);
    }
  }
}
