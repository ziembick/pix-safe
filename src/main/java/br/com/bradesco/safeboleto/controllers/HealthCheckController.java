package br.com.bradesco.safeboleto.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<Map<String, String>> healthCheck() {
        // Retorna um JSON simples com status "UP" e um código 200 OK.
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
