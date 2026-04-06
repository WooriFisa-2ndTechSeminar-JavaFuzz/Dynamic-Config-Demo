# ================================================================
# Jenkins CI 이미지 — Spring Boot / Jazzer / JaCoCo / DooD
# 베이스: jenkins/jenkins:2.504-jdk17
# ================================================================
FROM jenkins/jenkins:2.504-jdk17

USER root

# ----------------------------------------------------------------
# 1. 시스템 기본 패키지
#    --no-install-recommends : 권장 패키지 제외 → 이미지 크기 절감
#    rm -rf /var/lib/apt/lists/* : apt 캐시 레이어에서 제거
# ----------------------------------------------------------------
RUN apt-get update && apt-get install -y --no-install-recommends \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
 && rm -rf /var/lib/apt/lists/*

# ----------------------------------------------------------------
# 2. Maven 직접 설치
#    ARG 로 버전을 외부 주입 가능하게 유지
# ----------------------------------------------------------------

ARG MAVEN_VERSION=3.8.7
RUN curl -fsSL \
      https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xz -C /opt \
 && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/local/bin/mvn

# ----------------------------------------------------------------
# 3. Docker CLI 설치 (DooD 패턴 — 데몬 없이 CLI만 설치)
#    Testcontainers 가 호스트 Docker 소켓을 통해 MySQL 컨테이너를 생성
#    chmod a+r : GPG 키 읽기 권한 명시 (일부 환경에서 누락 시 오류)
# ----------------------------------------------------------------
RUN mkdir -p /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg \
      | gpg --dearmor -o /etc/apt/keyrings/docker.gpg \
 && chmod a+r /etc/apt/keyrings/docker.gpg \
 && echo \
      "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
       https://download.docker.com/linux/debian $(lsb_release -cs) stable" \
      | tee /etc/apt/sources.list.d/docker.list > /dev/null \
 && apt-get update && apt-get install -y --no-install-recommends docker-ce-cli \
 && rm -rf /var/lib/apt/lists/*

# ----------------------------------------------------------------
# 4. Docker 그룹 GID 설정
#    호스트의 docker 소켓 GID 와 반드시 일치해야 권한 오류 없음
#    확인 명령: stat -c '%g' /var/run/docker.sock
#    빌드 시 --build-arg DOCKER_GID=$(stat -c '%g' /var/run/docker.sock) 로 주입
# ----------------------------------------------------------------
ARG DOCKER_GID=999
RUN groupadd -g ${DOCKER_GID} docker 2>/dev/null || true \
 && gpasswd -a jenkins docker

# ----------------------------------------------------------------
# 5. Jenkins 플러그인 사전 설치
#    plugins.txt 에 선언된 플러그인 목록을 이미지 빌드 시 설치
#    → 컨테이너 최초 기동 시 플러그인 다운로드 대기 없음
# ----------------------------------------------------------------
COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

USER jenkins