package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.PreferencesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prefs")
public class PreferencesController {

    private final PreferencesService service;

    public PreferencesController(PreferencesService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public List<Map<String, String>> getPrefs(@PathVariable String userId) {
        return service.getPrefs(userId);
    }

    @PostMapping("/{userId}")
    public void savePref(
            @PathVariable String userId,
            @RequestParam String prefType,
            @RequestParam String valueName) {
        service.savePref(userId, prefType, valueName);
    }
}
