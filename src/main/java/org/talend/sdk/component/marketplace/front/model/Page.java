package org.talend.sdk.component.marketplace.front.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Page<T> {
    private Collection<T> items;
    private long total;
}
