package org.talend.sdk.component.marketplace.service.storage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.marketplace.model.Download;

/**
 * Hierarchic local file storage.
 */
@ApplicationScoped
public class FilesystemStorage implements CarStorage {
    @Inject
    @ConfigProperty(name = "talend.marketplace.storage.car.filesystem.location", defaultValue = "${catalina.base}/storage/car/")
    private String location;

    private File root;

    @PostConstruct
    private void init() {
        root = new File(location.replace("${catalina.base}", System.getProperty("catalina.base", ".")));
        if (!root.exists() && !root.mkdirs()) {
            throw new IllegalStateException("Can't create '" + root + "'");
        }
    }

    @Override
    public InputStream retrieve(final Download download) {
        final File file = toLocation(download);
        if (!file.exists()) {
            throw new IllegalArgumentException("No download: " + download.getId());
        }
        try {
            return new BufferedInputStream(new FileInputStream(file));
        } catch (final FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(final InputStream stream, final Download download) {
        final File output = toLocation(download);
        if (!output.getParentFile().exists() && !output.getParentFile().mkdirs()) {
            throw new IllegalStateException("Can't create car parent folder: '" + root + "'");
        }
        try {
            Files.copy(stream, output.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void delete(final Download entity) {
        final File output = toLocation(entity);
        if (!output.exists()) {
            return;
        }
        if (!output.delete()) {
            throw new IllegalStateException("Can't delete: '" + output + "'");
        }
    }

    private File toLocation(final Download download) {
        return new File(root, download.getComponent().getId() + "/" + download.getId() + ".car");
    }
}
