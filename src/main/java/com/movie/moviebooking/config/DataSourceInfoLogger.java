package com.movie.moviebooking.config;

import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSourceInfoLogger implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataSourceInfoLogger.class);
    private final DataSource dataSource;

    public DataSourceInfoLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            String className = dataSource.getClass().getName();
            log.info("DataSource implementation: {}", className);
            try (var conn = dataSource.getConnection()) {
                log.info("Connected to database: {}", conn.getMetaData().getURL());
            }
        } catch (Exception ex) {
            log.error("Unable to get DataSource connection: {}", ex.getMessage());
        }
    }
}
