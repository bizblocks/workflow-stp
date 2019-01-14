package com.groupstp.workflowstp.web.screenpalette.column;

import com.groupstp.workflowstp.entity.ScreenTableColumnTemplate;
import com.groupstp.workflowstp.service.ExtEntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.export.ByteArrayDataProvider;
import com.haulmont.cuba.gui.export.ExportDisplay;
import com.haulmont.cuba.gui.export.ExportFormat;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class ScreenTableColumnTemplateBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(ScreenTableColumnTemplateBrowse.class);

    @Inject
    private FileUploadingAPI uploadingAPI;
    @Inject
    private ExtEntityImportExportService entityImportExportService;
    @Inject
    private Security security;
    @Inject
    private ExportDisplay exportDisplay;
    @Inject
    private ViewRepository viewRepository;

    @Inject
    private Table<ScreenTableColumnTemplate> screenTableColumnTemplatesTable;
    @Inject
    private FileUploadField importBtn;
    @Inject
    private PopupButton exportBtn;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();
    }

    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("screenTableColumnTemplateBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload screen table column templates", e.getCause());
        });
        importBtn.addFileUploadSucceedListener(e -> {
            final UUID fileId = importBtn.getFileId();
            try {
                File file = uploadingAPI.getFile(fileId);
                if (file != null) {
                    byte[] data = Files.readAllBytes(file.toPath());

                    long count;
                    String ext = FilenameUtils.getExtension(importBtn.getFileName());
                    if ("json".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromJSON(
                                new String(data, StandardCharsets.UTF_8), getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof ScreenTableColumnTemplate)
                                .count();
                    } else if ("zip".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromZIP(
                                data, getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof ScreenTableColumnTemplate)
                                .count();
                    } else {
                        throw new RuntimeException(getMessage("screenTableColumnTemplateBrowse.unknownFileFormat"));
                    }

                    showNotification(String.format(getMessage("screenTableColumnTemplateBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    screenTableColumnTemplatesTable.getDatasource().refresh();
                } else {
                    showNotification(getMessage("screenTableColumnTemplateBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("screenTableColumnTemplateBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Screen table column templates import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(ScreenTableColumnTemplate.class, EntityOp.CREATE));
    }

    private void initExport() {
        exportBtn.addAction(new ItemTrackingAction("zipExport") {
            @Override
            public String getCaption() {
                return getMessage("screenTableColumnTemplateBrowse.zipExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<ScreenTableColumnTemplate> items = screenTableColumnTemplatesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("screenTableColumnTemplateBrowse.exportingFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("screenTableColumnTemplateBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Screen table column templates export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(screenTableColumnTemplatesTable.getSelected()) &&
                        security.isEntityOpPermitted(ScreenTableColumnTemplate.class, EntityOp.READ);
            }
        });
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(ScreenTableColumnTemplate.class)
                .addLocalProperties();
    }

    private View getExportingView() {
        View view = viewRepository.getView(ScreenTableColumnTemplate.class, View.LOCAL);
        if (view == null) {
            throw new DevelopmentException("View '_local' for wfstp$ScreenTableColumnTemplate not found");
        }
        return view;
    }
}