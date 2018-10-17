package org.talend.sdk.component.marketplace.service.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.test.Context;

@MonoMeecrowaveConfig
class SearchEngineTest {

    @Inject
    private Context context;

    @Inject
    private AuditedEntityDao dao;

    @Inject
    private LuceneSearchEngine engine;

    @Inject
    private Event<LuceneSearchEngine.SyncIndex> syncIndexEvent;

    @BeforeEach
    @AfterEach
    void reset() {
        engine.destroy();
        engine.init();
    }

    @Test
    void findSuggestions() throws ExecutionException, InterruptedException {
        engine.flushPendings();

        final Vendor vendor = new Vendor();
        vendor.setName("Talend");
        context.executeInTx(() -> {
            dao.save(vendor);
            IntStream.range(0, 5).mapToObj(i -> {
                final Component component = new Component();
                component.setName(i + " item " + (i % 2 == 0 ? "is good" : "is not bad"));
                component.setDescription("The " + i);
                component.setLicense("https://www.apache.org/licenses/LICENSE-2.0");
                component.setSources("https://github.com/Talend/component-runtime");
                component.setBugtracker("https://jira.talendforge.org/projects/TCOMP/issues");
                component.setChangelog("Things can change");
                component.setDocumentation("https://talend.github.io/component-runtime/");
                component.setVendor(vendor);
                return component;
            }).forEach(dao::save);
        });
        engine.doIndexAll().toCompletableFuture().get();

        {
            final SearchEngine.SearchResult suggestions = engine.findSuggestions("1 item", 3);
            assertEquals(5, suggestions.getTotalHits());
            assertEquals(3, suggestions.getItems().size());
            final Iterator<SearchEngine.Item> iterator = suggestions.getItems().iterator();
            final SearchEngine.Item item = iterator.next();
            assertEquals("1 item is not bad", item.getName());
            assertTrue(item.getScore() > iterator.next().getScore() * 1.5);
        }

        {
            final SearchEngine.SearchResult suggestions = engine.findSuggestions("goo", 5);
            assertEquals(3, suggestions.getTotalHits());
            assertEquals(3, suggestions.getItems().size());
        }

        {
            final SearchEngine.SearchResult suggestions = engine.findSuggestions("", 5);
            assertEquals(5, suggestions.getTotalHits(), () -> suggestions.getItems().toString());
            assertEquals(5, suggestions.getItems().size());
        }

        context.executeInTx(() -> dao.deleteById(Vendor.class, vendor.getId()));
        context.executeInTx(() -> dao.findAll(Component.class, 0, 10).forEach(it -> dao.deleteById(Component.class, it.getId())));
        // here we leak the index but it should be ok for current test suite
    }
}
