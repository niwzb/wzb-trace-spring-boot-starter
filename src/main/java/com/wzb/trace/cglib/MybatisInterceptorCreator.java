package com.wzb.trace.cglib;

import com.wzb.trace.aspect.ClassAspect;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;

@Slf4j
public class MybatisInterceptorCreator {

    public static Class<?> loadMybatisInterceptor() {

        ClassPool pool = new ClassPool(true);
        pool.appendClassPath(new LoaderClassPath(MybatisInterceptorCreator.class.getClassLoader()));
        CtClass ctClass = pool.makeClass("com.wzb.trace.cglib.WzbMybatisTraceInterceptor");
        try {
            CtClass interceptor = pool.get("org.apache.ibatis.plugin.Interceptor");
            CtClass classAspect = pool.get(ClassAspect.class.getName());

            ctClass.addInterface(interceptor);
            ctClass.addInterface(classAspect);

            ctClass.addConstructor(CtNewConstructor.defaultConstructor(ctClass));
            ctClass.addMethod(CtMethod.make("public com.wzb.trace.report.WzbTraceStorage getWzbTraceStorage() {" +
                    "        return (com.wzb.trace.report.WzbTraceStorage) com.wzb.trace.utils.SpringContextUtil.getBean(\"wzbTraceStorage\");" +
                    "    }", ctClass));
            ctClass.addMethod(CtMethod.make("public java.lang.Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws java.lang.Throwable {" +
                    "        int status = 200;" +
                    "        java.sql.Connection connection = (java.sql.Connection) invocation.getArgs()[0];" +
                    "        java.lang.String project = connection.getMetaData().getDatabaseProductName();" +
                    "        long start = 0;" +
                    "        if (com.wzb.trace.utils.StringUtil.isBlank(com.wzb.trace.network.TraceTools.get(project))) {" +
                    "            com.wzb.trace.network.TraceTools.put(project, com.wzb.trace.network.TraceTools.get(com.wzb.trace.network.TraceTools.X_TRACE_ID));" +
                    "            start = java.lang.System.currentTimeMillis();" +
                    "        }" +
                    "        try {" +
                    "            return invocation.proceed();" +
                    "        } catch (java.lang.Throwable throwable) {" +
                    "            status = com.wzb.trace.network.TraceTools.X_RESPONSE_STATUS_500;" +
                    "            throw throwable;" +
                    "        } finally {" +
                    "            if (start > 0) {" +
                    "                this.report(com.wzb.trace.report.WzbTrace.builder()" +
                    "                        .clientIP(com.wzb.trace.network.TraceTools.getLocalIp())" +
                    "                        .project(project)" +
                    "                        .duration(java.lang.System.currentTimeMillis() - start)" +
                    "                        .requestSize(0L)" +
                    "                        .responseSize(0L)" +
                    "                        .traceId(com.wzb.trace.network.TraceTools.get(com.wzb.trace.network.TraceTools.X_TRACE_ID) + \"|\" + com.wzb.trace.network.TraceTools.getTraceId())" +
                    "                        .path(invocation.getMethod().getName())" +
                    "                        .status(status)" +
                    "                        .timestamp(com.wzb.trace.utils.DateUtil.dateFormat(new java.util.Date(), com.wzb.trace.utils.DateUtil.UTC_DATE_FORMAT, com.wzb.trace.utils.DateUtil.TIMEZONE_UTC))" +
                    "                        .build());" +
                    "            }" +
                    "        }" +
                    "    }", ctClass));
            ctClass.addMethod(CtMethod.make("public java.lang.Object plugin(java.lang.Object target) {" +
                    "        return (target instanceof org.apache.ibatis.executor.statement.StatementHandler) ? org.apache.ibatis.plugin.Plugin.wrap(target, this) : target;" +
                    "    }", ctClass));

            ClassFile classFile = ctClass.getClassFile();
            ConstPool constPool = classFile.getConstPool();
            AnnotationsAttribute annotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);

            Annotation signatureAnnotation = new Annotation("org.apache.ibatis.plugin.Signature", constPool);
            signatureAnnotation.addMemberValue("type", new ClassMemberValue("org.apache.ibatis.executor.statement.StatementHandler", constPool));
            signatureAnnotation.addMemberValue("method", new StringMemberValue("prepare", constPool));
            MemberValue[] parameters = new MemberValue[]{
                    new ClassMemberValue(Connection.class.getCanonicalName(), constPool),
                    new ClassMemberValue(Integer.class.getCanonicalName(), constPool)
            };
            ArrayMemberValue parameter = new ArrayMemberValue(constPool);
            parameter.setValue(parameters);
            signatureAnnotation.addMemberValue("args", parameter);

            Annotation interceptsAnnotation = new Annotation("org.apache.ibatis.plugin.Intercepts", constPool);

            ArrayMemberValue interceptsValue = new ArrayMemberValue(constPool);
            interceptsValue.setValue(new MemberValue[]{
                    new AnnotationMemberValue(signatureAnnotation, constPool)
            });
            interceptsAnnotation.addMemberValue("value", interceptsValue);

            annotationsAttribute.addAnnotation(interceptsAnnotation);

            classFile.addAttribute(annotationsAttribute);

            return ctClass.toClass(MybatisInterceptorCreator.class.getClassLoader(), MybatisInterceptorCreator.class.getProtectionDomain());
        } catch (CannotCompileException | NotFoundException e) {
            log.warn("loadMybatisInterceptor", e);
        }
        return null;
    }

}
