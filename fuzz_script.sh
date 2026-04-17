# maven install - skip tests
mvn install -DskipTests

# fuzzing(with profile)
JAZZER_FUZZ=1 mvn test -Pfuzz -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# regression test(with profile)
# mvn test verify -Pregression -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# run spring boot without tests
# mvn spring-boot:run -DskipTests