package com.company.bl.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "bl.file-storage.reports")
public class ReportStorageProperties {

    private boolean ofdEnabled = true;

    private int ofdWorkerMaxHeapMb = 256;

    private Duration ofdWorkerTimeout = Duration.ofSeconds(60);

    private Path rootDir = Path.of(System.getProperty("java.io.tmpdir"), "sybase", "bl-center", "reports");

    private DataSize maxImageSize = DataSize.ofMegabytes(20);

    private Duration temporaryFileTtl = Duration.ofHours(24);

    private int cleanupBatchSize = 200;
}
