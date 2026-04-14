package kz.arta.synergy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class XmlDownloadService {

    @Value("${integration.storage.directory:./opt/app/xml_inbox}")
    private String defaultStorageDirectory;

    @Value("${integration.storage.directory.cert_eaeu:./opt/app/xml_inbox}")
    private String certStorageDirectory;

    @Value("${integration.storage.directory.decl_eaeu:./opt/app/xml_inbox}")
    private String declStorageDirectory;

    @Value("${integration.storage.directory.eaue_620:./opt/app/xml_inbox}")
    private String eaue620StorageDirectory;



    public void downloadLog(String filename, String folder, HttpServletResponse response) throws IOException {

        Path xmlFilesDir = getFilesDir(folder);

        Path filePath = xmlFilesDir.resolve(filename);

        String fullPath = filePath.toAbsolutePath().toString();


        if (!filePath.startsWith(xmlFilesDir) || !Files.exists(filePath) || Files.isDirectory(filePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.setContentType("text/plain; charset=UTF-8");
            response.getWriter().write("Файл не найден: " + fullPath + " " + filename);
            return;
        }

        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=" + filePath.getFileName());
        Files.copy(filePath, response.getOutputStream());
        response.flushBuffer();
    }



    public void downloadFolder(HttpServletResponse response, String folder) throws IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=zip-request.zip");
        Path xmlFilesDir = getFilesDir(folder);

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream());
             Stream<Path> paths = Files.walk(xmlFilesDir)) {

            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    ZipEntry zipEntry = new ZipEntry(xmlFilesDir.relativize(file).toString());
                    zos.putNextEntry(zipEntry);
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при добавлении файла в ZIP", e);
                }
            });
        }
    }

    private Path getFilesDir(String folder) {
        Path xmlFilesDir;
        if ("CertEaeu".equalsIgnoreCase(folder)) {
            xmlFilesDir = Paths.get(certStorageDirectory);
        } else if ("DeclEaeu".equalsIgnoreCase(folder)) {
            xmlFilesDir = Paths.get(declStorageDirectory);
        } else if ("Eaue620".equalsIgnoreCase(folder)) {
            xmlFilesDir = Paths.get(eaue620StorageDirectory);
        } else if (folder == null || folder.trim().isEmpty()) {
            xmlFilesDir = Paths.get(defaultStorageDirectory);
        } else {
            String normalizedFolder = folder.startsWith("./parent/") || folder.startsWith(".\\")
                ? folder
                : "./parent/" + folder;
            xmlFilesDir = Paths.get(normalizedFolder);
        }

        return xmlFilesDir;
    }
}
