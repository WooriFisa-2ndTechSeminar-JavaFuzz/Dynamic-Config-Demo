pipeline {
    agent any

    environment {
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
                sh 'mvn clean compile -B'
            }
        }

        stage('Fuzzing') {
            steps {
                // 1. 퍼징 실행
                sh 'JAZZER_FUZZ=1 mvn test -Pfuzz -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests'
                
                // 2. 크래시 탐지 및 상태 기록 (즉시 실패하지 않음)
                script {
                    def crashes = sh(
                        script: "find src/test/resources/com/example/dynamicconfigdemo/DynamicConfigDemoApplicationTestsInputs/fuzzDynamicConfiguration/ -name 'crash-*' | wc -l",
                        returnStdout: true
                    ).trim()
                    
                    if (crashes.toInteger() > 0) {
                        env.CRASH_DETECTED = "true"
                        env.CRASH_COUNT = crashes
                        echo "🚨 취약점(크래시) ${crashes}개 감지됨! (추후 배포 차단 예정)"
                        currentBuild.result = 'UNSTABLE' // 빌드 상태를 노란색으로 마킹
                    } else {
                        env.CRASH_DETECTED = "false"
                        echo "✅ 크래시 미발견: 퍼징 통과"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: '**/crash-*, **/target/surefire-reports/*.xml', allowEmptyArchive: true
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Coverage Check') {
            steps {
                script {
                    // 1. 회귀 테스트 및 커버리지 측정 (실패 시에도 파이프라인 진행을 위해 returnStatus: true 사용)
                    def mvnStatus = sh(
                        script: 'mvn test verify -Pregression -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests',
                        returnStatus: true
                    )
                    
                    // 2. jacoco.csv 파일을 읽어 현재 '브랜치 커버리지(Branch Coverage)' 퍼센티지를 정확히 연산
                    // CSV 구조상 6번째 열(BRANCH_MISSED), 7번째 열(BRANCH_COVERED)을 합산하여 비율을 계산합니다.
                    def actualCoverage = sh(
                        script: '''
                            if [ -f target/site/jacoco/jacoco.csv ]; then
                                awk -F"," 'NR>1 {m+=$6; c+=$7} END {if (m+c > 0) printf "%.2f", (c/(m+c))*100; else print "100.00"}' target/site/jacoco/jacoco.csv
                            else
                                echo "0.00"
                            fi
                        ''',
                        returnStdout: true
                    ).trim()
                    
                    env.ACTUAL_COVERAGE = actualCoverage
                    
                    // 3. 브랜치 커버리지 통과 여부 기록
                    if (mvnStatus != 0) {
                        env.COVERAGE_FAILED = "true"
                        echo "⚠️ 커버리지 검증 실패 (현재 브랜치 커버리지: ${actualCoverage}%)"
                        currentBuild.result = 'UNSTABLE'
                    } else {
                        env.COVERAGE_FAILED = "false"
                        echo "✅ 커버리지 검증 통과 (현재 브랜치 커버리지: ${actualCoverage}%)"
                    }
                }
            }
            post {
                always {
                    jacoco(
                        execPattern: '**/target/jacoco.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        inclusionPattern: '**/com/example/dynamicconfigdemo/**'
                    )
                }
            }
        }

        // 최종 품질 게이트 단계
        stage('Quality Gate Decision') {
            steps {
                script {
                    // 수집된 에러 메시지를 담을 리스트
                    def errors = []
                    
                    // 크래시가 발생했던 경우
                    if (env.CRASH_DETECTED == "true") {
                        errors.add("🚨 [보안 결함] 퍼징 중 ${env.CRASH_COUNT}개의 치명적 크래시(취약점)가 발견되었습니다.")
                    }
                    
                    // 커버리지가 미달된 경우
                    if (env.COVERAGE_FAILED == "true") {
                        errors.add("⚠️ [품질 미달] 코드 커버리지 기준을 충족하지 못했습니다. (현재 브랜치 커버리지: ${env.ACTUAL_COVERAGE}%)")
                    }
                    
                    // 에러가 하나라도 있다면 파이프라인을 강제로 실패(FAILURE) 처리하고 합쳐진 메시지 출력
                    if (errors.size() > 0) {
                        def finalErrorMsg = "❌ 품질 게이트 통과 실패로 배포를 차단합니다.\n" + errors.join("\n")
                        error(finalErrorMsg)
                    } else {
                        echo "🎉 모든 품질 게이트(보안 & 커버리지)를 완벽하게 통과했습니다!"
                    }
                }
            }
        }

        stage('Package') {
            steps {
                // 품질 게이트를 통과한 안전한 코드만 패키징됨
                sh 'mvn package -B -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar'
            }
        }
    }

    post {
        failure {
            echo '빌드 실패 — Jenkins 콘솔 로그의 Quality Gate Decision 결과를 확인하세요.'
        }
        success {
            echo '빌드 성공 — 배포 준비 완료'
        }
    }
}