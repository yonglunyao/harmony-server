package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";
    private Path basePath;

    public FileController() {
        try {
            this.basePath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
        } catch (IOException e) {
            log.error("Failed to create upload directory", e);
        }
    }

    /**
     * 安全方法：验证路径是否在允许的基础目录内
     */
    private boolean isPathSafe(Path requestedPath) {
        Path normalized = requestedPath.normalize();
        return normalized.startsWith(basePath);
    }

    /**
     * 安全方法：解析路径并确保安全
     */
    private Path resolveSafePath(String inputPath) throws IOException {
        Path requestedPath = Paths.get(inputPath).normalize();

        // 如果是相对路径，基于上传目录解析
        if (!requestedPath.isAbsolute()) {
            requestedPath = basePath.resolve(requestedPath).normalize();
        }

        // 防止目录遍历攻击
        if (!isPathSafe(requestedPath)) {
            throw new SecurityException("Path traversal detected: " + inputPath);
        }

        // 防止访问上传根目录本身（某些操作）
        if (requestedPath.equals(basePath)) {
            throw new SecurityException("Cannot access upload root directory directly");
        }

        return requestedPath;
    }

    // ==================== POST 接口：修改操作 ====================

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category) {

        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "File is empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String originalFilename = file.getOriginalFilename();

            // 安全检查：防止文件名包含路径遍历字符
            if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
                response.put("success", false);
                response.put("message", "Invalid filename");
                return ResponseEntity.badRequest().body(response);
            }

            Path targetPath = basePath.resolve(originalFilename).normalize();

            // 再次确保路径安全
            if (!isPathSafe(targetPath)) {
                response.put("success", false);
                response.put("message", "Access denied: invalid file path");
                return ResponseEntity.status(403).body(response);
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("filename", originalFilename);
            response.put("size", file.getSize());
            response.put("category", category);
            response.put("path", basePath.relativize(targetPath).toString());

            log.info("File uploaded: {}, size: {}", originalFilename, file.getSize());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/delete/{filename}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String filename) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 安全检查：防止路径遍历
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Path traversal attempt in delete: {}", filename);
                response.put("success", false);
                response.put("message", "Invalid filename");
                return ResponseEntity.status(403).body(response);
            }

            Path filePath = basePath.resolve(filename).normalize();

            // 确保路径在允许的目录内
            if (!isPathSafe(filePath)) {
                log.warn("Access denied to delete: {}", filename);
                response.put("success", false);
                response.put("message", "Access denied: invalid file path");
                return ResponseEntity.status(403).body(response);
            }

            if (!Files.exists(filePath)) {
                response.put("success", false);
                response.put("message", "File not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            long size = Files.size(filePath);
            Files.delete(filePath);

            response.put("success", true);
            response.put("message", "File deleted successfully");
            response.put("filename", filename);
            response.put("size", size);

            log.info("File deleted: {}", filename);
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            response.put("success", false);
            response.put("message", "Failed to delete file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/delete/path")
    public ResponseEntity<Map<String, Object>> deleteByPath(
            @RequestParam(required = false) String path,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();

        String targetPath = path != null ? path : (body != null ? body.get("path") : null);

        if (targetPath == null || targetPath.isEmpty()) {
            response.put("success", false);
            response.put("message", "Path is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Path requestedPath = resolveSafePath(targetPath);

            if (!Files.exists(requestedPath)) {
                response.put("success", false);
                response.put("message", "File or directory not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 记录删除前的信息
            boolean isDirectory = Files.isDirectory(requestedPath);
            String name = requestedPath.getFileName().toString();
            long size = Files.size(requestedPath);

            // 删除文件或目录
            if (isDirectory) {
                // 递归删除目录
                Files.walk(requestedPath)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("Failed to delete: {}", p, e);
                            }
                        });
            } else {
                Files.delete(requestedPath);
            }

            response.put("success", true);
            response.put("message", "Deleted successfully");
            response.put("name", name);
            response.put("path", basePath.relativize(requestedPath).toString());
            response.put("type", isDirectory ? "directory" : "file");
            response.put("size", size);

            log.info("Deleted: {} ({} bytes)", requestedPath, size);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Security exception: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Access denied: " + e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (IOException e) {
            log.error("Failed to delete by path: {}", targetPath, e);
            response.put("success", false);
            response.put("message", "Failed to delete: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/clean")
    public ResponseEntity<Map<String, Object>> cleanUploads() {
        Map<String, Object> response = new HashMap<>();

        try (Stream<Path> stream = Files.walk(basePath)) {
            // 统计要删除的文件
            var filesToDelete = stream
                    .filter(p -> !p.equals(basePath)) // 不删除根目录
                    .sorted((a, b) -> b.compareTo(a)) // 反向排序，先删除文件再删除目录
                    .toList();

            int fileCount = 0;
            int dirCount = 0;
            long totalSize = 0;

            for (Path path : filesToDelete) {
                try {
                    if (Files.isDirectory(path)) {
                        Files.deleteIfExists(path);
                        dirCount++;
                    } else {
                        totalSize += Files.size(path);
                        Files.deleteIfExists(path);
                        fileCount++;
                    }
                } catch (IOException e) {
                    log.warn("Failed to delete: {}", path, e);
                }
            }

            response.put("success", true);
            response.put("message", "Upload directory cleaned successfully");
            response.put("deletedFiles", fileCount);
            response.put("deletedDirectories", dirCount);
            response.put("freedSpace", totalSize);

            log.info("Cleaned uploads: {} files, {} directories, {} bytes freed",
                    fileCount, dirCount, totalSize);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to clean uploads directory", e);
            response.put("success", false);
            response.put("message", "Failed to clean: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ==================== GET 接口：查询操作 ====================

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            // 安全检查：防止路径遍历
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                log.warn("Path traversal attempt in download: {}", filename);
                return ResponseEntity.status(403).build();
            }

            Path filePath = basePath.resolve(filename).normalize();

            // 确保路径在允许的目录内
            if (!isPathSafe(filePath)) {
                log.warn("Access denied to file: {}", filename);
                return ResponseEntity.status(403).build();
            }

            if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
                return ResponseEntity.notFound().build();
            }

            if (Files.isDirectory(filePath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            // 检测 MIME 类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("File downloaded: {}", filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Failed to download file: {}", filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listFiles() {
        Map<String, Object> response = new HashMap<>();

        try {
            var files = Files.list(basePath)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();

            response.put("success", true);
            response.put("files", files);
            response.put("count", files.size());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Failed to list files", e);
            response.put("success", false);
            response.put("message", "Failed to list files: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/list/path")
    public ResponseEntity<Map<String, Object>> listFilesByPath(@RequestParam String path) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path requestedPath = resolveSafePath(path);

            if (!Files.exists(requestedPath)) {
                response.put("success", false);
                response.put("message", "Path does not exist");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            if (!Files.isDirectory(requestedPath)) {
                response.put("success", false);
                response.put("message", "Path is not a directory");
                return ResponseEntity.badRequest().body(response);
            }

            var fileInfoList = Files.list(requestedPath)
                    .map(this::toFileInfo)
                    .toList();

            response.put("success", true);
            response.put("path", basePath.relativize(requestedPath).toString());
            response.put("files", fileInfoList);
            response.put("count", fileInfoList.size());

            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            log.warn("Security exception: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Access denied: " + e.getMessage());
            return ResponseEntity.status(403).body(response);
        } catch (IOException e) {
            log.error("Failed to list files by path: {}", path, e);
            response.put("success", false);
            response.put("message", "Failed to list files: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/download/path")
    public ResponseEntity<Resource> downloadByPath(@RequestParam String path) {
        try {
            Path requestedPath = resolveSafePath(path);

            if (!Files.exists(requestedPath) || !Files.isReadable(requestedPath)) {
                return ResponseEntity.notFound().build();
            }

            if (Files.isDirectory(requestedPath)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(requestedPath.toUri());
            String filename = requestedPath.getFileName().toString();

            // 检测 MIME 类型
            String contentType = Files.probeContentType(requestedPath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            log.info("File downloaded: {}", requestedPath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (SecurityException e) {
            log.warn("Security exception: {}", e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (IOException e) {
            log.error("Failed to download file by path", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> toFileInfo(Path path) {
        Map<String, Object> info = new HashMap<>();
        try {
            info.put("name", path.getFileName().toString());
            info.put("isDirectory", Files.isDirectory(path));
            info.put("isFile", Files.isRegularFile(path));
            info.put("size", Files.size(path));
            info.put("lastModified", Files.getLastModifiedTime(path).toMillis());
        } catch (IOException e) {
            log.warn("Failed to get file info: {}", path, e);
        }
        return info;
    }
}
