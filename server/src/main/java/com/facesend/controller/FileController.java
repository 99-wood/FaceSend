package com.facesend.controller;

import com.facesend.dto.ApiResponse;
import com.facesend.entity.FileInfo;
import com.facesend.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "*")
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<Map<String, Object>> uploadFile(
            @RequestParam Long roomId,
            @RequestParam String uploaderId,
            @RequestParam MultipartFile file) {
        try {
            FileInfo fileInfo = fileService.uploadFile(roomId, uploaderId, file);
            return ApiResponse.success(Map.of(
                    "fileId", fileInfo.getId(),
                    "fileName", fileInfo.getFileName(),
                    "fileSize", fileInfo.getFileSize()
            ));
        } catch (Exception e) {
            return ApiResponse.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/list/{roomId}")
    public ApiResponse<List<FileInfo>> listFiles(@PathVariable Long roomId) {
        List<FileInfo> files = fileService.getFilesByRoomId(roomId);
        return ApiResponse.success(files);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> downloadFile(@PathVariable Long fileId) {
        try {
            System.out.println("[下载接口] 请求文件ID: " + fileId);
            
            Optional<FileInfo> fileInfoOpt = fileService.getFileById(fileId);
            if (fileInfoOpt.isEmpty()) {
                System.out.println("[下载接口] 文件记录不存在!");
                return ResponseEntity.notFound().build();
            }
            
            FileInfo fileInfo = fileInfoOpt.get();
            System.out.println("[下载接口] 找到文件: " + fileInfo.getFileName() + ", 存储路径: " + fileInfo.getStoredPath());
            
            InputStream is = fileService.downloadFile(fileId);
            org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(is);
            
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(fileInfo.getFileName(), StandardCharsets.UTF_8)
                    .build();
            
            System.out.println("[下载接口] 准备返回文件流");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(fileInfo.getFileSize()))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("[下载接口] 异常!");
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
