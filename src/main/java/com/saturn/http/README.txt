version: 0.1
说明: ares,希腊战神


监控访问地址
1.tps,接口访问次数，ip过滤 ，未知接口访问统计


2.RequestMapping 重命名，不要跟Spring类重名






#============================================================================
注意事项
1.如果ares-http.jar打包的目录为com.ares.http,spring 扫描路经也要为
	<context:component-scan base-package="com.ares.http" />

2.AresHttpRequest.ByteBuf.content() 如果是非堆内存，如果加了aggerator要在decoder里释放，如果没加，可以在writeResponse()
方法里释放，但是有问题
3.测试时发现会丢请求








#=============================================================================
待完成功能
1.AresHttpRequest.decodePost() 有bug，Post decode hasNext会抛异常
2.request.getContent,如果之前调用过request.getParameterMap(),此时如果直接request.getContent会出现读不到字节的情况
3.jmx监控
4.gradle依赖管理