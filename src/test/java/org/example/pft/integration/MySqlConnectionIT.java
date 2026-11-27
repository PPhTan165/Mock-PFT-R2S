package org.example.pft.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class MySqlConnectionIT {

    @Container
    static MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("pft_integration")
                    .withUsername("pft_test")
                    .withPassword("pft_test_password");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired
    DataSource dataSource;

    @Test
    void shouldConnectToMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertFalse(connection.isClosed());
            assertTrue(
                    connection.getMetaData()
                            .getDatabaseProductName()
                            .contains("MySQL")
            );
        }
    }

    @Test
    void shouldApplyAllFlywayMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery(
                     """
                     SELECT COUNT(*)
                     FROM flyway_schema_history
                     WHERE success = 1
                       AND version IN ('1', '2', '3')
                     """
             )) {

            assertTrue(resultSet.next());
            assertEquals(3, resultSet.getInt(1));
        }
    }
}