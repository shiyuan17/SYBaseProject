package com.company.bl.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "bl.file-storage.grossing-media")
public class GrossingMediaStorageProperties {

    private Path rootDir = Path.of(System.getProperty("java.io.tmpdir"), "sybase", "bl-center", "grossing-media");

    private String publicUrlPrefix = "/api/v1/grossing-media-assets/files";

    private DataSize maxFileSize = DataSize.ofMegabytes(20);

    private Set<String> allowedContentTypes = new LinkedHashSet<>(Set.of(
        "image/bmp",
        "image/jpeg",
        "image/png",
        "image/webp"
    ));
}
