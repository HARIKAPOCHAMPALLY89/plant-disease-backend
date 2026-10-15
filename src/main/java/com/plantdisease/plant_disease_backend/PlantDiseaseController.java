package com.plantdisease.plant_disease_backend;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PlantDiseaseController {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("http://127.0.0.1:5000")
            .build();

    @PostMapping(
        value = "/detect",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Object>> detectDisease(
            @RequestParam("image") MultipartFile image) {

        Map<String, Object> response = new LinkedHashMap<>();

        if (image == null || image.isEmpty()) {
            response.put("success", false);
            response.put("message", "Please upload a plant leaf image.");
            return ResponseEntity.badRequest().body(response);
        }

        if (image.getContentType() == null
                || !image.getContentType().startsWith("image/")) {
            response.put("success", false);
            response.put("message", "Please upload a valid image.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            ByteArrayResource imageResource =
                    new ByteArrayResource(image.getBytes()) {
                        @Override
                        public String getFilename() {
                            String name = image.getOriginalFilename();
                            return name == null ? "leaf.jpg" : name;
                        }
                    };

            MultiValueMap<String, Object> body =
                    new LinkedMultiValueMap<>();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    image.getContentType()));

            org.springframework.http.HttpEntity<ByteArrayResource>
                    imageEntity =
                    new org.springframework.http.HttpEntity<>(
                            imageResource, headers);

            body.add("image", imageEntity);

            Map<?, ?> aiResult = restClient.post()
                    .uri("/api/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (aiResult == null
                    || !Boolean.TRUE.equals(aiResult.get("success"))) {
                response.put("success", false);
                response.put("message", "The AI model could not analyze this image.");
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(response);
            }

            String disease = String.valueOf(aiResult.get("disease"));
            Object confidence = aiResult.get("confidence");

            response.put("success", true);
            response.put("fileName", image.getOriginalFilename());
            response.put("fileSize", image.getSize());
            response.put("disease", disease);
            response.put("confidence", confidence);
            response.put("severity", "Requires further assessment");
            response.put("treatment",
                    "Follow crop-specific agricultural guidance for the predicted class.");
            response.put("detectionType", "AI image classification");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Could not read the uploaded image.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message",
                    "Could not connect to the AI service. Check that it is running on port 5000.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(response);
        }
    }
}