/** ******RtnError.java*****/
/**
 *Copyright
 *
**/
package com.saturn.http.exception;

import com.saturn.util.http.JsonObjectUtil;

/**
 * @describe:
 * 
 *            <pre>
 *            返回错误抽象类
 *            </pre>
 * 
 * @date :2015年10月16日 下午2:48:59
 * @author : eric
 */
public abstract class RtnError extends Error {

    private static final long serialVersionUID = 1L;
    /**
     * 返回数据的ID
     */
    protected int rtn;
    /**
     * 数据的描述
     */
    protected String msg;
    /**
     * 数据的json表示
     */
    protected String _json;

    /**
     * 默认构造方法
     */
    public RtnError() {
        this.msg = this.getClass().getSimpleName();
    }

    /**
     * 获得返回数据的JSON表示
     * 
     * @return
     */
    public String getJson() {
        if (_json == null) {
            _json = JsonObjectUtil.getRtnAndDataJsonObject(getRtn(), JsonObjectUtil.buildMap("msg", getMsg()));
        }
        return _json;
    }

    public abstract int getRtn();

    /**
     * 返回的数据描述
     * 
     * @return
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 为了能在异常邮件中获取到异常的具体信息，覆盖了此方法
     */
    @Override
    public String getMessage() {
        return msg;
    }

    @Override
    public String toString() {
        return "RtnError [rtn=" + rtn + ", msg=" + msg + "]";
    }
}
