package org.talend.sdk.component.marketplace.service.search;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.facet.FacetQuery;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.talend.sdk.component.marketplace.configuration.LuceneConfiguration;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.service.event.DeletedEvent;
import org.talend.sdk.component.marketplace.service.event.PersistedEvent;

import lombok.extern.slf4j.Slf4j;

// implemented with lucene for now but using elasticsearch would make sense to be able to scale
@Slf4j
@ApplicationScoped
public class LuceneSearchEngine implements SearchEngine {

    @Inject
    private LuceneConfiguration configuration;

    @Inject
    private Event<SyncIndex> syncIndexEvent;

    @Inject
    private EntityManager entityManager;

    private Directory indexDir;

    private Directory indexFacetsDir;

    private Analyzer analyzer;

    private FacetsConfig facetsConfig;

    private IndexWriter writer;

    private DirectoryReader reader;

    private DirectoryTaxonomyWriter taxonomyWriter;

    private volatile IndexSearcher searcher;

    private volatile boolean closed;

    private final Collection<CompletionStage<?>> pendings = new CopyOnWriteArrayList<>();
    private final String[] fields = new String[] { "name", "description" };
    private final Map<String, Float> fieldBoots = new HashMap<String, Float>() {{
        put("name", 100.f);
        put("description", 5.f);
    }};

