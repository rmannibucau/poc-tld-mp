package org.talend.sdk.component.marketplace.api;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.service.dao.AuditedEntityDao;
import org.talend.sdk.component.marketplace.service.jaxrs.DownloadOutput;
import org.talend.sdk.component.marketplace.service.lang.FilenameNormalizer;
import org.talend.sdk.component.marketplace.service.security.User;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// note: theorically this can be extracted in another app reading the same database (or a synchro)
@ApplicationScoped
@Path("v1/component")
@Tag(name = "Component", description = "Endpoints enabling access to registered components.")
public class ComponentApiResource {
    @Inject
    private AuditedEntityDao dao;

    @Inject
    protected User user;

    @Inject
    private CarStorage storage;

    @Inject
    private FilenameNormalizer filenameNormalizer;

    @GET
    @Path("index")
    @Produces(APPLICATION_JSON)
    @Operation(description = "Returns the list of available components.")
    @APIResponse(responseCode = "200", description = "The list of components",
            content = @Content(mediaType = APPLICATION_JSON))
    public Components getComponents(@QueryParam("from") @DefaultValue("0") final int from,
                                    @QueryParam("max") @DefaultValue("20") final int max) {
        ensureAllowed();
        // todo: filter depending some context (query params?) which provides an info on how to filter
        return new Components(
                dao.findAll(Component.class, from, Math.min(Math.max(max, 0), 5000)).stream()
                    .map(it -> new ComponentRef(it.getId(), it.getName(), it.getDescription(), it.getLicense()))
                    .collect(toList()),
                dao.count(Component.class));
    }

    @GET
    @Path("downloads/{componentId}")
    @Produces(APPLICATION_JSON)
    @Operation(description = "Returns the list of available downloads for a component.")
    @APIResponse(responseCode = "200", description = "The list of downloads for the requested component.",
            content = @Content(mediaType = APPLICATION_JSON))
    public Downloads getDownloads(@PathParam("componentId") final String id) {
        ensureAllowed();
        return new Downloads(
                ofNullable(dao.findById(Component.class, id).getDownloads()).orElseGet(Collections::emptyList).stream()
                    .map(it -> new DownloadRef(it.getId(), it.getName(), it.getComponentVersion()))
                    .collect(toList()));
    }

    @GET
    @Path("download/{downloadId}")
    @Produces(APPLICATION_OCTET_STREAM)
    @Operation(description = "Returns the list of available components.")
    @APIResponse(responseCode = "200", description = "The list of components",
            content = @Content(mediaType = APPLICATION_OCTET_STREAM))
    public Response download(@PathParam("downloadId") final String id) {
        ensureAllowed();
        final Download download = dao.findById(Download.class, id);
        if (download == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Response.ok(new DownloadOutput(storage, download))
                       .header("Content-Disposition",
                               "attachment; filename=\"" + filenameNormalizer.normalize(download.getName()) + "\"")
                       .build();
    }

    private void ensureAllowed() {
        if (user.getGroups() == null || Stream.of("*", "**").noneMatch(allowed -> user.getGroups().contains(allowed))) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Components {
        private Collection<ComponentRef> items;
        private long total;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentRef {
        private String id;
        private String name;
        private String description;
        private String license;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Downloads {
        private Collection<DownloadRef> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DownloadRef {
        private String id;
        private String name;
        private String version;
    }
}
