package com.example.yy25gbs35_goods.controller;

import com.example.yy25gbs35_goods.exception.BusinessException;
import com.example.yy25gbs35_goods.goods.Files;
import com.example.yy25gbs35_goods.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Value("${file.upload.path}")
    private String uploadPath;

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer id) throws IOException {
        Files file = fileService.getById(id);
        if (file == null || file.getIsDeleted() == 1) {
            throw new BusinessException(404, "文件不存在或已被删除");
        }

        fileService.incrementDownloadCount(id);
        Path targetPath = Paths.get(uploadPath, file.getFilePath());
        Resource resource = new FileSystemResource(targetPath.toString());

        if (!resource.exists()) {
            throw new BusinessException(404, "文件物理路径不存在");
        }

        String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
                .body(resource);
    }
}
