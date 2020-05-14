package com.atguigu.gmall.common.cache;

import java.lang.annotation.*;

/**
 * 仿事务注解  缓存注解
 *
 * @Target({ElementType.TYPE, ElementType.METHOD})  :
 *      ElementType.TYPE   此注解打在类头上
 *      ElementType.METHOD ： 此注解打在方法上
 *@Documented ：此注解是否出现在Api文档
 *
 *  * CLASS  RUNTIME   SOURCE
 *  * 按生命周期来划分可分为3类：
 *  * 1、RetentionPolicy.SOURCE：注解只保留在源文件，当Java文件编译成class文件的时候，注解被遗弃；
 *  * 2、RetentionPolicy.CLASS：注解被保留到class文件，但jvm加载class文件时候被遗弃，这是默认的生命周期；
 *  * 3、RetentionPolicy.RUNTIME：注解不仅被保存到class文件中，jvm加载class文件之后，仍然存在；
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    //缓存的前缀   String key = 前缀 + 变量skuId
    String prefix() default "cache";

}
