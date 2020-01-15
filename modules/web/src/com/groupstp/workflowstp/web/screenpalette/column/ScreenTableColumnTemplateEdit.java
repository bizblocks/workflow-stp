package com.groupstp.workflowstp.web.screenpalette.column;

import com.groupstp.workflowstp.entity.ScreenTableColumnTemplate;
import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.util.codedialog.CodeDialog;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.DsBuilder;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.*;

/**
 * @author adiatullin
 */
public class ScreenTableColumnTemplateEdit extends AbstractEditor<ScreenTableColumnTemplate> {

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private WorkflowWebBean workflowWebBean;
    @Inject
    private MessageTools messageTools;
    @Inject
    private ComponentsFactory componentsFactory;
    @Inject
    private Scripting scripting;

    @Inject
    private LookupField<String> entityNameField;
    @Inject
    private LookupField<MetaProperty> entityPropertiesField;
    @Inject
    private BoxLayout captionBox;
    @Inject
    private TextField<String> captionField;
    @Named("fieldGroup.columnId")
    private TextField<String> columnIdField;
    @Inject
    private SourceCodeEditor generatorScript;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntityNameField();
        initColumnGenerator();
    }

    private void initEntityNameField() {
        Map<String, String> options = new TreeMap<>();
        for (MetaClass metaClass : workflowWebBean.getWorkflowEntities()) {
            String originalName = metaClass.getName();
            options.put(messageTools.getEntityCaption(metaClass) + " (" + originalName + ")", originalName);
        }
        entityNameField.setOptionsMap(options);
        entityNameField.addValueChangeListener(e -> {
            MetaClass metaClass = null;

            String value = entityNameField.getValue();
            if (!StringUtils.isEmpty(value)) {
                metaClass = metadata.getClassNN(value);
            }

            if (metaClass == null) {
                entityPropertiesField.setOptionsMap(Collections.emptyMap());
                entityPropertiesField.setVisible(false);
                captionBox.expand(captionField);
            } else {
                Map<String, MetaProperty> options1 = new TreeMap<>();
                for (MetaProperty property : metaClass.getProperties()) {
                    options1.put(messageTools.getPropertyCaption(property), property);
                }
                entityPropertiesField.setOptionsMap(options1);
                entityPropertiesField.setVisible(true);
                captionBox.expand(entityPropertiesField);
            }
        });
        entityPropertiesField.addValueChangeListener(e -> {
            MetaProperty selected = (MetaProperty) e.getValue();
            if (selected != null) {
                captionField.setValue(messageTools.getPropertyCaption(selected));
                columnIdField.setValue(selected.getName());

                entityPropertiesField.setValue(null);
            }
        });
        entityPropertiesField.setVisible(false);
        captionBox.expand(captionField);
    }

    private void initColumnGenerator() {
        generatorScript.setContextHelpIconClickHandler(e ->
                showMessageDialog(getMessage("screenTableColumnTemplateEdit.generator.title"),
                        getMessage("screenTableColumnTemplateEdit.generator.content"), MessageType.CONFIRMATION_HTML));
    }

    public void editGeneratorScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getGeneratorScript());
        dialog.addCloseWithCommitListener(() -> generatorScript.setValue(dialog.getCode()));
    }

    public void testGeneratorScript() {
        String script = getItem().getGeneratorScript();
        if (!StringUtils.isEmpty(script)) {
            Map<String, Object> params = new HashMap<>();
            params.put(WorkflowWebBean.STAGE, metadata.create(Stage.class));
            params.put(WorkflowWebBean.SCREEN, this);
            params.put(WorkflowWebBean.VIEW_ONLY, Boolean.FALSE);

            Table table = componentsFactory.createComponent(Table.class);
            table.setDatasource(DsBuilder.create().setJavaClass(getItem().getEntityName() == null ?
                    User.class : metadata.getClassNN(getItem().getEntityName()).getJavaClass())
                    .buildCollectionDatasource());
            params.put(WorkflowWebBean.TARGET, table);

            try {
                scripting.evaluateGroovy(script, params);
            } catch (CompilationFailedException e) {
                showMessageDialog(messages.getMainMessage("action.script.error"),
                        String.format(messages.getMainMessage("action.script.compilationError"), e.toString()), MessageType.WARNING_HTML);
                return;
            } catch (Exception ignore) {
            }
            showNotification(messages.getMainMessage("action.script.success"));
        }
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            ScreenTableColumnTemplate item = getItem();
            if (!isUnique(item)) {
                showNotification(getMessage("screenTableColumnTemplateEdit.sameColumnAlreadyExist"), NotificationType.WARNING);
                columnIdField.requestFocus();
                return false;
            }

            return true;
        }
        return false;
    }

    private boolean isUnique(ScreenTableColumnTemplate item) {
        List same;
        if (StringUtils.isEmpty(item.getEntityName())) {
            same = dataManager.load(ScreenTableColumnTemplate.class)
                    .query("select e from wfstp$ScreenTableColumnTemplate e where " +
                            "(e.name = :name or e.columnId = :columnId) and e.entityName is null and e.id <> :id")
                    .parameter("name", item.getName())
                    .parameter("columnId", item.getColumnId())
                    .parameter("id", item.getId())
                    .view(View.MINIMAL)
                    .maxResults(1)
                    .list();
        } else {
            same = dataManager.load(ScreenTableColumnTemplate.class)
                    .query("select e from wfstp$ScreenTableColumnTemplate e where " +
                            "(e.name = :name or e.columnId = :columnId) and e.entityName = :entityName and e.id <> :id")
                    .parameter("name", item.getName())
                    .parameter("columnId", item.getColumnId())
                    .parameter("entityName", item.getEntityName())
                    .parameter("id", item.getId())
                    .view(View.MINIMAL)
                    .maxResults(1)
                    .list();
        }
        return CollectionUtils.isEmpty(same);
    }
}