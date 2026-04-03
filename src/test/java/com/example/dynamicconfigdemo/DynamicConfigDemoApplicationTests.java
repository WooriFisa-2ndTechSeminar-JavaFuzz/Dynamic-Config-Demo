package com.example.dynamicconfigdemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;

import com.code_intelligence.jazzer.junit.FuzzTest;
import com.code_intelligence.jazzer.mutation.annotation.NotNull;

import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@SpringBootTest
class DynamicConfigDemoApplicationTests {

	// 시드를 생성하는 메서드
    static Stream<Arguments> fuzzTest() {
        return Stream.of(
            arguments("seed1"),
			arguments("seed2"),
			arguments("seed3")
        );
    }

	@MethodSource
	@FuzzTest(maxDuration = "10m")
	@Test
	void fuzzTest(@NotNull String input) {
		// TODO
	}
}