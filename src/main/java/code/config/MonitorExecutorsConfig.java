package code.config;

import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.guava.ThreadFactoryBuilder;

import java.util.concurrent.*;

@Slf4j
public class MonitorExecutorsConfig {

    private static ThreadFactory threadFactory = new ThreadFactoryBuilder().setDaemon(false).setNameFormat("monitor-pool-%d").build();

    private static ExecutorService fixedThreadPool = new ThreadPoolExecutor(
            2,
            10,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100000),
            threadFactory,
            (Runnable r, ThreadPoolExecutor executor) -> {
                log.error(r.toString()+" is Rejected");
            }
    );

    public static void submit(Runnable task) {
        fixedThreadPool.submit(task);
    }

}
