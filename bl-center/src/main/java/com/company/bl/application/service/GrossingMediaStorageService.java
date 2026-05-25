package com.company.bl.application.service;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.infrastructure.config.GrossingMediaStorageProperties;
import com.company.bl.interfaces.vo.GrossingMediaAssetResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class GrossingMediaStorageService {

    private static final DateTimeFormatter DATE_DIRECTORY_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern DATE_DIRECTORY_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern STORED_FILE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]+");
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
        "image/bmp", ".bmp",
        "image/jpeg", ".jpg",
        "image/png", ".png",
        "image/webp", ".webp"
    );

    private final GrossingMediaStorageProperties properties;
    private final Path rootDirectory;

    public GrossingMediaStorageService(GrossingMediaStorageProperties properties) {
        this.properties = properties;
        this.rootDirectory = properties.getRootDir().toAbsolutePath().normalize();
    }

    public GrossingMediaAssetResponse store(MultipartFile file) {
        validateFile(file);

        String contentType = normalizeContentType(file.getContentType());
        String dateDirectory = DATE_DIRECTORY_FORMATTER.format(LocalDate.now());
        String storedFileName = UUID.randomUUID() + CONTENT_TYPE_EXTENSIONS.get(contentType);
        Path targetDirectory = rootDirectory.resolve(dateDirectory).normalize();
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();
        ensureWithinRoot(targetFile);

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetFile);
        } catch (IOException exception) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Grossing image upload failed");
        }

        return new GrossingMediaAssetResponse(
            publicUrl(dateDirectory, storedFileName),
            sanitizeOriginalFileName(file.getOriginalFilename()),
            contentType,
            file.getSize());
    }

    public StoredGrossingMedia load(String dateDirectory, String storedFileName) {
        if (!DATE_DIRECTORY_PATTERN.matcher(dateDirectory).matches()
            || !STORED_FILE_NAME_PATTERN.matcher(storedFileName).matches()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid grossing image path");
        }

        Path file = rootDirectory.resolve(dateDirectory).resolve(storedFileName).normalize();
        ensureWithinRoot(file);
        if (!Files.isRegularFile(file)) {
            throw new BlBusinessException(BlErrorCode.RESOURCE_NOT_FOUND, 404, "Grossing image not found");
        }

        return new StoredGrossingMedia(new FileSystemResource(file), contentTypeOf(storedFileName));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Grossing image file is required");
        }
        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Grossing image file exceeds size limit");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!properties.getAllowedContentTypes().contains(contentType)
            || !CONTENT_TYPE_EXTENSIONS.containsKey(contentType)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Only image files can be uploaded");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
    }

    private String sanitizeOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "grossing-image";
        }
        String normalized = originalFileName.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        return fileName.isBlank() ? "grossing-image" : fileName;
    }

    private String publicUrl(String dateDirectory, String storedFileName) {
        String prefix = properties.getPublicUrlPrefix();
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + dateDirectory + "/" + storedFileName;
    }

    private MediaType contentTypeOf(String storedFileName) {
        String lowerName = storedFileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lowerName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lowerName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lowerName.endsWith(".bmp")) {
            return MediaType.parseMediaType("image/bmp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private void ensureWithinRoot(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Invalid grossing image path");
        }
    }

    public record StoredGrossingMedia(Resource resource, MediaType contentType) {
    }
}
