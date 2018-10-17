package org.talend.sdk.component.marketplace.service.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class DownloadOutput implements StreamingOutput {
    private final CarStorage storage;
    private final Download download;

    @Override
    public void write(final OutputStream output) throws WebApplicationException {
        final byte[] buffer = new byte[8192];
        int read;
        try (final InputStream stream = storage.retrieve(download)) {
            while ((read = stream.read(buffer)) >= 0) {
                if (read > 0) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
