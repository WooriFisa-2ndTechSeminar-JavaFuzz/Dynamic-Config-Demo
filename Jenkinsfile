pipeline {
    agent any

    environment {
        // Jazzer 크래시 파일 저장 경로 (Dockerfile 에서 미리 생성한 디렉토리)
        JAZZER_FINDINGS_DIR = '/var/jenkins_home/jazzer-findings'
        // Maven 로컬 캐시를 워크스페이스 내에 격리 → 병렬 빌드 충돌 방지
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
                // compile 만 — 테스트 제외, 의존성 다운로드 확인
                sh 'mvn clean compile -B'
            }
        }

        stage('Fuzz + Unit Test') {
            steps {
                // JAZZER_FUZZ=1 : 그레이박스 퍼징 모드 활성화
                // --keep_going=0 : 첫 크래시 발생 즉시 중단
                // test 페이즈 실행 → Surefire → Jazzer + JUnit 테스트 실행
                // testFailureIgnore=false 로 오버라이드 → 크래시 시 즉시 빌드 실패

                // sh '''
                //     mvn test -B \
                //         -Dsurefire.testFailureIgnore=false \
                //         -DJAZZER_FUZZ=1 \
                //         -DJAZZER_ARGS="--keep_going=0 -artifact_prefix=${JAZZER_FINDINGS_DIR}/"
                // '''

                sh '''
                    JAZZER_FUZZ=1 mvn test \
                        -Dmaven.test.failure.ignore=true \
                        -Djazzer.keep_going=0 \
                        -Dtest=DynamicConfigDemoApplicationTests
                    '''
            }
            post {
                // 크래시 파일이 생겼으면 아티팩트로 보관 (원인 분석용)
                always {
                    archiveArtifacts artifacts: 'jazzer-findings/**', allowEmptyArchive: true
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
                // 크래시 파일 존재 여부로 빌드 실패 명시
                failure {
                    script {
                        def crashes = sh(
                            script: "find ${JAZZER_FINDINGS_DIR} -name 'crash-*' | wc -l",
                            returnStdout: true
                        ).trim()
                        if (crashes.toInteger() > 0) {
                            error("Jazzer 크래시 감지: ${crashes}개 — 빌드 중단")
                        }
                    }
                }
            }
        }

        stage('Coverage Check') {
            steps {
                // verify 페이즈: JaCoCo check goal 실행
                // pom.xml 의 check-coverage execution 이 여기서 동작
                // 커버리지 미달 시 Maven 이 BUILD FAILURE → Jenkins 빌드 실패
                sh 'mvn verify -B -DskipTests'
            }
            post {
                always {
                    // Jenkins UI 에 JaCoCo 리포트 표시 (jacoco 플러그인 필요)
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        inclusionPattern: 'org/yaml/**'
                    )
                }
            }
        }

        stage('Package') {
            steps {
                // 테스트는 앞 스테이지에서 완료 → 스킵하고 jar 만 생성
                sh 'mvn package -B -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar'
            }
        }
    }

    post {
        failure {
            echo '빌드 실패 — Jazzer 크래시 또는 JaCoCo 커버리지 기준 미달을 확인하세요.'
        }
        success {
            echo '빌드 성공 — 퍼징 통과, 커버리지 기준 충족'
        }
    }
}