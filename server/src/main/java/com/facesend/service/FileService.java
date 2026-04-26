package com.facesend.service;

import com.facesend.entity.FileInfo;
import com.facesend.repository.FileInfoRepository;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private void ensureBucketExists() throws Exception {
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    @Transactional
    public FileInfo uploadFile(Long roomId, String uploaderId, MultipartFile file) throws Exception {
        ensureBucketExists();
        String storedPath = roomId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storedPath)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        FileInfo fileInfo = new FileInfo();
        fileInfo.setRoomId(roomId);
        fileInfo.setUploaderId(uploaderId);
        fileInfo.setFileName(file.getOriginalFilename());
        fileInfo.setStoredPath(storedPath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setMimeType(file.getContentType());
        
        return fileInfoRepository.save(fileInfo);
    }

    public List<FileInfo> getFilesByRoomId(Long roomId) {
        return fileInfoRepository.findByRoomId(roomId);
    }

    public Optional<FileInfo> getFileById(Long fileId) {
        return fileInfoRepository.findById(fileId);
    }

    public InputStream downloadFile(Long fileId) throws Exception {
        Optional<FileInfo> fileInfoOpt = fileInfoRepository.findById(fileId);
        if (fileInfoOpt.isEmpty()) {
            throw new RuntimeException("File not found");
        }
        FileInfo fileInfo = fileInfoOpt.get();
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileInfo.getStoredPath())
                        .build()
        );
    }

    public void deleteAllFilesByRoomId(Long roomId) {
        List<FileInfo> files = fileInfoRepository.findByRoomId(roomId);
        for (FileInfo file : files) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(file.getStoredPath())
                                .build()
                );
            } catch (Exception e) {
            }
        }
        fileInfoRepository.deleteAll(files);
    }
}
