package com.it.qr_download.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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

    public List<File> loadFiles() {
        List<File> files = new ArrayList<>();
        try {
            File directory = this.fileStorageLocation.toFile();
            if (directory.isDirectory()) {
                File[] fileList = directory.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        if (file.isFile()) {
                            files.add(file);
                        }
                    }
                }
            } else {
                throw new IllegalArgumentException("Not a directory: " + directory.getAbsolutePath());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load files from storage location", ex);
        }
        return files;
    }

    public ByteArrayOutputStream generateQR(List<File> files) throws IOException {
        ByteArrayOutputStream zipOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOut = new ZipOutputStream(zipOutputStream)) {
            for (File file : files) {
                try {
                    // Genera il codice QR per il file corrente
                    MultiFormatWriter writer = new MultiFormatWriter();
                    BitMatrix bitMatrix = writer.encode("file:///" + file.getAbsolutePath(), BarcodeFormat.QR_CODE, 800, 800);
                    BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

                    // Crea un nome univoco per il file PNG
                    String fileName = file.getName()+"_"+UUID.randomUUID().toString() + ".png";

                    // Aggiunge il file PNG al file ZIP
                    zipOut.putNextEntry(new ZipEntry(fileName));
                    ImageIO.write(image, "PNG", zipOut);
                    zipOut.closeEntry();
                } catch (IOException | WriterException e) {
                    log.error(e.getLocalizedMessage());
                }
            }
        }

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
