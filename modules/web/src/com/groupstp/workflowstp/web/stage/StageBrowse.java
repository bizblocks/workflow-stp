package com.groupstp.workflowstp.web.stage;

import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.service.ExtEntityImportExportService;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.app.importexport.CollectionImportPolicy;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.AbstractLookup;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.GroupTable;
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
public class StageBrowse extends AbstractLookup {
    private static final Logger log = LoggerFactory.getLogger(StageBrowse.class);

    public static final String ENTITY_NAME = "entityName";
    public static final String LOOKUP = "lookup";

    @Inject
    private FileUploadingAPI uploadingAPI;
    @Inject
    private ExtEntityImportExportService entityImportExportService;
    @Inject
    private ViewRepository viewRepository;
    @Inject
    private ExportDisplay exportDisplay;
    @Inject
    private Security security;

    @Inject
    private CollectionDatasource<Stage, UUID> stagesDs;
    @Inject
    private GroupTable<Stage> stagesTable;
    @Inject
    private FileUploadField importBtn;


    @WindowParam(name = LOOKUP)
    private Boolean lookup;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        stagesDs.refresh(ParamsMap.of("entityName", params.get(ENTITY_NAME)));

        initImport();
        initExport();
    }

    //workflow import behaviour
    private void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("stageEdit.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload stage", e.getCause());
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
                            .filter(entity -> entity instanceof Stage)
                            .count();
                    showNotification(String.format(getMessage("stageEdit.importedSuccess"), count), NotificationType.HUMANIZED);
                    stagesDs.refresh();

                } else {
                    showNotification(getMessage("stageEdit.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("stageEdit.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Stage import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);//anyway delete temp file
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(stagesDs.getMetaClass(), EntityOp.CREATE));
    }

    //workflow export behaviour
    private void initExport() {
        stagesTable.addAction(new BaseAction("export") {
            @Override
            public String getCaption() {
                return getMessage("stageEdit.export");
            }

            @Override
            public void actionPerform(Component component) {
                Collection<Stage> items = stagesTable.getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("stageEdit.exportFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("stageEdit.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Stage export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && !CollectionUtils.isEmpty(stagesTable.getSelected());
            }
        });
    }

    @Override
    public void ready() {
        super.ready();

        if (Boolean.TRUE.equals(lookup)) {//lookup mode - remove all actions and make table multiselectable
            stagesTable.removeAllActions();
            stagesTable.getButtonsPanel().removeAll();
            stagesTable.setMultiSelect(true);
        }

        stagesTable.expandAll();
    }

    private EntityImportView getImportingView() {
        return new EntityImportView(Stage.class)
                .addLocalProperties()
                .addManyToManyProperty("actors", ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.KEEP_ABSENT_ITEMS)
                .addManyToManyProperty("actorsRoles", ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.KEEP_ABSENT_ITEMS)
                .addManyToManyProperty("viewers", ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.KEEP_ABSENT_ITEMS)
                .addManyToManyProperty("viewersRoles", ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.KEEP_ABSENT_ITEMS);
    }

    private View getExportingView() {
        View view = viewRepository.getView(Stage.class, "stage-export");
        if (view == null) {
            throw new DevelopmentException("View 'stage-export' for wfstp$Stage not found");
        }
        return view;
    }
}