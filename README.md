# Dynamic-config-Demo

## 📝 프로젝트 소개
Jazzer와 JaCoCo 기반 테스트 자동화 CI/CD를 실습하기 위한 프로젝트

## 💡 핵심 개념
* **CI/CD(Continuous Integration/Continuous Deployment):** 코드 변경 시 빌드-테스트-배포 과정을 자동으로 실행해, 안정적으로 배포 가능한 상태를 유지하는 개발 방식
* **Jenkins:** 대표적인 CI/CD 툴로 `Jenkinsfile` 스크립트로 관리한다.
* **Testcontainers:** 테스트 실행 시 필요한 인프라(예: DB)를 컨테이너로 자동으로 띄워 재현 가능한 통합 테스트를 지원하는 라이브러리. 테스트가 끝나면 컨테이너는 자동 폐기된다.

## 🛠️ 사용 기술 스택
* **Web Application:** ![Spring Boot](https://img.shields.io/badge/Spring%20boot-ED8B00?style=for-the-badge&logo=springboot&logoColor=white)
* **Testing & Analysis:** ![Jazzer](https://img.shields.io/badge/Jazzer-4B32C3?style=for-the-badge&logo=jazzer&logoColor=white)
![Jenkins JaCoCo Plugin](https://img.shields.io/badge/Jenkins%20JaCoCo%20Plugin-E10098?style=for-the-badge)
![Testcontainers](https://img.shields.io/badge/Testcontainers-22b2d2?style=for-the-badge)
* **Build Tool:** ![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

## 🏗️ 프로젝트 구조
```plaintext
Dynamic-Config-Demo/
├──.cifuzz-corpus/  # 코퍼스
│   └── com.example.dynamicconfigdemo.DynamicConfigDemoApplicationTests
│       └── fuzzDynamicConfiguration
│           ├── 6e7e881c994a86b61dc4c750aead3cfb62385f88
│           ├── 7bdb4e165d34867c147c41330308d65d4171dc2b
│           ├── 7c8708acfb24d4b314d7a6290118d0411a5100c3
│           ├── 70dabbb27af6f27f756457400bb9f16defb6aba5

(생략)

├── src/main/java/com/example/dynamicconfigdemo
│   ├── ConfigDemoApplication.java
│   ├── controller
│   │   └── ConfigController.java    # 웹 요청 처리 및 UI 연결
│   ├── entity
│   │   └── AppConfig.java           # DB 테이블 매핑 엔티티
│   ├── repository
│   │   └── ConfigRepository.java    # Spring Data JPA 인터페이스
│   └── service
│       └── ConfigService.java       # 핵심 비즈니스 로직 및 취약점 포함
├── src/main/resources
│   ├── application.yml              # MySQL 연결 설정
│   └── templates
│       └── index.html               # Thymeleaf UI 템플릿
├── src/test/java/com/example/dynamicconfigdemo
│   └── DynamicConfigDemoApplicationTests.java     # Jazzer 그레이박스 퍼징 드라이버
├── src/test/resources/com/example/dynamicconfigdemo/DynamicConfigDemoApplicationTestsInputs
│       └── fuzzDynamicConfiguration
│            └── crash-34b3c6a1db6cc302884fce9a243a6a8a34660e32
└── target
    ├── site
    │   └── jacoco  # JaCoCo 커버리지 리포트
    │       ├── index.html

(생략)

    ├── surefire-reports
    │   └── com.example.SnakeYamlFuzzTest.txt  # 회귀 테스트 결과 에러 로그

(생략)

├── fuzz_script.sh      # 테스트 및 앱 실행 스크립트
├── clear.sh            # 테스트 결과 초기화 스크립트
├── Dockerfile          # 젠킨스 이미지 빌드 스크립트
├── docker-compose.yaml # 젠킨스 컨테이너 생성 스크립트
├── Jenkinsfile         # 젠킨스 설정 스크립트
└── plugins.txt         # 젠킨스에 설치하는 플러그인
```

## ▶️ 테스트 실행 방법

> `fuzz_script.sh`, `clear.sh` 참고

```bash
# 테스트 및 퍼징 실행(환경에 맞게 스크립트 사용)
JAZZER_FUZZ=1 mvn test -Pfuzz -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# 회귀 테스트 실행 및 커버리지 기록
mvn test verify -Pregression -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# 스프링 부트 웹 실행
mvn spring-boot:run -DskipTests

# 테스트 초기화
./clear.sh
```

생성된 커버리지 리포트는 `target/site/jacoco/index.html`에서 확인할 수 있다.

## 🌿 테스트용 스프링 부트 앱 소개

이 앱은 **동적 설정(Dynamic Configuration)** 을 웹에서 수정하고 즉시 반영하는 컨셉의 간이 Spring Boot 사이트다.

핵심 동작은 다음과 같다.
* 각 설정은 `Key`, `Value`, `Reference key`를 가진다.
* `Reference key`를 통해 다른 설정 값을 참조할 수 있다.
* `Value`에는 SpEL(Spring Expression Language) 구문 입력을 허용한다.

실습을 위해 아래 보안 취약점을 **의도적으로** 포함했다.
* 참조키 해석 시 순환 참조(circular reference)가 발생할 수 있지만, 순환 횟수 제한 로직이 없다.
* SpEL 구문 입력을 받을 때 신뢰할 수 있는 입력인지 검증/제한하는 로직이 없다.

즉, 이 프로젝트의 목적은 "안전한 예제"가 아니라, 취약 코드가 CI/CD에서 어떻게 탐지되는지 관찰하는 것이다. Jenkins 파이프라인에서 테스트(퍼징/회귀/커버리지) 단계 결과를 기준으로 품질 게이트를 통과하지 못하면 배포를 중단하도록 구성해, **테스트 단계에서 배포가 차단되는 흐름**을 확인하는 데 초점을 둔다.

### 유스케이스 예시
| id (PK) | key (설정 이름) | alue (설정 값) | reference key (참조 설정 이름) |
| --- | --- | --- | --- |
| 1 | max_login_retry | 5 | NULL |
| 2 | welcome_message | 환영합니다! | NULL |
| 3 | event_discount_rate | 0.15 | base_discount_rate |
| 4 | admin_contact | admin@test.com | NULL |

## 🔎 젠킨스 파이프라인 상세
### 테스트 통과 조건
* Jazzer 그레이 박스 퍼징 결과 Crash가 단 한 개도 발견되어선 안됨
* 테스트 결과, 브랜치 커버리지가 60% 이상 달성되어야 함

### 파이프라인 실행 모습

## 🧪 CI/CD 파이프라인 관찰 포인트

