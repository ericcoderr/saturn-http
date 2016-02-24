/** ******SimpleRtnError.java*****/
/**
 *Copyright
 *
**/
package com.saturn.http.exception;

import com.saturn.util.StringTools;
import com.saturn.util.http.JsonObjectUtil;

/**
 * @describe:
 * 
 *            <pre>
 *            样本返回错误类
 *            </pre>
 * 
 * @date :2015年10月16日 下午2:51:37
 * @author : eric
 */
public class SampleRtnError extends RtnError {

    private static final long serialVersionUID = 1L;

    public SampleRtnError() {
    }

    /**
     * 默认的构造方法
     * 
     * @param rtn
     */
    public SampleRtnError(int rtn) {
        this(rtn, null);
    }

    /**
     * 构造方法
     * 
     * @param rtn
     * @param msg
     */
    public SampleRtnError(int rtn, String msg) {
        this.rtn = rtn;
        this.msg = msg;
        if (StringTools.isEmpty(msg)) {
            _json = JsonObjectUtil.getOnlyRtnJson(rtn);
        } else {
            _json = JsonObjectUtil.getRtnAndDataJsonObject(getRtn(), JsonObjectUtil.buildMap("msg", getMsg()));
        }
    }

    /**
     * 获得返回码
     */
    @Override
    public int getRtn() {
        return rtn;
    }
}
