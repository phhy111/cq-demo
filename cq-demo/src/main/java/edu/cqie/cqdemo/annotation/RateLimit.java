package edu.cqie.cqdemo.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    int limit() default 60;
    
    int timeoutSeconds() default 60;
    
    String key() default "";
    
    String message() default "请求过于频繁，请稍后再试";
}
