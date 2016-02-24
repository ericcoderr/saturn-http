/** ******IpAuth.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @describe: <pre>
 * ip验证注解，如果方法包含这个注解，表示需要ip验证
 * </pre>
 * @date :2014年12月30日 下午2:27:42
 * @author : ericcoderr@gmail.com
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IpAuth {

    String[] value() default {};
}
