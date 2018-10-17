package org.talend.sdk.component.marketplace.service.storage;

import java.io.InputStream;

import org.talend.sdk.component.marketplace.model.Download;

public interface CarStorage {
    /**
     * Persist the stream materializing the download.
     *
     * @param stream the stream to persist.
     * @param download the related download (useful to map its identifier).
     */
    void save(InputStream stream, Download download);

    /**
     * Find the stream corresponding to a saved download.
     *
     * @param download the download to retrieve.
     * @return the stream for the download passed as parameter.
     */
    InputStream retrieve(final Download download);

    /**
     * Deletes the file materializing the entity.
     *
     * @param entity the download model.
     */
    void delete(Download entity);
}
