pipeline {
    agent any

    environment {
        // Maven 로컬 캐시를 워크스페이스 내에 격리하여 병렬 빌드 충돌 방지
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // compile만 수행하여 의존성 다운로드 및 문법 오류 사전 확인
                sh 'mvn clean compile -B'
            }
        }

        stage('Fuzzing') {
            steps {
                // 1. Jazzer 퍼징 실행 
                // testFailureIgnore=true 이므로 크래시가 발생해도 Maven은 Exit 0(성공)을 반환하며 종료됨
                sh 'JAZZER_FUZZ=1 mvn test -Pfuzz -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests'
                
                // 2. 크래시 파일 탐지 및 파이프라인 제어 로직
                script {
                    // jazzer-junit이 생성하는 하위 디렉토리(src/test/resources/...)를 모두 포함하여 crash-* 파일을 찾음
                    def crashes = sh(
                        script: "find . -name 'crash-*' | wc -l",
                        returnStdout: true
                    ).trim()
                    
                    if (crashes.toInteger() > 0) {
                        // 크래시가 1개 이상 존재하면 error() 함수를 호출하여 파이프라인을 강제로 FAILURE 상태로 전환하고 중단시킴
                        error("Jazzer 크래시 감지: ${crashes}개 발견 — 빌드 중단")
                    } else {
                        echo "크래시 미발견: 퍼징 단계를 무사히 통과했습니다."
                    }
                }
            }
            post {
                always {
                    // 빌드 성공/실패 여부와 상관없이 발견된 크래시 파일과 Surefire 리포트를 Jenkins UI에 아카이빙
                    archiveArtifacts artifacts: '**/crash-*, **/target/surefire-reports/*.xml', allowEmptyArchive: true
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Coverage Check') {
            steps {
                // verify 페이즈: JaCoCo check goal 실행 (회귀 모드)
                // 코퍼스와 크래시 파일들을 재실행하여 커버리지를 측정하고, pom.xml 기준 미달 시 Maven이 에러를 발생시킴
                sh 'mvn test verify -Pregression -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests'
            }
            post {
                always {
                    // Jenkins 플러그인을 통해 커버리지 결과를 대시보드에 시각화
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        inclusionPattern: '**/com/example/dynamicconfigdemo/**'
                    )
                }
            }
        }

        stage('Package') {
            steps {
                // 앞선 단계에서 검증이 끝났으므로 테스트를 스킵하고 JAR 패키징 수행
                sh 'mvn package -B -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar'
            }
        }
    }

    post {
        failure {
            echo '빌드 실패 — 컴파일 에러 또는 크래시(보안 취약점) 감지 또는 커버리지 기준 미달'
        }
        success {
            echo '빌드 성공 — 퍼징 통과 및 커버리지 기준 충족, 배포 준비 완료'
        }
    }
}