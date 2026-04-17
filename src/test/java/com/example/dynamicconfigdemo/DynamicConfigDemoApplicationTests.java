package com.example.dynamicconfigdemo;

import com.example.dynamicconfigdemo.service.ConfigService;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;
// import com.code_intelligence.jazzer.api.FuzzedDataProvider;

import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.stream.Stream;

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

    // 시드(Corpus)를 생성하는 정적 메서드
    static Stream<Arguments> fuzzDynamicConfiguration() {
        return Stream.of(
            // 시드 1: 정상적인 참조 흐름을 알려주는 기본 데이터 (A -> B 참조)
            arguments("KeyA", "ValueA", "KeyB", "KeyB", "ValueB", ""),
            
            // 시드 2: RCE (SpEL Injection) 취약점 탐색을 유도하는 페이로드 형태
            arguments("EvilKey", "T(java.lang.Runtime).getRuntime().exec('calc')", "", "Dummy", "Val", ""),
            
            // 시드 3: StackOverflow (순환 참조) 취약점 탐색을 유도하는 구조 (Loop1 <-> Loop2)
            arguments("Loop1", "Val1", "Loop2", "Loop2", "Val2", "Loop1")
        );
    }

    // 시드를 주입받아 퍼징을 수행하는 테스트 메서드
    // 순환 참조를 만들어내기 위해 2개의 설정값(총 6개 파라미터)을 동시에 입력
    @MethodSource
    @FuzzTest(maxDuration = "3m")
    void fuzzDynamicConfiguration(
            @NotNull String key1, @NotNull String value1, String refKey1,
            @NotNull String key2, @NotNull String value2, String refKey2) {
        
        // 1. 퍼저가 변조한 데이터를 DB에 저장 (시드 기반으로 변형됨)
        configService.saveConfig(key1, value1, refKey1);
        configService.saveConfig(key2, value2, refKey2);

        // 2. 취약점 트리거 (조회 및 경로 추적 로직 실행)
        // SpEL 인젝션이 성공하거나 무한 재귀에 빠지면 Jazzer가 Crash로 기록
        configService.resolveConfigWithHistory(key1, new ArrayList<>());
    }
}