package com.saturn.http.component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import com.saturn.http.controller.annotation.ReqMapping;
import com.saturn.http.util.HttpServerConfig;
import com.saturn.http.util.LoadClassUtil;
import com.saturn.http.util.jetty.PathMap;
import com.saturn.util.ResourceBundleUtil;
import com.saturn.util.log.Log;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * <pre>
 * url 映射 到 bean
 * 映射默认规则为/类名/方法名 ,类名和方法名都是小写
 * 如果方法上加了@RequestMapping ,表示这个URL被重写了，以@RequestMapping为准
 * 
 * <pre>
 */
public class UrlHandlerMapping {

    protected static final Logger LOG = Log.getLogger();

    private Map<String, UrlMapping> URLMAP = new HashMap<String, UrlMapping>();

    // 处理模糊Url ,Copy to jetty9.0
    private PathMap<UrlMapping> pathMap = new PathMap<UrlMapping>();

    // 用来存储初始化的Controler跟类名的映射关系,类名第一个字母小写
    private Map<String, Object> classMap = new LinkedHashMap<String, Object>();

    public Map<String, UrlMapping> getUrlMap() {
        return URLMAP;
    }

    private String[] pkgNames;

    public void setPkgNames(String... pkgNames) {
        this.pkgNames = pkgNames;
    }

    /**
     * <pre>
     * Register all handlers specified in the URL map for the corresponding paths.
     * 必须是加了ReqMapping annotation才表示是一个处理Request请求的类
     * </pre>
     */
    public void registerUrlMap() throws Exception {
        if (pkgNames != null) {
            for (String pkgName : pkgNames) {
                Set<Object> classes = LoadClassUtil.newInstance(pkgName);
                if (classes.isEmpty()) {
                    LOG.warn("Neither 'urlMap' nor 'mappings' set on UrlHandlerMapping");
                } else {
                    for (Object obj : classes) {

                        // TODO 跟spring 耦合在一块了
                        if (obj.getClass().getAnnotation(Deprecated.class) == null && obj.getClass().isAnnotationPresent(Controller.class)) {
                            putUrlMap(obj);
                        }
                    }
                }
            }
        }
    }

    // 如果要自己定义包名初始化，必须要先要设置setPkgNames(String ... pkgNames)
    public void initUrlMap() throws Exception {
        registerUrlMap();
        urlMapping4Properties();
        LOG.info("AUTO_MAP:\t\t{}", URLMAP);
    }

    /**
     * 只把第一个字母小写
     */
    private String _lower(String className) {
        return className.toUpperCase().charAt(0) + className.substring(1);
    }

    /**
     * 只把类和方法映射成标准的url
     * 
     * @param baseControler
     */
    protected void putUrlMap(Object controler) {
        Map<String, UrlMapping> tmp_auto = new LinkedHashMap<String, UrlMapping>();
        Method[] mehods = controler.getClass().getDeclaredMethods();
        for (Method method : mehods) {
            if (!isControlMethod(method)) {
                continue;
            }

            // ReqMapping 的value是不是 空字符串，如果不是，url使用当前值，否则，按类名+方法名拼接url
            UrlMapping urlMapping = new UrlMapping(controler, method);
            ReqMapping reqAnnotation = method.getAnnotation(ReqMapping.class);
            if (reqAnnotation != null && reqAnnotation.value().length > 0) {
                tmp_auto.put(reqAnnotation.value()[0], urlMapping);
            } else {
                classMap.put(_lower(controler.getClass().getSimpleName()), controler);
                tmp_auto.put(MessageFormat.format("/{0}/{1}", urlMapping.getControler().getClass().getSimpleName().toLowerCase(), method.getName().toLowerCase()), urlMapping);
            }
        }
        URLMAP.putAll(tmp_auto);
    }

    /**
     * 处理特殊url映射,urlMapping.properties(特殊url配置文件)
     */
    protected void urlMapping4Properties() {
        ResourceBundle rb = ResourceBundleUtil.reload("urlMapping");

        if (rb != null) {
            for (String url : rb.keySet()) {
                if (!isConfigUrl(url)) {
                    continue;
                }
                String value = rb.getString(url).trim();
                Set<UrlMapping> set = createUrlMapping(value);
                if (set != null && set.size() > 0) {
                    for (UrlMapping mapping : set) {
                        pathMap.put(url, mapping);
                    }
                }
            }
        }
        LOG.info("CONFIG_MAP:\t\t{}", pathMap);
    }

    private boolean isConfigUrl(String configUrlKey) {
        return configUrlKey.startsWith("*") || configUrlKey.startsWith("/");
    }

    private Set<UrlMapping> createUrlMapping(String config) {
        String[] configs = config.split("/");
        // 类似foo/bar
        if (configs.length == 2) {
            return _createUrlMapping(configs[0], true);
        } else {// 类似 foo,说明该controler只有一个默认方法
            return _createUrlMapping(config, false);
        }
    }

