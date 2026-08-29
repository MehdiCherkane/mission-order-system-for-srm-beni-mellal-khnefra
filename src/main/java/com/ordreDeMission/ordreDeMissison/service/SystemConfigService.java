package com.ordreDeMission.ordreDeMissison.service;

import com.ordreDeMission.ordreDeMissison.model.SystemConfig;
import com.ordreDeMission.ordreDeMissison.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SystemConfigService {

    public static final String KEY_DEFAULT_CHEF = "default_chef_matricule";
    public static final String KEY_DEFAULT_DIRECTEUR = "default_directeur_matricule";

    private final SystemConfigRepository repo;

    public SystemConfigService(SystemConfigRepository repo) {
        this.repo = repo;
    }

    public String get(String key) {
        return repo.findById(key).map(SystemConfig::getValue).orElse(null);
    }

    public String getOrDefault(String key, String fallback) {
        String v = get(key);
        return (v == null || v.isBlank()) ? fallback : v;
    }

    public Optional<SystemConfig> find(String key) {
        return repo.findById(key);
    }

    public List<SystemConfig> findAll() {
        return repo.findAll();
    }

    public void save(SystemConfig cfg) {
        repo.save(cfg);
    }

    public void set(String key, String value, String description) {
        SystemConfig cfg = repo.findById(key).orElse(new SystemConfig(key, value, description));
        cfg.setValue(value);
        if (description != null) cfg.setDescription(description);
        repo.save(cfg);
    }
}
