package com.it.qr_download.controller;

import com.it.qr_download.service.QRDownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping
@Tag(name="QR Download Controller", description="Service to create and download QR code based on PDFs for Eleonora Amboni")
@Slf4j
public class QRDownloadController {

    @Autowired
    QRDownloadService service;

    @GetMapping("/generate")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) throws IOException {
        List<Resource> resources = service.loadFilesAsResources();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "qr_codes.zip");

        ByteArrayResource zipResource = new ByteArrayResource(service.generateQR(resources).toByteArray());

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipResource.contentLength())
                .body(zipResource);
    }

    @PostMapping(value = "/uploadPDF", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @ResponseBody
    public ResponseEntity<?> uploadPDF(@RequestParam(value="file", required = true) MultipartFile multipartFile) {
        File file = service.createFileFromMultipart(multipartFile);
        if(file!=null)
            return ResponseEntity.ok().body("File uploaded successfully");
        return ResponseEntity.notFound().build();
    }
}
