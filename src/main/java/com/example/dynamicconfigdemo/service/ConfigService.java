package com.example.dynamicconfigdemo.service;

import com.example.dynamicconfigdemo.entity.AppConfig;
import com.example.dynamicconfigdemo.repository.ConfigRepository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

    private final ConfigRepository repository;

    public ConfigService(ConfigRepository repository) {
        this.repository = repository;
    }

    // 저장(Create)
    public void saveConfig(String key, String value, String refKey) {
        if (key == null || key.trim().isEmpty()) return;

        // refKey가 빈 문자열로 들어오면 명시적으로 null로 치환
        String processedRefKey = (refKey != null && refKey.trim().isEmpty()) ? null : refKey;

        repository.save(new AppConfig(key, value, processedRefKey));
    }

    // 취약점이 존재하는 동적 평가 로직
    public String evaluateConfig(String key) {
        AppConfig config = repository.findByConfigKey(key);
        if (config == null) {
            return "Not Found";
        }

        // [취약점 1] StackOverflow: 참조 키가 존재할 경우 재귀 호출 수행 (순환 참조 검증 누락)
        if (config.getReferenceKey() != null && !config.getReferenceKey().trim().isEmpty()) {
            return evaluateConfig(config.getReferenceKey());
        }

        // [취약점 2] RCE: 설정 값을 SpEL 구문으로 파싱하여 실행
        try {
            ExpressionParser parser = new SpelExpressionParser();
            // configValue에 시스템 명령어(예: T(java.lang.Runtime).getRuntime().exec(...))가 포함되면 그대로 실행됨
            return parser.parseExpression(config.getConfigValue()).getValue(String.class);
        } catch (Exception e) {
            // SpEL 문법 오류 발생 시 앱이 죽지 않도록 예외 처리하고 원본 문자열 반환
            // 퍼저가 일반적인 문법 오류를 크래시로 오인하지 않도록 방지
            return config.getConfigValue();
        }
    }

    // 조회(Read)
    public static class ConfigResolution {
        public List<String> path = new ArrayList<>();
        public String finalValue;
    }

    public ConfigResolution resolveConfigWithHistory(String key, List<String> history) {
        ConfigResolution result = new ConfigResolution();
        result.path = history;
        
        AppConfig config = repository.findByConfigKey(key);
        if (config == null) {
            result.finalValue = "Not Found";
            return result;
        }

        result.path.add(key);

        // 참조가 있는 경우 재귀적으로 경로 추적
        if (config.getReferenceKey() != null && !config.getReferenceKey().trim().isEmpty()) {
            return resolveConfigWithHistory(config.getReferenceKey(), result.path);
        }

        // 최종 목적지에 도달하면 SpEL 평가 실행 (RCE 취약점 지점)
        try {
            ExpressionParser parser = new SpelExpressionParser();
            result.finalValue = parser.parseExpression(config.getConfigValue()).getValue(String.class);
        } catch (Exception e) {
            result.finalValue = config.getConfigValue();
        }
        
        return result;
    }
}
