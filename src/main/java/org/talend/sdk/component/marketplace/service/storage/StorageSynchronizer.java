package org.talend.sdk.component.marketplace.service.storage;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.service.event.DeletedEvent;

@ApplicationScoped
public class StorageSynchronizer {
    @Inject
    private CarStorage storage;

    void onDeleteComponent(@Observes final DeletedEvent<Component> componentDeletedEvent) {
        final Collection<Download> downloads = componentDeletedEvent.getEntity().getDownloads();
        if (downloads == null || downloads.isEmpty()) {
            return;
        }
        downloads.forEach(storage::delete);
    }

    void onDeleteDownload(@Observes final DeletedEvent<Download> downloadDeletedEvent) {
        storage.delete(downloadDeletedEvent.getEntity());
    }
}
