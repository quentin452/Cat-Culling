package net.tclproject.mysteriumlib.asm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = {ElementType.METHOD})
public @interface Fix {
  public EnumReturnSetting returnSetting() default EnumReturnSetting.NEVER;

  public FixOrder order() default FixOrder.USUAL;

  public String targetMethod() default "";

  public String returnedType() default "";

  public boolean createNewMethod() default false;

  public boolean isFatal() default false;

  public boolean insertOnExit() default false;

  @Deprecated
  public int insertOnLine() default -1;

  public String anotherMethodReturned() default "";

  public boolean nullReturned() default false;

  public boolean booleanAlwaysReturned() default false;

  public byte byteAlwaysReturned() default 0;

  public short shortAlwaysReturned() default 0;

  public int intAlwaysReturned() default 0;

  public long longAlwaysReturned() default 0L;

  public float floatAlwaysReturned() default 0.0f;

  public double doubleAlwaysReturned() default 0.0;

  public char charAlwaysReturned() default 0;

  public String stringAlwaysReturned() default "";
}
