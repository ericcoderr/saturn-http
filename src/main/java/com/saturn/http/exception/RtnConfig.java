/** ******RtnConfig.java*****/
/**
 *Copyright
 *
**/
package com.saturn.http.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @describe:
 * 
 *            <pre>
 *            </pre>
 * 
 * @date :2015年10月16日 下午3:02:19
 * @author : eric
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RtnConfig {

    public int value();
}
