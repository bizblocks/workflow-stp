package com.groupstp.workflowstp.web.screenpalette.action;

import com.groupstp.workflowstp.entity.ScreenActionTemplate;
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
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
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
public class ScreenActionTemplateBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(ScreenActionTemplateBrowse.class);

    @Inject
    private ComponentsFactory componentsFactory;
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
    private Table<ScreenActionTemplate> screenActionTemplatesTable;
    @Inject
    private FileUploadField importBtn;
    @Inject
    private PopupButton exportBtn;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initTable();
        initImport();
        initExport();
    }

    private void initTable() {
        screenActionTemplatesTable.addGeneratedColumn("icon", entity -> {
            if (StringUtils.isEmpty(entity.getIcon())) {
                return new Table.PlainTextCell(StringUtils.EMPTY);
            }
            Label label = componentsFactory.createComponent(Label.class);
            label.setIcon(entity.getIcon());
            label.setValue(entity.getIcon().split(":")[1]);
            return label;
        });
    }

    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("screenActionTemplateBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload screen action templates", e.getCause());
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
                                .filter(entity -> entity instanceof ScreenActionTemplate)
                                .count();
                    } else if ("zip".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromZIP(
                                data, getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof ScreenActionTemplate)
                                .count();
                    } else {
                        throw new RuntimeException(getMessage("screenActionTemplateBrowse.unknownFileFormat"));
                    }

                    showNotification(String.format(getMessage("screenActionTemplateBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    screenActionTemplatesTable.getDatasource().refresh();
                } else {
                    showNotification(getMessage("screenActionTemplateBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("screenActionTemplateBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Screen action templates import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(ScreenActionTemplate.class, EntityOp.CREATE));
    }

    private void initExport() {
        exportBtn.addAction(new ItemTrackingAction("zipExport") {
            @Override
            public String getCaption() {
                return getMessage("screenActionTemplateBrowse.zipExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<ScreenActionTemplate> items = screenActionTemplatesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("screenActionTemplateBrowse.exportingFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("screenActionTemplateBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Screen action templates export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(screenActionTemplatesTable.getSelected()) &&
                        security.isEntityOpPermitted(ScreenActionTemplate.class, EntityOp.READ);
            }
        });
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(ScreenActionTemplate.class)
                .addLocalProperties();
    }

    private View getExportingView() {
        View view = viewRepository.getView(ScreenActionTemplate.class, View.LOCAL);
        if (view == null) {
            throw new DevelopmentException("View '_local' for wfstp$ScreenActionTemplate not found");
        }
        return view;
    }
}