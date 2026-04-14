package kz.arta.synergy.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {

    @Value("${integration.storage.directory:./opt/app/xml_inbox}")
    private String defaultStorageDirectory;

    @Value("${integration.storage.directory.cert_eaeu:./opt/app/xml_inbox}")
    private String certStorageDirectory;

    @Value("${integration.storage.directory.decl_eaeu:./opt/app/xml_inbox}")
    private String declStorageDirectory;

    @Value("${integration.storage.directory.eaue_620:./opt/app/xml_inbox}")
    private String eaue620StorageDirectory;


    public String saveXmlData(String messageId, String xmlData, String folder) throws IOException {

        Path directory;
        String directoryPath;

        // Check if folder is a predefined keyword or a custom path
        if ("CertEaeu".equalsIgnoreCase(folder)) {
            directory = Paths.get(certStorageDirectory);
            directoryPath = certStorageDirectory;
        } else if ("DeclEaeu".equalsIgnoreCase(folder)) {
            directory = Paths.get(declStorageDirectory);
            directoryPath = declStorageDirectory;
        } else if ("Eaue620".equalsIgnoreCase(folder)) {
            directory = Paths.get(eaue620StorageDirectory);
            directoryPath = eaue620StorageDirectory;
        } else if ("default".equalsIgnoreCase(folder)) {
            directory = Paths.get(defaultStorageDirectory);
            directoryPath = defaultStorageDirectory;
        } else {
            String normalizedFolder = folder.startsWith("./parent") || folder.startsWith(".\\")
                ? folder
                : "./parent/" + folder;
            directory = Paths.get(normalizedFolder);
            directoryPath = normalizedFolder;
        }

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        String fileName = String.format("%s.xml", messageId);
        Path filePath = directory.resolve(fileName);

        Files.write(filePath, xmlData.getBytes("UTF-8"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);

        return String.format("Файл xml сохранен как %s, в папке %s", fileName, directoryPath.substring(2));
    }

    /**
     * Преобразует timestamp в формат для имени файла
     * Например: "2025-06-18T15:00:00Z" -> "20250618T150000Z"
     */
    private String formatTimestampForFileName(String timestamp) {
        try {
            return timestamp.replaceAll("[-:]", "");
        } catch (Exception e) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
        }
    }

    public long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
}
