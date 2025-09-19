package com.raman.soccer_backend.controller;

import com.raman.soccer_backend.service.PreferencesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/prefs")
@CrossOrigin(origins = "*") // allow frontend extension to call
public class PreferencesController {

    private final PreferencesService prefsService;

    public PreferencesController(PreferencesService prefsService) {
        this.prefsService = prefsService;
    }

    // Get all preferences for a user
    @GetMapping("/{userId}")
    public List<Map<String, String>> getPrefs(@PathVariable String userId) {
        return prefsService.getPrefs(userId);
    }

    // Save a preference (team or league)
    @PostMapping("/{userId}")
    public void savePref(
            @PathVariable String userId,
            @RequestParam String prefType,
            @RequestParam String valueName
    ) {
        prefsService.savePref(userId, prefType, valueName);
    }

    // Delete all preferences for a user (triggered by Reset button)
    @DeleteMapping("/{userId}")
    public void deletePrefs(@PathVariable String userId) {
        prefsService.deletePrefs(userId);
    }
}
