# maven install - skip tests
mvn install -DskipTests

# fuzzing(bash)
# JAZZER_FUZZ=1 mvn test -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# fuzzing(with profile)
JAZZER_FUZZ=1 mvn test -Pfuzz -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# before doing regression test, copy corpus data to crash directory
# cp .cifuzz-corpus/com.example.dynamicconfigdemo.DynamicConfigDemoApplicationTests/fuzzDynamicConfiguration/* src/test/resources/com/example/dynamicconfigdemo/DynamicConfigDemoApplicationTestsInputs/fuzzDynamicConfiguration/

# regression test
# mvn test -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# regression test(with profile)
mvn test verify -Pregression -Dmaven.test.failure.ignore=true -Djazzer.keep_going=0 -Dtest=DynamicConfigDemoApplicationTests

# run spring boot without tests
# mvn spring-boot:run -DskipTests