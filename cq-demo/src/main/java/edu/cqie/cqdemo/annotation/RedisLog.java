package edu.cqie.cqdemo.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLog {
    
    /**
     * 日志类型
     */
    String type() default "INFO";
    
    /**
     * 所属模块
     */
    String module() default "DEFAULT";
    
    /**
     * 日志描述
     */
    String description() default "";
}
