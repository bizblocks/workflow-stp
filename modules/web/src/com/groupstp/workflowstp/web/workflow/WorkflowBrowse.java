package com.groupstp.workflowstp.web.workflow;

import com.groupstp.workflowstp.entity.Step;
import com.groupstp.workflowstp.entity.StepDirection;
import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.service.ExtEntityImportExportService;
import com.haulmont.cuba.core.app.importexport.CollectionImportPolicy;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.export.ByteArrayDataProvider;
import com.haulmont.cuba.gui.export.ExportDisplay;
import com.haulmont.cuba.gui.export.ExportFormat;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class WorkflowBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(WorkflowBrowse.class);

    @Inject
    private Security security;
    @Inject
    private DataManager dataManager;
    @Inject
    private FileUploadingAPI uploadingAPI;
    @Inject
    private ExtEntityImportExportService entityImportExportService;
    @Inject
    private ViewRepository viewRepository;
    @Inject
    private ExportDisplay exportDisplay;

    @Inject
    private GroupTable<Workflow> workflowsTable;
    @Inject
    private CollectionDatasource<Workflow, UUID> workflowsDs;
    @Inject
    private FileUploadField importBtn;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        //action to mark workflow as active
        Action action = new BaseAction("activate") {
            @Override
            public void actionPerform(Component component) {
                Set<Workflow> items = workflowsTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        for (Workflow item : items) {
                            item.setActive(Boolean.TRUE.equals(item.getActive()) ? Boolean.FALSE : Boolean.TRUE);
                        }
                        dataManager.commit(new CommitContext(items));
                    } finally {
                        workflowsDs.refresh();
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<Workflow> items = workflowsTable.getSelected();
                    if (!CollectionUtils.isEmpty(items)) {
                        return security.isEntityOpPermitted(workflowsDs.getMetaClass(), EntityOp.UPDATE);
                    }
                }
                return false;
            }
        };
        action.setCaption(getMessage("workflowBrowse.activate"));
        workflowsTable.addAction(action);

        //multiselect listener
        workflowsDs.addItemChangeListener(e -> {
            Workflow item = workflowsTable.getSingleSelected();
            Set<Workflow> items = workflowsTable.getSelected();

            EditAction editAction = (EditAction) workflowsTable.getActionNN(EditAction.ACTION_ID);
            if (!CollectionUtils.isEmpty(items) && items.size() == 1 && item != null) {
                editAction.setEnabled(true);
                boolean viewOnly = Boolean.TRUE.equals(item.getActive());
                editAction.setCaption(messages.getMainMessage(viewOnly ? "actions.View" : "actions.Edit"));
            } else {
                editAction.setEnabled(false);//edit only able for one item
                editAction.setWindowParams(null);
                editAction.setCaption(messages.getMainMessage("actions.Edit"));
            }
            workflowsTable.getActionNN(RemoveAction.ACTION_ID).setEnabled(!CollectionUtils.isEmpty(items) && checkAllIsNotActive(items));
            action.setEnabled(!CollectionUtils.isEmpty(items) && checkAllHaveSameStatus(items));

            if (item == null) {
                action.setCaption(getMessage("workflowBrowse.activate"));
            } else {
                action.setCaption(getMessage(Boolean.TRUE.equals(item.getActive()) ?
                        getMessage("workflowBrowse.deActivate") : getMessage("workflowBrowse.activate")));
            }
        });

        initImport();
        initExport();
    }

    //workflow import behaviour
    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("workflowBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload workflow", e.getCause());
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
                            .filter(entity -> entity instanceof Workflow)
                            .count();
                    showNotification(String.format(getMessage("workflowBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    workflowsDs.refresh();

                } else {
                    showNotification(getMessage("workflowBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("workflowBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Workflow import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);//anyway delete temp file
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(workflowsDs.getMetaClass(), EntityOp.CREATE));
    }

    //workflow export behaviour
    private void initExport() {
        workflowsTable.addAction(new BaseAction("export") {
            @Override
            public String getCaption() {
                return getMessage("workflowBrowse.export");
            }

            @Override
            public void actionPerform(Component component) {
                Collection<Workflow> items = workflowsTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("workflowEdit.exportFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("workflowBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Workflow export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && !CollectionUtils.isEmpty(workflowsTable.getSelected());
            }
        });
    }

    //check what all workflows in not active state
    private boolean checkAllIsNotActive(Set<Workflow> items) {
        if (!CollectionUtils.isEmpty(items)) {
            for (Workflow item : items) {
                if (Boolean.TRUE.equals(item.getActive())) {
                    return false;
                }
            }
        }
        return true;
    }

    //check what all workflows have the same activation status
    private boolean checkAllHaveSameStatus(Set<Workflow> items) {
        if (!CollectionUtils.isEmpty(items)) {
            boolean first = Boolean.TRUE.equals(IterableUtils.get(items, 0).getActive());
            for (Workflow item : items) {
                boolean current = Boolean.TRUE.equals(item.getActive());
                if (current != first) {
                    return false;
                }
            }
        }
        return true;
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(Workflow.class)
                .addLocalProperties()
                .addOneToManyProperty("steps",
                        new EntityImportView(Step.class)
                                .addLocalProperties()
                                .addManyToOneProperty("stage", ReferenceImportBehaviour.ERROR_ON_MISSING)
                                .addOneToManyProperty("directions",
                                        new EntityImportView(StepDirection.class)
                                                .addLocalProperties()
                                                .addManyToOneProperty("from", ReferenceImportBehaviour.ERROR_ON_MISSING)
                                                .addManyToOneProperty("to", ReferenceImportBehaviour.ERROR_ON_MISSING), CollectionImportPolicy.REMOVE_ABSENT_ITEMS)
                                .addManyToOneProperty("workflow", ReferenceImportBehaviour.ERROR_ON_MISSING),
                        CollectionImportPolicy.REMOVE_ABSENT_ITEMS);
    }

    private View getExportingView() {
        View view = viewRepository.getView(Workflow.class, "workflow-export");
        if (view == null) {
            throw new DevelopmentException("View 'workflow-export' for wfstp$Workflow not found");
        }
        return view;
    }

    @Override
    public void ready() {
        super.ready();

        workflowsTable.expandAll();
    }
}