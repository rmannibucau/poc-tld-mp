package org.talend.sdk.component.marketplace.service.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;

@MonoMeecrowaveConfig
class FilesystemStorageTest {

    @Inject
    private CarStorage defaultStorage;

    @Inject
    @ConfigProperty(name = "talend.marketplace.storage.car.filesystem.location", defaultValue = "${catalina.base}/storage/car/")
    private String location;

    @Test
    void run() {
        final Download entity = new Download() {

            {
                setId("downloadId");
                setComponent(new Component() {

                    {
                        setId("compId");
                    }
                });
            }
        };
        final File downloadFile = new File(
                location.replace("${catalina.base}", System.getProperty("catalina.base", ".")),
                "compId/downloadId.car");

        defaultStorage.save(new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8)), entity);
        assertTrue(downloadFile.isFile());
        defaultStorage.delete(entity);
        assertFalse(downloadFile.exists());
    }
}
