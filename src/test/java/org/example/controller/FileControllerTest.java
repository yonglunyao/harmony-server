package org.example.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FileController 文件操作测试")
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String UPLOAD_DIR = "uploads";
    private static final String TEST_FILENAME = "test-file.txt";
    private static final String TEST_CONTENT = "Hello, HarmonyOS!";

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理所有测试文件和目录
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (Files.exists(uploadPath)) {
            Files.walk(uploadPath)
                    .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件
                    .forEach(p -> {
                        try {
                            if (!p.equals(uploadPath)) // 不删除根目录
                                Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    @Test
    @DisplayName("上传文件 - 成功")
    void testUploadFile_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file)
                        .param("category", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File uploaded successfully"))
                .andExpect(jsonPath("$.filename").value(TEST_FILENAME))
                .andExpect(jsonPath("$.size").value(TEST_CONTENT.length()))
                .andExpect(jsonPath("$.category").value("test"))
                .andExpect(jsonPath("$.path").exists());
    }

    @Test
    @DisplayName("上传文件 - 空文件失败")
    void testUploadFile_EmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    @DisplayName("上传文件 - 无category参数")
    void testUploadFile_WithoutCategory() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                TEST_FILENAME,
                "text/plain",
                TEST_CONTENT.getBytes()
        );

        mockMvc.perform(multipart("/api/file/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.category").isEmpty());
    }

    @Test
    @DisplayName("下载文件 - 成功")
    void testDownloadFile_Success() throws Exception {
        // 先上传文件
        Path testFile = Paths.get(UPLOAD_DIR, TEST_FILENAME);
        Files.writeString(testFile, TEST_CONTENT);

        mockMvc.perform(get("/api/file/download/" + TEST_FILENAME))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(TEST_CONTENT.getBytes()));
    }

    @Test
    @DisplayName("下载文件 - 文件不存在")
    void testDownloadFile_NotFound() throws Exception {
        mockMvc.perform(get("/api/file/download/nonexistent.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("删除文件 - 成功")
    void testDeleteFile_Success() throws Exception {
        // 先创建文件
        Path testFile = Paths.get(UPLOAD_DIR, TEST_FILENAME);
        Files.writeString(testFile, TEST_CONTENT);

        mockMvc.perform(post("/api/file/delete/" + TEST_FILENAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File deleted successfully"))
                .andExpect(jsonPath("$.filename").value(TEST_FILENAME));

        // 验证文件已删除
        assert !Files.exists(testFile);
    }

    @Test
    @DisplayName("删除文件 - 文件不存在")
    void testDeleteFile_NotFound() throws Exception {
        mockMvc.perform(post("/api/file/delete/nonexistent.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("列出文件 - 成功")
    void testListFiles_Success() throws Exception {
        // 创建测试文件
        Files.writeString(Paths.get(UPLOAD_DIR, "file1.txt"), "content1");
        Files.writeString(Paths.get(UPLOAD_DIR, "file2.txt"), "content2");

        mockMvc.perform(get("/api/file/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.count").value(greaterThanOrEqualTo(2)));

        // 清理
        Files.deleteIfExists(Paths.get(UPLOAD_DIR, "file1.txt"));
        Files.deleteIfExists(Paths.get(UPLOAD_DIR, "file2.txt"));
    }

    @Test
    @DisplayName("列出文件 - 空目录")
    void testListFiles_Empty() throws Exception {
        mockMvc.perform(get("/api/file/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    // ==================== 按路径查询文件列表测试 ====================

    @Test
    @DisplayName("按路径查询文件列表 - 成功")
    void testListFilesByPath_Success() throws Exception {
        // 创建测试目录和文件
        Path subDir = Paths.get(UPLOAD_DIR, "subfolder");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("file1.txt"), "content1");
        Files.writeString(subDir.resolve("file2.json"), "{\"key\":\"value\"}");

        mockMvc.perform(get("/api/file/list/path")
                        .param("path", "subfolder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.count").value(2));

        // 清理
        Files.deleteIfExists(subDir.resolve("file1.txt"));
        Files.deleteIfExists(subDir.resolve("file2.json"));
        Files.deleteIfExists(subDir);
    }

    @Test
    @DisplayName("按路径查询文件列表 - 嵌套目录")
    void testListFilesByPath_NestedDirectory() throws Exception {
        // 创建嵌套目录结构
        Path nestedDir = Paths.get(UPLOAD_DIR, "level1", "level2");
        Files.createDirectories(nestedDir);
        Files.writeString(nestedDir.resolve("nested.txt"), "nested content");

        mockMvc.perform(get("/api/file/list/path")
                        .param("path", "level1/level2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.count").value(greaterThanOrEqualTo(1)));

        // 清理
        Files.deleteIfExists(nestedDir.resolve("nested.txt"));
        Files.deleteIfExists(nestedDir);
        Files.deleteIfExists(Paths.get(UPLOAD_DIR, "level1"));
    }

    @Test
    @DisplayName("按路径查询文件列表 - 目录不存在")
    void testListFilesByPath_NotFound() throws Exception {
        mockMvc.perform(get("/api/file/list/path")
                        .param("path", "nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Path does not exist"));
    }

    @Test
    @DisplayName("按路径查询文件列表 - 路径遍历攻击防护")
    void testListFilesByPath_PathTraversal() throws Exception {
        mockMvc.perform(get("/api/file/list/path")
                        .param("path", "../../../etc"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("按路径查询文件列表 - 路径是文件而非目录")
    void testListFilesByPath_IsFileNotDirectory() throws Exception {
        // 创建一个文件
        Path testFile = Paths.get(UPLOAD_DIR, "notdir.txt");
        Files.writeString(testFile, "content");

        mockMvc.perform(get("/api/file/list/path")
                        .param("path", "notdir.txt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Path is not a directory"));

        // 清理
        Files.deleteIfExists(testFile);
    }

    // ==================== 按路径下载文件测试 ====================

    @Test
    @DisplayName("按路径下载文件 - Query参数成功")
    void testDownloadByPath_QueryParam() throws Exception {
        Path subDir = Paths.get(UPLOAD_DIR, "downloads");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("test.pdf");
        Files.writeString(testFile, "PDF content");

        mockMvc.perform(get("/api/file/download/path")
                        .param("path", "downloads/test.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes("PDF content".getBytes()));

        // 清理
        Files.deleteIfExists(testFile);
        Files.deleteIfExists(subDir);
    }

    @Test
    @DisplayName("按路径下载文件 - 文件不存在")
    void testDownloadByPath_NotFound() throws Exception {
        mockMvc.perform(get("/api/file/download/path")
                        .param("path", "nonexistent/file.txt"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("按路径下载文件 - 路径遍历攻击防护")
    void testDownloadByPath_PathTraversal() throws Exception {
        mockMvc.perform(get("/api/file/download/path")
                        .param("path", "../../../etc/passwd"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("按路径下载文件 - 路径为空")
    void testDownloadByPath_EmptyPath() throws Exception {
        mockMvc.perform(get("/api/file/download/path"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("按路径下载文件 - 目录而非文件")
    void testDownloadByPath_IsDirectory() throws Exception {
        Path dir = Paths.get(UPLOAD_DIR, "testdir");
        Files.createDirectories(dir);

        mockMvc.perform(get("/api/file/download/path")
                        .param("path", "testdir"))
                .andExpect(status().isBadRequest());

        // 清理
        Files.deleteIfExists(dir);
    }

    // ==================== 按路径删除文件测试 ====================

    @Test
    @DisplayName("按路径删除文件 - Query参数成功")
    void testDeleteByPath_QueryParam() throws Exception {
        Path subDir = Paths.get(UPLOAD_DIR, "todelete");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("delete.txt");
        Files.writeString(testFile, "to be deleted");

        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", "todelete/delete.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Deleted successfully"))
                .andExpect(jsonPath("$.name").value("delete.txt"))
                .andExpect(jsonPath("$.type").value("file"));

        // 验证文件已删除
        assert !Files.exists(testFile);

        // 清理
        Files.deleteIfExists(subDir);
    }

    @Test
    @DisplayName("按路径删除文件 - RequestBody成功")
    void testDeleteByPath_RequestBody() throws Exception {
        Path testFile = Paths.get(UPLOAD_DIR, "bodydelete.txt");
        Files.writeString(testFile, "content");

        mockMvc.perform(post("/api/file/delete/path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"bodydelete.txt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 验证文件已删除
        assert !Files.exists(testFile);
    }

    @Test
    @DisplayName("按路径删除文件 - 删除非空目录")
    void testDeleteByPath_NonEmptyDirectory() throws Exception {
        Path dir = Paths.get(UPLOAD_DIR, "dirToDelete");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("file1.txt"), "content1");
        Files.writeString(dir.resolve("file2.txt"), "content2");

        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", "dirToDelete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.type").value("directory"));

        // 验证目录已删除
        assert !Files.exists(dir);
    }

    @Test
    @DisplayName("按路径删除文件 - 文件不存在")
    void testDeleteByPath_NotFound() throws Exception {
        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", "nonexistent.txt"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("按路径删除文件 - 路径遍历攻击防护")
    void testDeleteByPath_PathTraversal() throws Exception {
        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", "../../../etc/passwd"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    @Test
    @DisplayName("按路径删除文件 - 尝试删除根目录")
    void testDeleteByPath_RootDirectory() throws Exception {
        // 获取 uploads 目录的绝对路径
        Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath();
        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", uploadPath.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("access upload root")));
    }

    @Test
    @DisplayName("按路径删除文件 - 路径为空")
    void testDeleteByPath_EmptyPath() throws Exception {
        mockMvc.perform(post("/api/file/delete/path"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Path is required"));
    }

    @Test
    @DisplayName("按路径删除文件 - 删除嵌套目录")
    void testDeleteByPath_NestedDirectory() throws Exception {
        Path nestedDir = Paths.get(UPLOAD_DIR, "parent", "child", "grandchild");
        Files.createDirectories(nestedDir);
        Files.writeString(nestedDir.resolve("deep.txt"), "deep content");

        mockMvc.perform(post("/api/file/delete/path")
                        .param("path", "parent/child/grandchild"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.type").value("directory"));

        // 验证目录已删除
        assert !Files.exists(nestedDir);

        // 清理父目录（由 tearDown 处理，但这里确保不影响其他测试）
        Path parentDir = Paths.get(UPLOAD_DIR, "parent");
        if (Files.exists(parentDir)) {
            Files.walk(parentDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    // ==================== 清空上传目录测试 ====================

    @Test
    @DisplayName("清空上传目录 - 成功")
    void testCleanUploads_Success() throws Exception {
        // 创建测试文件和目录
        Files.writeString(Paths.get(UPLOAD_DIR, "file1.txt"), "content1");
        Files.writeString(Paths.get(UPLOAD_DIR, "file2.txt"), "content2");
        Path subDir = Paths.get(UPLOAD_DIR, "subdir");
        Files.createDirectories(subDir);
        Files.writeString(subDir.resolve("file3.txt"), "content3");

        mockMvc.perform(post("/api/file/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Upload directory cleaned successfully"))
                .andExpect(jsonPath("$.deletedFiles").value(3))
                .andExpect(jsonPath("$.deletedDirectories").value(1))
                .andExpect(jsonPath("$.freedSpace").value(greaterThan(0)));

        // 验证文件已清空
        mockMvc.perform(get("/api/file/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @DisplayName("清空上传目录 - 空目录")
    void testCleanUploads_Empty() throws Exception {
        mockMvc.perform(post("/api/file/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.deletedFiles").value(0))
                .andExpect(jsonPath("$.deletedDirectories").value(0));
    }
}
