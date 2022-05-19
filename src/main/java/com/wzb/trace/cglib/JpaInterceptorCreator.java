package com.wzb.trace.cglib;

import com.wzb.trace.aspect.ClassAspect;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JpaInterceptorCreator {

    public static Class<?> loadJpaInterceptor() {

        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(JpaInterceptorCreator.class.getClassLoader()));
        try {
            CtClass interceptor = pool.get("org.hibernate.resource.jdbc.spi.StatementInspector");
            CtClass ctClass = pool.makeClass("com.wzb.trace.cglib.WzbJpaTraceInterceptor");
            ctClass.addInterface(pool.get(ClassAspect.class.getName()));
            ctClass.addInterface(interceptor);

            ctClass.addConstructor(CtNewConstructor.defaultConstructor(ctClass));
            ctClass.addMethod(CtMethod.make("public com.wzb.trace.report.WzbTraceStorage getWzbTraceStorage() {" +
                    "        return (com.wzb.trace.report.WzbTraceStorage) com.wzb.trace.utils.SpringContextUtil.getBean(\"wzbTraceStorage\");" +
                    "    }", ctClass));
            ctClass.addMethod(CtMethod.make("public java.lang.String inspect(java.lang.String sql) {" +
                    "        if (com.wzb.trace.utils.StringUtil.isBlank(com.wzb.trace.network.TraceTools.get(com.wzb.trace.network.TraceTools.DB_FLAG))) {" +
                    "            com.wzb.trace.network.TraceTools.put(com.wzb.trace.network.TraceTools.DB_FLAG, com.wzb.trace.network.TraceTools.get(com.wzb.trace.network.TraceTools.X_TRACE_ID));" +
                    "            this.report(com.wzb.trace.report.WzbTrace.builder()" +
                    "                    .clientIP(com.wzb.trace.network.TraceTools.getLocalIp())" +
                    "                    .project(com.wzb.trace.network.TraceTools.DB_FLAG)" +
                    "                    .duration(5L)" +
                    "                    .requestSize(0L)" +
                    "                    .responseSize(0L)" +
                    "                    .traceId(com.wzb.trace.network.TraceTools.get(com.wzb.trace.network.TraceTools.X_TRACE_ID) + \"|\" + com.wzb.trace.network.TraceTools.getTraceId())" +
                    "                    .path(sql.split(\" \")[0])" +
                    "                    .status(200)" +
                    "                    .timestamp(com.wzb.trace.utils.DateUtil.dateFormat(new java.util.Date(), com.wzb.trace.utils.DateUtil.UTC_DATE_FORMAT, com.wzb.trace.utils.DateUtil.TIMEZONE_UTC))" +
                    "                    .build());" +
                    "        }" +
                    "        return sql;" +
                    "    }", ctClass));


            return ctClass.toClass(MybatisInterceptorCreator.class.getClassLoader(), MybatisInterceptorCreator.class.getProtectionDomain());
        } catch (CannotCompileException | NotFoundException e) {
            log.warn("loadJpaInterceptor", e);
        }

        return null;
    }
}
