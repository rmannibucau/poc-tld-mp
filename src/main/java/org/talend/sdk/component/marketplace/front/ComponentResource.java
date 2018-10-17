package org.talend.sdk.component.marketplace.front;

import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.marketplace.cdi.Tx;
import org.talend.sdk.component.marketplace.front.model.ComponentModel;
import org.talend.sdk.component.marketplace.front.model.DownloadModel;
import org.talend.sdk.component.marketplace.front.model.Suggestions;
import org.talend.sdk.component.marketplace.model.Component;
import org.talend.sdk.component.marketplace.model.Download;
import org.talend.sdk.component.marketplace.model.Vendor;
import org.talend.sdk.component.marketplace.service.jaxrs.DownloadOutput;
import org.talend.sdk.component.marketplace.service.lang.FilenameNormalizer;
import org.talend.sdk.component.marketplace.service.mapper.ApplicationMapper;
import org.talend.sdk.component.marketplace.service.search.SearchEngine;
import org.talend.sdk.component.marketplace.service.storage.CarStorage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("component")
public class ComponentResource extends BaseResource<Component, ComponentModel> {
    @Inject
    private ApplicationMapper mapper;

    @Inject
    private SearchEngine searchEngine;

    @Inject
    private EntityManager entityManager;

    @Inject
    private CarStorage storage;

    @Inject
    private FilenameNormalizer filenameNormalizer;

    @Override
    protected Component ensureAccess(final Component entity) {
        final Set<String> perms = user.getGroups();
        if (!perms.contains("**") && (entity.getVendor() == null || !perms.contains(entity.getVendor().getId()))) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return entity;
    }

    @Override
    protected ComponentModel map(final Component component, final boolean withRelationships) {
        final ComponentModel model = mapper.toModel(component);
        if (withRelationships) {
            model.setCompleteVendor(mapper.toModel(component.getVendor()));
            model.setProducts(component.getProducts() == null ?
                    emptyList() : component.getProducts().stream().map(mapper::toModel).collect(toList()));
            model.setDownloads(component.getDownloads() == null ?
                    emptyList() : component.getDownloads().stream().map(mapper::toModel).collect(toList()));
        }
        return model;
    }

    @Override
    protected void map(final ComponentModel model, final Component entity) {
        entity.setName(model.getName());
        entity.setDescription(model.getDescription());
        entity.setChangelog(model.getChangelog());
        entity.setSources(model.getSources());
        entity.setLicense(model.getLicense());
        entity.setBugtracker(model.getBugtracker());
        entity.setDocumentation(model.getDocumentation());
        entity.setVendor(requireNonNull(entityManager.find(Vendor.class, model.getVendor())));
    }

    @GET
    @Path("suggestions")
    public Suggestions getDatalistProposals(@QueryParam("max") @DefaultValue("15") final int max,
                                            @QueryParam("q") final String query) {
        final SearchEngine.SearchResult suggestions = searchEngine.findSuggestions(query, max);
        return new Suggestions(suggestions.getItems().stream()
            .map(it -> new Suggestions.Item(it.getName(), it.getId()))
            .collect(toList()), suggestions.getTotalHits());
    }

    @GET
    @Path("ui/download")
    public Ui uiDownload() {
        ensureLogged();
        final Ui form = uiService.createFormFor(Download.class);
        form.setProperties(new HashMap<>());
        return form;
    }

    @GET
    @Path("download/{downloadId}")
    @Produces(APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("downloadId") final String downloadId) {
        final Download download = entityManager.find(Download.class, downloadId);
        if (download == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        return Response.ok(new DownloadOutput(storage, download))
                       .header("Content-Disposition",
                               "attachment; filename=\"" + filenameNormalizer.normalize(download.getName()) + "\"")
                       .build();
    }

    @Tx
    @POST
    @Path("download/{componentId}")
    @Consumes(MULTIPART_FORM_DATA)
    public DownloadModel attachDownload(@PathParam("componentId") final String componentId,
                                        @QueryParam("downloadName") final String name,
                                        @QueryParam("componentVersion") final String version,
                                        @Context final HttpServletRequest request) {
        ensureLogged();
        final Component component = ensureAccess(unauthorizedIfNull(auditedEntityDao.findById(Component.class, componentId)));
        final Download download = new Download();
        download.initId(); // to ensure it is available to storage#save call
        download.setName(ofNullable(name)
                .orElseGet(() -> component.getName()
                                          .toLowerCase(ROOT)
                                          .replace(" ", "-") + "-" + version + ".car"));
        download.setComponent(component);
        download.setComponentVersion(version);
        try {
            final Part file = request.getPart("file");
            if (file == null) {
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            }
            try (final InputStream stream = file.getInputStream()) {
                storage.save(stream, download);
            }
            auditedEntityDao.save(download); // done after the upload which is more risky in practise
            entityManager.flush();
            return mapper.toModel(download);
        } catch (final IOException | ServletException e) {
            log.error(e.getMessage(), e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Tx
    @DELETE
    @Path("download/{componentId}/{downloadId}")
    public DownloadModel deleteDownload(@PathParam("componentId") final String componentId,
                                        @PathParam("downloadId") final String downloadId) {
        ensureLogged();
        final Download download = unauthorizedIfNull(auditedEntityDao.findById(Download.class, downloadId));
        ensureAccess(download.getComponent());
        storage.delete(download);
        entityManager.remove(download);
        entityManager.flush();
        return mapper.toModel(download);
    }

    private <T> T unauthorizedIfNull(final T test) {
        if (test == null) {
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        }
        return test;
    }
}
