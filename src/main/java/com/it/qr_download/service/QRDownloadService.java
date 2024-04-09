package com.it.qr_download.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.it.qr_download.config.FileStorageProperties;
import com.it.qr_download.exception.FileNotFoundException;
import com.it.qr_download.exception.FileStorageException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class QRDownloadService {

    private final Path fileStorageLocation;

    @Autowired
    public QRDownloadService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public List<Resource> loadFilesAsResources() {
        List<Resource> resources = new ArrayList<>();
        try {
            Path directoryPath = this.fileStorageLocation.normalize();
            if (Files.isDirectory(directoryPath)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)) {
                    for (Path filePath : directoryStream) {
                        Resource resource = new UrlResource(filePath.toUri());
                        if (resource.exists()) {
                            resources.add(resource);
                        }
                    }
                }
            } else {
                throw new FileNotFoundException("Not a directory: " + directoryPath);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load files from storage location", ex);
        }
        return resources;
    }

    public ByteArrayOutputStream generateQR(List<Resource> resources) throws IOException {
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();

        // Crea un oggetto ZipOutputStream per scrivere i file ZIP
        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            for (Resource resource : resources) {
                try {
                    // Estrai il testo dal PDF
                    PDDocument document = PDDocument.load(resource.getInputStream());
                    PDFTextStripper pdfStripper = new PDFTextStripper();
                    String text = pdfStripper.getText(document);
                    document.close();

                    // Genera il codice QR
                    QRCodeWriter qrCodeWriter = new QRCodeWriter();
                    BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 2000, 2000);

                    // Salva il codice QR come immagine PNG
                    String qrCodeFileName = "codice_qr_" + UUID.randomUUID().toString() + ".png";
                    File qrCodeFile = new File(qrCodeFileName);
                    MatrixToImageWriter.writeToPath(bitMatrix, "PNG", qrCodeFile.toPath());

                    // Aggiunge il file al file ZIP
                    zipOut.putNextEntry(new ZipEntry(qrCodeFileName));
                    Files.copy(qrCodeFile.toPath(), zipOut);
                    zipOut.closeEntry();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (WriterException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Flush e chiude lo stream ZIP
        zipOutputStream.flush();
        zipOutputStream.close();

        return zipOutputStream;
    }

    public File createFileFromMultipart(MultipartFile multipartFile) {
        try {
            File file = new File(fileStorageLocation+"\\" + multipartFile.getOriginalFilename());
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(multipartFile.getBytes());
            fileOutputStream.flush();
            fileOutputStream.close();
            log.info("upload PDF fatto: " + file.getAbsolutePath());
            return file;
        } catch (IOException e) {
            log.error("Errore in upload PDF!");
        }
        return null;
    }
}
