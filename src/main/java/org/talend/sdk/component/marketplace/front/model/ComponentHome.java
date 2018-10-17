package org.talend.sdk.component.marketplace.front.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ComponentHome {
    private Page<ComponentModel> page;
    private Suggestions suggestions;
}
