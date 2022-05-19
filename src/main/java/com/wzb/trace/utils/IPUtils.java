package com.wzb.trace.utils;


import com.wzb.trace.network.TraceTools;
import com.wzb.trace.wrapper.AbsentLinkedHashSet;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
public final class IPUtils {

    /**
     * 获取用户真实IP地址，不使用request.getRemoteAddr()的原因是有可能用户使用了代理软件方式避免真实IP地址,
     * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值
     *
     * @return ip
     */
    public static String getClientIP(HttpServletRequest request) {
        String ip = firstNotBlank(request, header -> StringUtil.isNotBlank(header) && !"unknown".equalsIgnoreCase(header),
                TraceTools.X_FORWARDED_FOR, TraceTools.PROXY_CLIENT_IP, TraceTools.WL_PROXY_CLIENT_IP, TraceTools.HTTP_CLIENT_IP, TraceTools.HTTP_X_FORWARDED_FOR, TraceTools.X_REAL_IP);
        if (StringUtil.isNotBlank(ip) && ip.contains(",")) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            ip = ip.split(",")[0];
        } else if (StringUtil.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public static String firstNotBlank(HttpServletRequest request, Function<String, Boolean> condition, String... headers) {
        return Stream.of(headers).map(request::getHeader).filter(condition::apply).findFirst().orElse(null);
    }

    public static Set<ProxyClient> getClientTrace(HttpServletRequest request) {
        Set<ProxyClient> clientTraceIpSet = new AbsentLinkedHashSet<>();
        String ip = request.getHeader(TraceTools.X_FORWARDED_FOR);
        String agent = request.getHeader(TraceTools.USER_AGENT);
        agent = StringUtil.isNotBlank(agent) ? StringUtil.firstSubString(agent, " ") : agent;
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            ProxyClient.generateProxyClient(ip, Agent.X_FORWARDED_FOR, agent, clientTraceIpSet);
        }
        ip = request.getHeader(TraceTools.PROXY_CLIENT_IP);
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            ProxyClient.generateProxyClient(ip, Agent.PROXY_CLIENT_IP, agent, clientTraceIpSet);
        }
        ip = request.getHeader(TraceTools.WL_PROXY_CLIENT_IP);
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            ProxyClient.generateProxyClient(ip, Agent.WL_PROXY_CLIENT_IP, agent, clientTraceIpSet);
        }
        ip = request.getHeader(TraceTools.HTTP_X_FORWARDED_FOR);
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            ProxyClient.generateProxyClient(ip, Agent.X_FORWARDED_FOR, agent, clientTraceIpSet);
        }
        ip = request.getHeader(TraceTools.HTTP_CLIENT_IP);
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            clientTraceIpSet.add(new ProxyClient(ip, Agent.HTTP_CLIENT_IP));
        }
        ip = request.getHeader(TraceTools.X_REAL_IP);
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            ProxyClient.generateProxyClient(ip, Agent.X_REAL_IP, agent, clientTraceIpSet);
        }
        ip = request.getRemoteAddr();
        if (StringUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            clientTraceIpSet.add(new ProxyClient(ip, Agent.HTTP_CLIENT_IP));
        }
        if (clientTraceIpSet.isEmpty()) {
            clientTraceIpSet.add(new ProxyClient("", Agent.UNKNOWN));
        }
        return clientTraceIpSet;
    }


    public interface AgentType {
        default boolean isMerge() {
            return false;
        }
        String getType();
    }

    @ToString
    @Getter
    public static class ProxyClient {

        private final String ip;
        private final AgentType agentType;

        public ProxyClient(String ip, AgentType agentType) {
            this.ip = ip;
            this.agentType = agentType;
        }

        public static void generateProxyClient(String ip, AgentType agentType, String agent, Set<ProxyClient> proxyClientSet) {
            if (ip.contains(",")) {
                String[] ips = ip.split(",");
                proxyClientSet.add(new ProxyClient(ips[0], () -> agent));
                for (int i = 1; i < ips.length; i++) {
                    proxyClientSet.add(new ProxyClient(ips[i], agentType));
                }
            } else {
                proxyClientSet.add(new ProxyClient(ip, agentType));
            }
        }

        public String getComponent() {
            return this.agentType.isMerge() || StringUtil.isBlank(this.ip) ? this.agentType.getType() : this.agentType.getType() + "(" + this.ip + ")";
        }

        @Override
        public int hashCode() {
            return ip.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ProxyClient) {
                if (obj == this) {
                    return true;
                }
                ProxyClient client = (ProxyClient) obj;
                return this.getIp().equals(client.getIp());
            }
            return false;
        }
    }

    @Getter
    public enum Agent implements AgentType {

        X_FORWARDED_FOR("Nginx", false),
//        X_FORWARDED_FOR("Squid", true),
        PROXY_CLIENT_IP("Apache", false),
        WL_PROXY_CLIENT_IP("WebLogic", false),
        HTTP_CLIENT_IP("Http", false),
        X_REAL_IP("Nginx", true),
        USER("User", false),
        REDIS("Redis", true),
        DB("DB", true),
        UNKNOWN("入口", true);

        private final String type;

        private final boolean merge;

        Agent(String type, boolean merge) {
            this.type = type;
            this.merge = merge;
        }
    }
}
