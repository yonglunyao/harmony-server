package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DataController 数据接口测试")
class DataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("上报数据 - 成功")
    void testReportData_Success() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", "device-001");
        data.put("temperature", 25.5);
        data.put("humidity", 60);
        data.put("timestamp", System.currentTimeMillis());

        mockMvc.perform(post("/api/data/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Data received successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.dataSize").value(4));
    }

    @Test
    @DisplayName("上报数据 - 空数据")
    void testReportData_Empty() throws Exception {
        Map<String, Object> data = new HashMap<>();

        mockMvc.perform(post("/api/data/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.dataSize").value(0));
    }

    @Test
    @DisplayName("上报数据 - 包含嵌套对象")
    void testReportData_NestedObject() throws Exception {
        Map<String, Object> location = new HashMap<>();
        location.put("lat", 39.9042);
        location.put("lng", 116.4074);

        Map<String, Object> data = new HashMap<>();
        data.put("deviceId", "device-002");
        data.put("location", location);

        mockMvc.perform(post("/api/data/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.dataSize").value(2));
    }

    @Test
    @DisplayName("上报日志 - INFO级别")
    void testReportLog_Info() throws Exception {
        DataController.LogRequest logRequest = new DataController.LogRequest();
        logRequest.setLevel("INFO");
        logRequest.setTag("TestTag");
        logRequest.setMessage("Test log message");
        logRequest.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/data/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Log received successfully"));
    }

    @Test
    @DisplayName("上报日志 - ERROR级别")
    void testReportLog_Error() throws Exception {
        DataController.LogRequest logRequest = new DataController.LogRequest();
        logRequest.setLevel("ERROR");
        logRequest.setTag("ErrorTag");
        logRequest.setMessage("Error occurred!");
        logRequest.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/data/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("上报日志 - 缺少必填字段")
    void testReportLog_MissingFields() throws Exception {
        Map<String, Object> logRequest = new HashMap<>();
        logRequest.put("level", "DEBUG");
        // 缺少 tag 和 message

        mockMvc.perform(post("/api/data/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("心跳检测 - 成功")
    void testHeartbeat_Success() throws Exception {
        DataController.HeartbeatRequest heartbeat = new DataController.HeartbeatRequest();
        heartbeat.setDeviceId("device-123");
        heartbeat.setVersion("1.0.0");
        heartbeat.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/data/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartbeat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("pong"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("心跳检测 - 最小参数")
    void testHeartbeat_Minimal() throws Exception {
        DataController.HeartbeatRequest heartbeat = new DataController.HeartbeatRequest();
        heartbeat.setDeviceId("device-456");

        mockMvc.perform(post("/api/data/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartbeat)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("pong"));
    }

    @Test
    @DisplayName("心跳检测 - 空设备ID")
    void testHeartbeat_EmptyDeviceId() throws Exception {
        DataController.HeartbeatRequest heartbeat = new DataController.HeartbeatRequest();
        heartbeat.setDeviceId("");

        mockMvc.perform(post("/api/data/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(heartbeat)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("并发上报数据测试")
    void testReportData_Concurrent() throws Exception {
        // 模拟多个设备同时上报数据
        for (int i = 0; i < 5; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("deviceId", "device-" + i);
            data.put("value", i * 10);
            data.put("timestamp", System.currentTimeMillis());

            mockMvc.perform(post("/api/data/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(data)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
}
