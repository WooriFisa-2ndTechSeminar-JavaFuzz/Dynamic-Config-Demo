# clear corpus
rm -rf .cifuzz-corpus/com.example.dynamicconfigdemo.DynamicConfigDemoApplicationTests/fuzzDynamicConfiguration/*

# clear crash
rm -rf src/test/resources/com/example/dynamicconfigdemo/DynamicConfigDemoApplicationTestsInputs/fuzzDynamicConfiguration/*

# maven clean
mvn clean

# maven cache clear
mvn dependency:purge-local-repository