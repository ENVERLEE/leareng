package com.leareng.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Bean
    @Primary
    public DataSource dataSource() {
        // URL 로깅 (비밀번호 제외)
        logger.info("=== Oracle Database Connection Info ===");
        logger.info("JDBC URL: {}", jdbcUrl != null ? jdbcUrl.replaceAll("password=[^;]*", "password=***") : "NULL");
        logger.info("Username: {}", username);
        logger.info("========================================");
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        
        // Oracle 연결 풀 설정
        config.setConnectionTimeout(30000);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Oracle 특정 설정
        config.addDataSourceProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        config.addDataSourceProperty("oracle.jdbc.ReadTimeout", "30000");
        
        return new HikariDataSource(config);
    }
}

