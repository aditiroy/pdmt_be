package com.lowes.permits.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogSensitive {

	Type type() default Type.CUSTOM;

	int ignoreStart() default 0;

	int ignoreEnd() default 0;

	enum Type {
		PHONENUMBER,
		EMAIL,
		CREDITCARD,
		CUSTOM
	}
}
