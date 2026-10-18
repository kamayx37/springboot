package com.example.product.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在 Controller 的写操作方法上，由 {@link OperationLogAspect} 记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 业务模块，如「商品管理」 */
    String module();

    /** 操作描述，如「新增商品」 */
    String operation();

    /** 是否记录请求参数 */
    boolean saveParams() default true;
}
