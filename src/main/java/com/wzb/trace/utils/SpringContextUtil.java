package com.wzb.trace.utils;

import com.wzb.trace.network.TraceTools;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SpringContextUtil {

    private static volatile ApplicationContext context;

    private static final Map<DataSource, String> dbTypeMap = new HashMap<>();

    public static <T> T getBean(Class<? extends T> clazz) {
        return null == context ? null : context.getBean(clazz);
    }

    public static Object getBean(String clazz) {
        return null == context ? null : context.getBean(clazz);
    }

    public static void addDbType(DataSource datasource) throws SQLException {
        if (!dbTypeMap.containsKey(datasource)) {
            try {
                dbTypeMap.put(datasource, datasource.getConnection().getMetaData().getDatabaseProductName());
            } catch (SQLException ignored) {}
        }
    }

    public static String getDbType(DataSource datasource) {
        return Optional.ofNullable(dbTypeMap.get(datasource)).orElse(TraceTools.DB_FLAG);
    }

    public static class SpringContextUtilAware implements ApplicationContextAware {
        @Override
        public void setApplicationContext(ApplicationContext context) throws BeansException {
            if (null == SpringContextUtil.context) {
                SpringContextUtil.context = context;
            }
        }
    }
}
