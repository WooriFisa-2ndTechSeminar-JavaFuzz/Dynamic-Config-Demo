# Dynamic-config-Demo

테스트 자동화 CI/CD를 실습하기 위한 데모 스프링부트 프로젝트입니다.

## 프로젝트 구조
```plaintext
Dynamic-Config-Demo
├── src/main/java/com/example/dynamicconfigdemo
│   ├── ConfigDemoApplication.java
│   ├── controller
│   │   └── ConfigController.java     (웹 요청 처리 및 UI 연결)
│   ├── entity
│   │   └── AppConfig.java            (DB 테이블 매핑 엔티티)
│   ├── repository
│   │   └── ConfigRepository.java     (Spring Data JPA 인터페이스)
│   └── service
│       └── ConfigService.java        (핵심 비즈니스 로직 및 취약점 포함)
├── src/main/resources
│   ├── application.yml               (MySQL 연결 설정)
│   └── templates
│       └── index.html                (Thymeleaf UI 템플릿)
└── src/test/java/com/example/configdemo
    └── ConfigFuzzTest.java           (Jazzer 그레이박스 퍼징 드라이버)
```

## 유스케이스 예시
| id (PK) | config_key (설정 이름) | config_value (설정 값) | reference_key (참조 설정 이름) |
| --- | --- | --- | --- |
| 1 | max_login_retry | 5 | NULL |
| 2 | welcome_message | 환영합니다! | NULL |
| 3 | event_discount_rate | 0.15 | base_discount_rate |
| 4 | admin_contact | admin@test.com | NULL |