package com.problemsolving.platform.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionDataSourceConfigTest {

    @Test
    void convertsPrismaUrlToJdbcSettings() {
        ProductionDataSourceConfig.DatabaseSettings settings = ProductionDataSourceConfig.parse(
                "postgres://user%40app:p%40ss%2Bword@db.prisma.io:5432/postgres?sslmode=require");

        assertEquals("jdbc:postgresql://db.prisma.io:5432/postgres?sslmode=require", settings.jdbcUrl());
        assertEquals("user@app", settings.username());
        assertEquals("p@ss+word", settings.password());
    }

    @Test
    void suppliesPostgresDefaults() {
        ProductionDataSourceConfig.DatabaseSettings settings = ProductionDataSourceConfig.parse(
                "postgresql://user:password@example.com/app");

        assertEquals("jdbc:postgresql://example.com:5432/app?sslmode=require", settings.jdbcUrl());
    }

    @Test
    void rejectsNonPostgresUrls() {
        assertThrows(IllegalArgumentException.class,
                () -> ProductionDataSourceConfig.parse("prisma+postgres://example.com/database"));
    }
}
