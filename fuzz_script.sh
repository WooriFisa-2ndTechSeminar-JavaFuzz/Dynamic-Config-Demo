# maven install - skip tests
mvn install -DskipTests

# fuzzing(bash)
JAZZER_FUZZ=1 mvn test -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# regression test
mvn test -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# run spring boot without tests
mvn spring-boot:run -DskipTests