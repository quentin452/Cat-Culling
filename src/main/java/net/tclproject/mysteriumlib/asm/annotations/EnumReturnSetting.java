package net.tclproject.mysteriumlib.asm.annotations;

public enum EnumReturnSetting {
  NEVER(false),
  ALWAYS(false),
  ON_TRUE(true),
  ON_NULL(true),
  ON_NOT_NULL(true);

  public final boolean conditionRequiredToReturn;

  private EnumReturnSetting(boolean conditionRequiredToReturn) {
    this.conditionRequiredToReturn = conditionRequiredToReturn;
  }
}