    @PostConstruct
    void init() {
        final String directory = configuration.getDirectory();
        if ("[memory]".equals(directory)) {
            indexDir = new RAMDirectory();
            indexFacetsDir = new RAMDirectory();
            log.info(
                    "Using Lucene RAMDirectory, it is recommended to set talend.marketplace.lucene.directory property to use a disk index");
        } else {
            final File index = findDir(directory, "index");
            final File facet = findDir(directory, "facet");
            try {
                indexDir = FSDirectory.open(index.toPath());
                indexFacetsDir = FSDirectory.open(facet.toPath());
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        final Map<String, Analyzer> fieldAnalyzers = new HashMap<>();
        final Analyzer defaultAnalyzer = new StandardAnalyzer();
        fieldAnalyzers.put("id", defaultAnalyzer);
        fieldAnalyzers.put("name", defaultAnalyzer);
        fieldAnalyzers.put("description", defaultAnalyzer);
        analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzers);
        try {
            final IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(indexDir, writerConfig);
            taxonomyWriter = new DirectoryTaxonomyWriter(indexFacetsDir);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }

        facetsConfig = new FacetsConfig();

        closed = false;

        doIndexAll();
    }

    @Override
    public SearchResult findSuggestions(final String queryString, final int max) {
        final IndexSearcher indexSearcher = getOrCreateSearcher();
        if (indexSearcher == null) {
            return new SearchResult(0, Collections.emptyList());
        }
        try {
            final Query parsed = queryString.trim().isEmpty() ?
                    new MatchAllDocsQuery() :
                    new MultiFieldQueryParser(fields, analyzer, fieldBoots).parse(preProcessQuery(queryString));
            final TopDocs topDocs = indexSearcher.search(parsed, max);
            return new SearchResult(topDocs.totalHits, Stream.of(topDocs.scoreDocs).map(doc -> {
                try {
                    final Document document = searcher.doc(doc.doc);
                    return new Item(document.get("id"), document.get("name"), doc.score);
                } catch (IOException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(toList()));
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String preProcessQuery(final String queryString) {
        return queryString.trim().replace(" ", "* ") + '*';
    }

    CompletionStage<SyncIndex> doIndexAll() {
        final CompletableFuture<SyncIndex> syncStage = new CompletableFuture<>();
        pendings.add(syncStage);
        syncIndexEvent.fireAsync(new SyncIndex()).handle((r, e) -> {
            pendings.remove(syncStage);
            if (e == null) {
                syncStage.complete(r);
                return r;
            }
            syncStage.completeExceptionally(e);
            if (RuntimeException.class.isInstance(e)) {
                throw RuntimeException.class.cast(e);
            }
            throw new IllegalStateException(e);
        });
        return syncStage;
    }

    @PreDestroy
    void destroy() {
        closed = true;

        flushPendings();

        Stream.of(reader, writer, taxonomyWriter, indexDir, indexFacetsDir).filter(Objects::nonNull).forEach(c -> {
            try {
                c.close();
            } catch (final IOException e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    void flushPendings() {
        final IllegalStateException ise = new IllegalStateException("Some pending requests failed");
        pendings.forEach(p -> {
            try {
                p.toCompletableFuture().get(1, MINUTES);
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException | TimeoutException e) {
                ise.addSuppressed(e);
            }
        });
        if (ise.getSuppressed().length > 0) {
            throw ise;
        }
    }

    void updateIndex(@ObservesAsync final SyncIndex sync) {
        if (closed) {
            return;
        }

        ensureOpened();
        final Iterator<Component> components = entityManager.createNamedQuery("Component.findAllWithProducts", Component.class)
                                                            .getResultList().iterator();
        int remaining = configuration.getCommitInterval();
        while (remaining > 0 && components.hasNext()) {
            doIndex(components.next());
            remaining--;
        }
        if (remaining != configuration.getCommitInterval()) {
            commit();
        }
    }

    private void doIndex(final Consumer<Document> documentFiller) {
        ensureOpened();
        final Document doc = new Document();
        documentFiller.accept(doc);
        try {
            writer.addDocument(facetsConfig.build(taxonomyWriter, doc));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void commit() {
        try {
            writer.commit();
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    void onUpdateComponent(@Observes final PersistedEvent<Component> onComponent) {
        ensureOpened();
        doIndex(onComponent.getEntity());
        commit();
    }

    private void doIndex(final Component view) {
        doIndex(doc -> {
            doc.add(new StringField("id", view.getId(), Field.Store.YES));
            doc.add(new TextField("name", view.getName(), Field.Store.YES));
            doc.add(new TextField("description", view.getName(), Field.Store.NO));
            doc.add(new FacetField("product", "component"));
            if (view.getProducts() != null) {
                view.getProducts().forEach(product -> doc.add(new FacetField("product", product.getName())));
            }
        });
    }

    void onDeleteComponent(@Observes final DeletedEvent<Component> onComponent) {
        ensureOpened();
        try {
            writer.deleteDocuments(new TermQuery(new Term("id", onComponent.getEntity().getId())),
                    new FacetQuery("entity", "component"));
            commit();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private IndexSearcher getOrCreateSearcher() {
        if (closed) {
            return null;
        }
        if (searcher == null) { // create it if needed
            try {
                if (!DirectoryReader.indexExists(indexDir)) {
                    return null;
                }
            } catch (final IOException e) {
                return null;
            }

            synchronized (this) {
                if (searcher == null) {
                    try {
                        reader = DirectoryReader.open(indexDir);
                        searcher = new IndexSearcher(reader);
                    } catch (final IndexNotFoundException infe) {
                        return null;
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        if (closed) {
            return null;
        }
        try {
            if (!reader.isCurrent()) { // no more up to date, recreate
                synchronized (this) {
                    if (!reader.isCurrent()) {
                        final DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
                        if (newReader != null) {
                            reader.close();
                            reader = newReader;
                        }
                        searcher = new IndexSearcher(reader);
                    }
                }
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return searcher;
    }

    private void ensureOpened() {
        if (closed) {
            throw new IllegalStateException("Search Engine closed");
        }
    }

    private File findDir(final String directory, final String name) {
        File dir = new File(directory, name);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("catalina.base", "."), directory + "/" + name);
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalArgumentException("Can't find or create " + dir.getAbsolutePath());
        }
        return dir;
    }

    public static class SyncIndex {
        // just a marker
    }
}
