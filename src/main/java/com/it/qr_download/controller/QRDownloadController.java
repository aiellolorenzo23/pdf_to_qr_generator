package com.it.qr_download.controller;

import com.it.qr_download.service.QRDownloadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("api/Sound51")
@CrossOrigin
@Tag(name="QR Download Controller", description="Service to create and download QR code based on PDFs for Eleonora Amboni")
@Slf4j
public class QRDownloadController {

    @Autowired
    QRDownloadService service;

    @GetMapping("/generate")
    @ResponseBody
    public ResponseEntity<?> downloadFile(HttpServletRequest request) throws IOException {
        List<Resource> resources = service.loadFilesAsResources();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "qr_codes.zip");

        StreamingResponseBody responseBody = outputStream -> {
            try (ByteArrayOutputStream zipOutputStream = service.generateQR(resources, outputStream)) {
                zipOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        return ResponseEntity.ok()
                .headers(headers)
                .body(responseBody);
    }

    @PostMapping(value = "/uploadPDF", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @ResponseBody
    public ResponseEntity<?> addWsaDealByFile(@RequestParam(value="file", required = true) MultipartFile multipartFile) {
        File file = service.createFileFromMultipart(multipartFile);
        if(file!=null)
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }
}
