package com.raman.soccer_backend;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allows extension/frontend to call during dev
public class HealthController {

    @GetMapping("/health")
    public Health ok() {
        return new Health("ok");
    }

    record Health(String status) {}
}
