package net.tclproject.mysteriumlib.asm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(value = {ElementType.PARAMETER})
public @interface ReturnedValue {}
