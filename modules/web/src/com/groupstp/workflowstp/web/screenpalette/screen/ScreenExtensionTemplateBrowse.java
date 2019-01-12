package com.groupstp.workflowstp.web.screenpalette.screen;

import com.groupstp.workflowstp.entity.ScreenExtensionTemplate;
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
public class ScreenExtensionTemplateBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(ScreenExtensionTemplateBrowse.class);

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
    private FileUploadField importBtn;
    @Inject
    private PopupButton exportBtn;
    @Inject
    private Table<ScreenExtensionTemplate> screenExtensionTemplatesTable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();
    }

    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("screenExtensionTemplateBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload screen templates", e.getCause());
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
                                .filter(entity -> entity instanceof ScreenExtensionTemplate)
                                .count();
                    } else if ("zip".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromZIP(
                                data, getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof ScreenExtensionTemplate)
                                .count();
                    } else {
                        throw new RuntimeException(getMessage("screenExtensionTemplateBrowse.unknownFileFormat"));
                    }

                    showNotification(String.format(getMessage("screenExtensionTemplateBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    screenExtensionTemplatesTable.getDatasource().refresh();
                } else {
                    showNotification(getMessage("screenExtensionTemplateBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("screenExtensionTemplateBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Screen templates import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(ScreenExtensionTemplate.class, EntityOp.CREATE));
    }

    private void initExport() {
        exportBtn.addAction(new ItemTrackingAction("zipExport") {
            @Override
            public String getCaption() {
                return getMessage("screenExtensionTemplateBrowse.zipExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<ScreenExtensionTemplate> items = screenExtensionTemplatesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("screenExtensionTemplateBrowse.exportingFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("screenExtensionTemplateBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Screen templates export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(screenExtensionTemplatesTable.getSelected()) &&
                        security.isEntityOpPermitted(ScreenExtensionTemplate.class, EntityOp.READ);
            }
        });
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(ScreenExtensionTemplate.class)
                .addLocalProperties();
    }

    private View getExportingView() {
        View view = viewRepository.getView(ScreenExtensionTemplate.class, View.LOCAL);
        if (view == null) {
            throw new DevelopmentException("View '_local' for wfstp$ScreenExtensionTemplate not found");
        }
        return view;
    }
}