package com.plantdisease.plant_disease_backend;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "Plant Disease Backend is running!";
    }

    @PostMapping("/api/detect")
    public String detectDisease(@RequestParam("image") MultipartFile image) {

        return "Image received successfully! Disease detection AI will be connected next.";
    }
}