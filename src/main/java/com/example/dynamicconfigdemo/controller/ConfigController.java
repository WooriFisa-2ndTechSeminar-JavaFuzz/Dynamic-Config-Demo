package com.example.dynamicconfigdemo.controller;

import com.example.dynamicconfigdemo.service.ConfigService;

import java.util.ArrayList;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/save")
    public String save(@RequestParam String key, @RequestParam String value, @RequestParam(required = false) String refKey, Model model) {
        configService.saveConfig(key, value, refKey);
        
        // 저장 직후 바로 평가하여 결과를 UI에 표시 (이때 서버에서 취약점 발현 가능)
        String evaluatedResult = configService.evaluateConfig(key);
        model.addAttribute("result", "평가 결과: " + evaluatedResult);
        
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam String searchKey, Model model) {
        try {
            // 경로 추적 시작
            ConfigService.ConfigResolution resolution = configService.resolveConfigWithHistory(searchKey, new ArrayList<>());
        
            model.addAttribute("searchKey", searchKey);
            model.addAttribute("path", String.join(" -> ", resolution.path));
            model.addAttribute("finalValue", resolution.finalValue);
        } catch (StackOverflowError e) {
            // 순환 참조 발생 시 에러 메시지 표시
            model.addAttribute("error", "StackOverflowError: 순환 참조가 감지되었습니다!");
        }
        return "index";
    }
}