package com.milktea.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceOwnerCheck {
    ResourceType resourceType();
    String idParam() default "id";
    String notFoundMessage() default "Resource not found";
    String notAuthorizedMessage() default "Not authorized";
    boolean allowAdmin() default true;
}
