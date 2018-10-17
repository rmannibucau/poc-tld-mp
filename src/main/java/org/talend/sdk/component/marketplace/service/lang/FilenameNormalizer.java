package org.talend.sdk.component.marketplace.service.lang;

import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FilenameNormalizer {
    private final Pattern filenamePattern = Pattern.compile("[^a-zA-Z0-9.\\-]");

    public String normalize(final String name) {
        final String filename = filenamePattern.matcher(name)
                                               .replaceAll("_")
                                               .replace("\"", "\\\"");
        if (!filename.endsWith(".car")) {
            return filename + ".car";
        }
        return filename;
    }
}
