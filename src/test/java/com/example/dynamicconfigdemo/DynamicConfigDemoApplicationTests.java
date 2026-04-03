package com.example.dynamicconfigdemo;

import com.example.dynamicconfigdemo.service.ConfigService;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
public class DynamicConfigDemoApplicationTests {

    // ----------------------------------------------------------------
    // static: 테스트 클래스 전체에서 컨테이너 하나를 공유
    // 매 테스트마다 컨테이너를 새로 띄우면 퍼징 10분 내내 오버헤드가 커짐
    // ----------------------------------------------------------------
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("fuzzdb")
            .withUsername("fuzz")
            .withPassword("fuzz");

    // ----------------------------------------------------------------
    // Testcontainers는 MySQL을 랜덤 포트로 띄우므로
    // 실제 포트가 확정된 뒤 Spring DataSource 설정을 동적으로 덮어써야 함
    // application.properties의 datasource 설정은 이 값으로 무시됨
    // ----------------------------------------------------------------
    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        // DDL 자동 생성: 컨테이너는 매번 빈 DB로 시작하므로 테이블을 새로 만들어야 함
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ConfigService configService;

    @FuzzTest(maxDuration = "10m")
    public void fuzzDynamicConfiguration(FuzzedDataProvider data) {
        String key1   = data.consumeString(10);
        String value1 = data.consumeString(30);
        String refKey1 = data.consumeString(10);

        String key2   = data.consumeString(10);
        String value2 = data.consumeString(30);
        String refKey2 = data.consumeString(10);

        configService.saveConfig(key1, value1, refKey1);
        configService.saveConfig(key2, value2, refKey2);

        configService.evaluateConfig(key1);
    }
}