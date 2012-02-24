package com.soletta.beanprocessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Indicates that tuple implementations should be generated into the
 * package.
 * 
 * @author rjudson
 *
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface Tuples {
  int value();
}

