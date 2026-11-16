package com.problemsolving.platform.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Builds the production datasource from a standard Prisma/PostgreSQL DB_URL. */
@Configuration
@Profile("prod")
public class ProductionDataSourceConfig {

    @Bean
    DataSource productionDataSource(@Value("${DB_URL}") String databaseUrl) {
        DatabaseSettings settings = parse(databaseUrl);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(settings.jdbcUrl());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10_000);
        return new HikariDataSource(config);
    }

    static DatabaseSettings parse(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalArgumentException("DB_URL is required in the prod profile");
        }

        URI uri;
        try {
            uri = URI.create(databaseUrl.trim());
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("DB_URL must be a valid PostgreSQL connection URL", error);
        }

        if (!"postgres".equalsIgnoreCase(uri.getScheme()) && !"postgresql".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("DB_URL must start with postgres:// or postgresql://");
        }
        if (uri.getHost() == null || uri.getRawUserInfo() == null) {
            throw new IllegalArgumentException("DB_URL must include database credentials and a host");
        }

        String[] credentials = uri.getRawUserInfo().split(":", 2);
        if (credentials.length != 2) {
            throw new IllegalArgumentException("DB_URL must include both a username and password");
        }

        String database = uri.getPath();
        if (database == null || database.length() <= 1) {
            throw new IllegalArgumentException("DB_URL must include a database name");
        }

        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            query = "sslmode=require";
        } else if (!query.toLowerCase().contains("sslmode=")) {
            query += "&sslmode=require";
        }

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + "?" + query;
        return new DatabaseSettings(
                jdbcUrl,
                decode(credentials[0]),
                decode(credentials[1]));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    record DatabaseSettings(String jdbcUrl, String username, String password) {
    }
}
