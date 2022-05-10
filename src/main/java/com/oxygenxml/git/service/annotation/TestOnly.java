package com.oxygenxml.git.service.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an element as used only for tests.
 * <p>
 * Denotes that a local variable, parameter, field, method return value is expected
 * to be used for tests.
 *
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ FIELD, METHOD})
public @interface TestOnly {
  // marker annotation with no members
}