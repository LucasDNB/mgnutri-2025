package ar.edu.usal.mgnutri.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/** Ejecutor para tareas en segundo plano (backups). */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "tareasExecutor")
    public Executor tareasExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(2);
        ex.setQueueCapacity(10);
        ex.setThreadNamePrefix("mgnutri-tarea-");
        ex.initialize();
        return ex;
    }
}
