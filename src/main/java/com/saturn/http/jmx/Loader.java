/** ******Loader.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.jmx;

/**
 * @describe: <pre>
 * </pre>
 * @date :2014年2月24日 上午10:54:38
 * @author : ericcoderr@gmail.com
 */
public class Loader {

    /**
     * Load a class.
     * 
     * @param loadClass
     * @param name
     * @return Class
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("rawtypes")
    public static Class loadClass(Class loadClass, String name) throws ClassNotFoundException {
        ClassNotFoundException ex = null;
        Class<?> c = null;
        ClassLoader context_loader = Thread.currentThread().getContextClassLoader();
        if (context_loader != null) {
            try {
                c = context_loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                ex = e;
            }
        }

        if (c == null && loadClass != null) {
            ClassLoader load_loader = loadClass.getClassLoader();
            if (load_loader != null && load_loader != context_loader) {
                try {
                    c = load_loader.loadClass(name);
                } catch (ClassNotFoundException e) {
                    if (ex == null)
                        ex = e;
                }
            }
        }

        if (c == null) {
            try {
                c = Class.forName(name);
            } catch (ClassNotFoundException e) {
                if (ex != null)
                    throw ex;
                throw e;
            }
        }

        return c;
    }
}
