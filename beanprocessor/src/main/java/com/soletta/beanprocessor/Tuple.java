package com.soletta.beanprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value=ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface Tuple {
  Class<?> [] value();
  String tupleTypeName() default "";
}
