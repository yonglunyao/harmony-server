package org.example.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data")
public class DataController {

    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> reportData(@RequestBody Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();

        log.info("Received data report: {}", data);

        response.put("success", true);
        response.put("message", "Data received successfully");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        response.put("dataSize", data.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/log")
    public ResponseEntity<Map<String, Object>> reportLog(@RequestBody LogRequest logRequest) {
        Map<String, Object> response = new HashMap<>();

        log.info("[{}] {} - {}", logRequest.getLevel(), logRequest.getTag(), logRequest.getMessage());

        response.put("success", true);
        response.put("message", "Log received successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, Object>> heartbeat(@RequestBody HeartbeatRequest heartbeat) {
        Map<String, Object> response = new HashMap<>();

        log.debug("Heartbeat from device: {}", heartbeat.getDeviceId());

        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "pong");

        return ResponseEntity.ok(response);
    }

    @Data
    public static class LogRequest {
        private String level;
        private String tag;
        private String message;
        private Long timestamp;
    }

    @Data
    public static class HeartbeatRequest {
        private String deviceId;
        private String version;
        private Long timestamp;
    }
}
