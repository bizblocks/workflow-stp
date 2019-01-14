package com.groupstp.workflowstp.web.workflowdefinition;

import com.groupstp.workflowstp.entity.WorkflowDefinition;
import com.groupstp.workflowstp.service.ExtEntityImportExportService;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
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
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class WorkflowDefinitionBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(WorkflowDefinitionBrowse.class);

    @Inject
    private Security security;
    @Inject
    private FileUploadingAPI uploadingAPI;
    @Inject
    private ExtEntityImportExportService entityImportExportService;
    @Inject
    private ViewRepository viewRepository;
    @Inject
    private ExportDisplay exportDisplay;

    @Inject
    private GroupTable<WorkflowDefinition> workflowDefinitionsTable;
    @Inject
    private CollectionDatasource<WorkflowDefinition, UUID> workflowDefinitionsDs;
    @Inject
    private FileUploadField importBtn;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();
        initSorting();
    }

    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("workflowDefinitionBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload workflow definition", e.getCause());
        });
        importBtn.addFileUploadSucceedListener(e -> {
            final UUID fileId = importBtn.getFileId();
            try {
                File file = uploadingAPI.getFile(fileId);
                if (file != null) {

                    Collection<Entity> importedEntities;
                    byte[] data = java.nio.file.Files.readAllBytes(file.toPath());

                    if ("json".equalsIgnoreCase(FilenameUtils.getExtension(e.getFileName()))) {
                        String jsonContent = new String(data, StandardCharsets.UTF_8);
                        importedEntities = entityImportExportService.importEntitiesFromJSON(jsonContent, getImportingView());
                    } else {
                        importedEntities = entityImportExportService.importEntitiesFromZIP(data, getImportingView());
                    }
                    long count = importedEntities.stream()
                            .filter(entity -> entity instanceof WorkflowDefinition)
                            .count();
                    showNotification(String.format(getMessage("workflowDefinitionBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    workflowDefinitionsDs.refresh();

                } else {
                    showNotification(getMessage("workflowDefinitionBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("workflowDefinitionBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Workflow definition import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);//anyway delete temp file
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(workflowDefinitionsDs.getMetaClass(), EntityOp.CREATE));
    }

    //workflow export behaviour
    private void initExport() {
        workflowDefinitionsTable.addAction(new BaseAction("export") {
            @Override
            public String getCaption() {
                return getMessage("workflowDefinitionBrowse.export");
            }

            @Override
            public void actionPerform(Component component) {
                Collection<WorkflowDefinition> items = workflowDefinitionsTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("workflowDefinitionBrowse.exportFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("workflowDefinitionBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Workflow definition export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && !CollectionUtils.isEmpty(workflowDefinitionsTable.getSelected());
            }
        });
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(WorkflowDefinition.class)
                .addLocalProperties()
                .addManyToOneProperty("workflow", ReferenceImportBehaviour.ERROR_ON_MISSING);
    }

    private View getExportingView() {
        View view = viewRepository.getView(WorkflowDefinition.class, "workflowDefinition-export");
        if (view == null) {
            throw new DevelopmentException("View 'workflowDefinition-export' for wfstp$WorkflowDefinition not found");
        }
        return view;
    }

    private void initSorting() {
        workflowDefinitionsTable.sort("priority", Table.SortDirection.DESCENDING);
    }

    @Override
    public void ready() {
        super.ready();

        workflowDefinitionsTable.expandAll();
    }
}