    public Set<UrlMapping> _createUrlMapping(String config, boolean multi) {
        Object controler = classMap.get(config);
        if (controler != null) {
            Set<UrlMapping> set = new HashSet<UrlMapping>();
            if (multi) {
                Method[] methods = controler.getClass().getMethods();
                for (Method m : methods) {
                    if (isControlMethod(m)) {
                        set.add(new UrlMapping(controler, m, true));
                    }
                }
            } else {
                try {
                    Method method = controler.getClass().getMethod(HttpServerConfig.DEFAULT_METHOD);
                    if (isControlMethod(method)) {
                        set.add(new UrlMapping(controler, method, true));
                    }
                } catch (Exception e) {
                    LOG.error("urlMapping4Properties error,", e);
                }
            }
            return set;
        }
        return null;
    }

    public UrlMapping getUrlMapping(String path) throws Exception {
        path = sanitizePath(path);

        // 默认进入跟目录请求
        if ("/".equals(path)) {
            path = "/welcome";
        }
        return getUrlMap().get(path);
    }

    /**
     * 处理URL,去掉最后的 / ,替换Url多余的斜线
     */
    private String sanitizePath(String path) {
        int len = path.length();
        if (len > 1 && path.lastIndexOf('/') == len - 1) {
            path = path.substring(0, len - 1);
        }

        // 替换url 中双 // 或//...,目前最大只能替换10个连续斜线
        return path.replaceAll("(?<!:)(/){2,}", "/");
    }

    /**
     * 检验此方法是不是处理HTTP请求的方法
     */
    private boolean isControlMethod(Method method) {
        if (method.getAnnotation(Deprecated.class) != null)
            return false;
        if (!Modifier.isPublic(method.getModifiers()))
            return false;
        if (!method.isAnnotationPresent(ReqMapping.class)) {
            return false;
        }
        return true;
    }

    public static class UrlMapping {

        private Object controler;
        private Method method;
        private String path;
        private boolean isConfig;
        private Map<String, Class<?>> methodParamNames;

        public UrlMapping(Object controler, Method method) {
            this.controler = controler;
            this.method = method;
            this.path = controler.getClass().getSimpleName().toLowerCase() + "." + method.getName().toLowerCase();
        }

        public UrlMapping(Object controler, Method method, boolean isConfig) {
            this.controler = controler;
            this.method = method;
            this.path = controler.getClass().getSimpleName().toLowerCase() + "." + method.getName().toLowerCase();
            this.isConfig = isConfig;
        }

        public Object getControler() {
            return controler;
        }

        public Method getMethod() {
            return method;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((controler == null) ? 0 : controler.hashCode());
            result = prime * result + ((method == null) ? 0 : method.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            UrlMapping other = (UrlMapping) obj;
            if (controler == null) {
                if (other.controler != null)
                    return false;
            } else if (!controler.equals(other.controler))
                return false;
            if (method == null) {
                if (other.method != null)
                    return false;
            } else if (!method.equals(other.method))
                return false;
            return true;
        }

        public boolean isConfig() {
            return isConfig;
        }

        /**
         * <pre>
         * 获取方法里的参数名,借助于javassist实现，因为要解析字节码文件里的
         * LocalVariableTable, 所以编译文件必须在debug模式下编译
         * 要保证参数顺序，此处使用LinkedHashMap
         * </pre>
         */
        public Map<String, Class<?>> _setMethodParamNames() throws Exception {

            // TODO 一个Controler里可能会有多个方法，应该缓存起来下面的操作
            Class<?> clazz = controler.getClass();
            ClassPool pool = new ClassPool();
            pool.appendSystemPath();
            pool.appendClassPath(new ClassClassPath(clazz));
            CtClass cc = pool.get(clazz.getName());
            CtMethod method = cc.getDeclaredMethod(getMethod().getName());
            MethodInfo methodInfo = method.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (attr == null) {
                throw new IllegalAccessException("Java class compile not arg debug,LocalVariableAttribute is null.");
            }
            Class<?>[] parameterTypes = getMethod().getParameterTypes();
            Map<String, Class<?>> paramNames = new LinkedHashMap<String, Class<?>>(method.getParameterTypes().length);
            int pos = Modifier.isStatic(method.getModifiers()) ? 0 : 1;

            // TODO 如果类里面有跟请求同名的方法，此处会报错，应该是必须判断参数和返回值？还有方法访问权限
            for (int i = 0; i < method.getParameterTypes().length; i++) {
                paramNames.put(attr.variableName(i + pos), parameterTypes[i]);
            }
            return paramNames;
        }

        /**
         * 基于jdk8 实现,编译时必须加参数–parameter,否则获取的参数名字为arg0 要保证参数顺序，此处使用LinkedHashMap
         * 
         * @return
         * @throws Exception
         */
        public Map<String, Class<?>> _setMethodParamNames(boolean isJdK8) throws Exception {
            Parameter[] params = getMethod().getParameters();
            Map<String, Class<?>> paramNames = new LinkedHashMap<String, Class<?>>(params.length);
            for (Parameter p : params) {
                if (p.isImplicit()) {
                    throw new IllegalAccessException("Java class compile not arg –parameter.");
                }
                paramNames.put(p.getName(), p.getType());
            }
            return paramNames;
        }

        public Map<String, Class<?>> getMethodParamNames(boolean isJdK8) throws Exception {
            if (isJdK8) {
                this.methodParamNames = _setMethodParamNames(isJdK8);
            } else {
                this.methodParamNames = _setMethodParamNames();
            }
            return methodParamNames;
        }

        @Override
        public String toString() {
            return path;
        }
    }
}
