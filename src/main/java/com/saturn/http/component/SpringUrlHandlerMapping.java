/** ******SpringUrlHandlerMapping.java*****/
/**
 *Copyright
 *
 **/
package com.saturn.http.component;

import java.util.Map;
import org.springframework.stereotype.Controller;
import com.saturn.http.SaturnServerBootstrap;

/**
 * @describe: <pre>
 * spring  url mapping,，通过spring 管理类
 * </pre>
 * @date :2014年7月14日 上午10:18:04
 * @author : ericcoderr@gmail.com
 */
public class SpringUrlHandlerMapping extends UrlHandlerMapping {

    @Override
    public void registerUrlMap() throws Exception {
        Map<String, Object> map = SaturnServerBootstrap.context.getBeansWithAnnotation(Controller.class);
        LOG.debug("spring scane class map:{}", map);
        if (map.isEmpty()) {
            LOG.warn("Neither 'urlMap' nor 'mappings' set on UrlHandlerMapping");
        } else {
            for (String key : map.keySet()) {
                Object obj = map.get(key);
                if (obj != null && obj.getClass().getAnnotation(Deprecated.class) == null) {
                    putUrlMap(obj);
                }
            }
        }
    }
}
