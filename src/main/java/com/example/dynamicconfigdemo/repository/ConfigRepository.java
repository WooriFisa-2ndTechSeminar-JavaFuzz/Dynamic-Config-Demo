package com.example.dynamicconfigdemo.repository;

import com.example.dynamicconfigdemo.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigRepository extends JpaRepository<AppConfig, String> {
    AppConfig findByConfigKey(String configKey);
}
