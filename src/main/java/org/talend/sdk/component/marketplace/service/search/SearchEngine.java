package org.talend.sdk.component.marketplace.service.search;

import java.util.Collection;

import lombok.Data;

public interface SearchEngine {
    SearchResult findSuggestions(String queryString, int max);

    @Data
    class SearchResult {

        private final long totalHits;

        private final Collection<Item> items;
    }

    @Data
    class Item {

        private final String id;

        private final String name;

        private final double score;
    }
}
