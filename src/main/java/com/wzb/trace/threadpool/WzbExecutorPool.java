package com.wzb.trace.threadpool;

import com.wzb.trace.network.TraceTools;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public final class WzbExecutorPool {

    private final static int MAX = 100;

    private final static int TIMEOUT = 500;

    private final static long KEEP_ALIVE_TIME = 10;

    private final static int QUEUE_SIZE = 1000;

    private final static Map<String, WzbExecutor> registryMap = new ConcurrentHashMap<>();

    private final static List<WzbExecutor> executorList = new ArrayList<>();

    private final static ReentrantLock lock = new ReentrantLock();

    public static WzbExecutor getExecutor(String name) {
        if (registryMap.containsKey(name)) {
            return registryMap.get(name);
        }
        return newExecutor(name, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE));
    }

    public static WzbExecutor newExecutor(String name, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        try {
            if (lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                if (registryMap.containsKey(name)) {
                    return registryMap.get(name);
                }
                if (executorList.size() < MAX) {
                    int coreSize = Runtime.getRuntime().availableProcessors();
                    WzbExecutor executor = new WzbExecutor(name, coreSize, coreSize, keepAliveTime, unit, workQueue);
                    registryMap.put(name, executor);
                    executorList.add(executor);
                    return executor;
                }
            }
            return null;
        } catch (InterruptedException e) {
            log.error("[Try create executor]", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public static WzbExecutor newExecutor(String name, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        try {
            if (lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                if (registryMap.containsKey(name)) {
                    return registryMap.get(name);
                }
                if (executorList.size() < MAX) {
                    int coreSize = Runtime.getRuntime().availableProcessors();
                    WzbExecutor executor = new WzbExecutor(name, coreSize, coreSize, keepAliveTime, unit, workQueue, threadFactory);
                    registryMap.put(name, executor);
                    executorList.add(executor);
                    return executor;
                }
            }
            return null;
        } catch (InterruptedException e) {
            log.error("[Try create executor]", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public static WzbExecutor newExecutor(String name, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        try {
            if (lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                if (registryMap.containsKey(name)) {
                    return registryMap.get(name);
                }
                if (executorList.size() < MAX) {
                    int coreSize = Runtime.getRuntime().availableProcessors();
                    WzbExecutor executor = new WzbExecutor(name, coreSize, coreSize, keepAliveTime, unit, workQueue, handler);
                    registryMap.put(name, executor);
                    executorList.add(executor);
                    return executor;
                }
            }
            return null;
        } catch (InterruptedException e) {
            log.error("[Try create executor]", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public static WzbExecutor newExecutor(String name, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        try {
            if (lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
                if (registryMap.containsKey(name)) {
                    return registryMap.get(name);
                }
                if (executorList.size() < MAX) {
                    int coreSize = Runtime.getRuntime().availableProcessors();
                    WzbExecutor executor = new WzbExecutor(name, coreSize, coreSize, keepAliveTime, unit, workQueue, threadFactory, handler);
                    registryMap.put(name, executor);
                    executorList.add(executor);
                    return executor;
                }
            }
            return null;
        } catch (InterruptedException e) {
            log.error("[Try create executor]", e);
            return null;
        } finally {
            lock.unlock();
        }
    }

    public static void remove(String name) {
        WzbExecutor executor = registryMap.remove(name);
        if (Objects.nonNull(executor)) {
            executor.shutdownNow();
        }
    }

    public static class WzbExecutor extends ThreadPoolExecutor {

        private final String name;

        public WzbExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
            this.name = name;
        }

        public WzbExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
            this.name = name;
        }

        public WzbExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
            this.name = name;
        }

        public WzbExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
            this.name = name;
        }

        @Override
        public void shutdown() {
            lock.lock();
            WzbExecutor executor = registryMap.remove(name);
            if (Objects.nonNull(executor)) {
                executorList.remove(executor);
            }
            lock.unlock();
            super.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            lock.lock();
            WzbExecutor executor = registryMap.remove(name);
            if (Objects.nonNull(executor)) {
                executorList.remove(executor);
            }
            lock.unlock();
            return super.shutdownNow();
        }

        public String getName() {
            return this.name;
        }

        @Override
        public void execute(Runnable command) {
            super.execute(new WzbRunnable(command, MDC.getCopyOfContextMap(), TraceTools.getCopyOfTraceContextMap()));
        }

    }

    static class WzbRunnable implements Runnable {

        private Map<String, String> contextMap;
        private Map<String, String> traceContextMap;

        private Runnable runnable;

        public WzbRunnable(Runnable runnable, Map<String, String> contextMap, Map<String, String> traceContextMap) {
            this.runnable = runnable;
            this.contextMap = contextMap;
            this.traceContextMap = traceContextMap;
        }

        @Override
        public void run() {
            Optional.ofNullable(this.contextMap).ifPresent(MDC::setContextMap);
            Optional.ofNullable(this.traceContextMap).ifPresent(TraceTools::setTraceContextMap);

            try {
                runnable.run();
            } catch (Throwable throwable) {
                throw throwable;
            } finally {
                contextMap = null;
                TraceTools.clear();
            }
        }
    }

}
