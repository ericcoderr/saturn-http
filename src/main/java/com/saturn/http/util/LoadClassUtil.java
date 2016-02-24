/** ******ReloadClass.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import com.saturn.util.CharsetUtil;
import com.saturn.util.StringTools;
import com.saturn.util.log.Log;

/**
 * @describe: <pre>
 * 把指定包路径下的类初始化后放到map里
 * 如果类加了scope的注解，表示此类每次都要初始化，反之，只初始化一次
 * 
 * 原作者 http://www.oschina.net/code/snippet_129830_8767
 * </pre>
 * @date :2013年8月7日 下午5:26:32
 * @author : ericcoderr@gmail.com
 */
public class LoadClassUtil {

    private static final Logger log = Log.getLogger();

    private static final Charset CHARSET = CharsetUtil.UTF_8;

    public static Set<Class<?>> loadPkgClass(String pkgName) throws Exception {
        return loadPkgClass(pkgName, CHARSET.displayName());
    }

    public static Set<Class<?>> loadPkgClass(String pkgName, String charset) throws Exception {
        if (StringTools.isEmpty(pkgName)) {
            return null;
        }
        Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换,把 . 替换成 /
        Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(pkgName.replace(".", "/"));
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                String filePath = URLDecoder.decode(url.getFile(), charset);
                findAndAddClassesInPackageByFile(pkgName, filePath, recursive, classes);
            } else if ("jar".equals(protocol)) {
                JarFile jar;
                try {
                    // 获取jar
                    jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    // 从此jar包 得到一个枚举类
                    Enumeration<JarEntry> entries = jar.entries();

                    // 同样的进行循环迭代
                    while (entries.hasMoreElements()) {
                        // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文件
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        // 如果是以/开头的
                        if (name.charAt(0) == '/') {
                            // 获取后面的字符串
                            name = name.substring(1);
                        }
                        String jarPkgName = "";
                        // 如果前半部分和定义的包名相同
                        if (name.startsWith(pkgName)) {
                            int idx = name.lastIndexOf('/');
                            // 如果以"/"结尾 是一个包
                            if (idx != -1) {
                                // 获取包名 把"/"替换成"."
                                jarPkgName = name.substring(0, idx).replace('/', '.');
                            }
                            // 如果可以迭代下去 并且是一个包
                            if ((idx != -1) || recursive) {
                                // 如果是一个.class文件 而且不是目录
                                if (name.endsWith(".class") && !entry.isDirectory()) {
                                    // 去掉后面的".class" 获取真正的类名
                                    String className = name.substring(jarPkgName.length() + 1, name.length() - 6);
                                    try {
                                        // 添加到classes
                                        classes.add(Thread.currentThread().getContextClassLoader().loadClass(pkgName + '.' + className));
                                    } catch (ClassNotFoundException e) {
                                        log.error("Class :{} load failed.", pkgName + '.' + className, e);
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    log.error("Scan :{} jar failed.", pkgName, e);
                }
            }
        }
        return classes;
    }

    /**
     * 以文件的形式来获取包下的所有Class
     * 
     * @param packageName
     * @param packagePath
     * @param recursive
     * @param classes
     */
    private static void findAndAddClassesInPackageByFile(String packageName, String packagePath, final boolean recursive, Set<Class<?>> classes) {
        // 获取此包的目录 建立一个File
        File dir = new File(packagePath);
        // 如果不存在或者 也不是目录就直接返回
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("用户定义包名 " + packageName + " 下没有任何文件");
            return;
        }
        // 如果存在 就获取包下的所有文件 包括目录
        File[] dirfiles = dir.listFiles(new FileFilter() {

            // 自定义过滤规则 如果可以循环(包含子目录) 或是以.class结尾的文件(编译好的java类文件)
            public boolean accept(File file) {
                return (recursive && file.isDirectory()) || (file.getName().endsWith(".class"));
            }
        });
        // 循环所有文件
        for (File file : dirfiles) {
            // 如果是目录 则继续扫描
            if (file.isDirectory()) {
                findAndAddClassesInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, classes);
            } else {
                // 如果是java类文件 去掉后面的.class 只留下类名
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    // 添加到集合中去
                    classes.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + '.' + className));
                } catch (ClassNotFoundException e) {
                    log.error("Class :{} load failed.", packageName + '.' + className, e);
                }
            }
        }
    }

    public static Set<Object> newInstance(String pkgName) throws Exception {
        Set<Class<?>> set = LoadClassUtil.loadPkgClass(pkgName);
        Set<Object> instanceSet = new LinkedHashSet<Object>();
        for (Class<?> clazz : set) {
            if (clazz.isAnnotation()) {
                continue;
            }
            instanceSet.add(clazz.newInstance());
        }
        return instanceSet;
    }

    public static Set<Object> newInstance(String pkgName, String charset) throws Exception {
        Set<Class<?>> set = LoadClassUtil.loadPkgClass(pkgName, charset);
        Set<Object> instanceSet = new LinkedHashSet<Object>();
        for (Class<?> clazz : set) {
            instanceSet.add(clazz.newInstance());
        }
        return instanceSet;
    }
}
