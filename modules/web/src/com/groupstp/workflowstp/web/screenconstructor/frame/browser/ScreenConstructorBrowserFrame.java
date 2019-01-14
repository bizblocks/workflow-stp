package com.groupstp.workflowstp.web.screenconstructor.frame.browser;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;
import com.groupstp.workflowstp.web.util.action.ItemMoveAction;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class ScreenConstructorBrowserFrame extends AbstractScreenConstructorFrame {

    private static final List<String> TEMPLATE_PROPERTIES = Arrays.asList("caption", "columnId", "generatorScript", "editable");

    @Inject
    private MetadataTools metadataTools;
    @Inject
    private MessageTools messageTools;
    @Inject
    private DataManager dataManager;

    @Inject
    private Table<ScreenTableColumn> columnsTable;
    @Inject
    private CollectionDatasource<ScreenTableColumn, UUID> columnsDs;
    @Inject
    private DatasourceImplementation<ScreenTableColumn> columnDs;
    @Inject
    private CollectionDatasource<ScreenTableColumnTemplate, UUID> columnTemplatesDs;
    @Inject
    private BoxLayout columnEditBox;
    @Inject
    private FieldGroup columnFieldGroup;
    @Inject
    private TextField captionField;
    @Inject
    private LookupField namePropertiesLookup;
    @Inject
    private TextArea generatorScriptEditor;
    @Inject
    private FlowBoxLayout generatorScriptEditorBox;
    @Inject
    private LookupField templateField;

    private ScreenTableColumn editingItem;


    @Override
    public void prepare() {
        super.prepare();

        Collection<ScreenTableColumn> items = columnsDs.getItems();
        if (!CollectionUtils.isEmpty(items)) {
            for (ScreenTableColumn item : items) {
                cleanupIfSame(getTemplate(item), item);
            }
        }
    }


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initTemplates();
        initColumnsTable();
        initColumnsNamesLookup();
        initTableBehaviour();
        initColumnGeneratorEditor();
        initTemplateField();

        disableEdit();
        sortTable(columnsTable, "order");
    }

    private void initTemplates() {
        List<ScreenTableColumnTemplate> templates = dataManager.load(ScreenTableColumnTemplate.class)
                .query("select e from wfstp$ScreenTableColumnTemplate e where e.entityName is null or e.entityName = :entityName")
                .parameter("entityName", entityMetaClass.getName())
                .view(View.LOCAL)
                .list();
        if (!CollectionUtils.isEmpty(templates)) {
            for (ScreenTableColumnTemplate item : templates) {
                columnTemplatesDs.includeItem(item);
            }
        }
    }

    private void initColumnsTable() {
        columnsTable.addAction(new CreateAction(columnsTable) {
            @Override
            public void actionPerform(Component component) {
                ScreenTableColumn item = metadata.create(ScreenTableColumn.class);
                item.setOrder(columnsDs.size() + 1);

                columnDs.setItem(item);
                columnDs.refresh();
                columnDs.setModified(false);

                enableEdit();
            }
        });
        columnsTable.addAction(new EditAction(columnsTable) {
            @Override
            public void actionPerform(Component component) {
                Set<ScreenTableColumn> selected = columnsTable.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    editingItem = IterableUtils.get(selected, 0);
                    columnDs.setItem(metadataTools.copy(editingItem));//user can click cancel
                    columnDs.refresh();
                    columnDs.setModified(false);

                    enableEdit();
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<ScreenTableColumn> selected = columnsTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
        columnsTable.addAction(new ItemMoveAction(columnsTable, true));
        columnsTable.addAction(new ItemMoveAction(columnsTable, false));

        columnsTable.addGeneratedColumn("caption", entity -> {
            String value = entity.getCaption();
            if (StringUtils.isEmpty(value)) {
                ScreenTableColumnTemplate template = getTemplate(entity);
                if (template != null) {
                    value = template.getCaption();
                }
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        });
        columnsTable.addGeneratedColumn("columnId", entity -> {
            String value = entity.getColumnId();
            if (StringUtils.isEmpty(value)) {
                ScreenTableColumnTemplate template = getTemplate(entity);
                if (template != null) {
                    value = template.getColumnId();
                }
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        });
        columnsTable.addGeneratedColumn("editable", entity -> {
            Boolean value = entity.getEditable();
            if (value == null) {
                ScreenTableColumnTemplate template = getTemplate(entity);
                if (template != null) {
                    value = Boolean.TRUE.equals(template.getEditable());
                }
            }
            CheckBox checkBox = componentsFactory.createComponent(CheckBox.class);
            checkBox.setValue(value);
            checkBox.setEditable(false);
            return checkBox;
        });
        columnsTable.addGeneratedColumn("template", entity -> {
            String value = StringUtils.EMPTY;
            ScreenTableColumnTemplate template = getTemplate(entity);
            if (template != null) {
                value = template.getName();
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        });
    }

    private void initColumnsNamesLookup() {
        Map<String, MetaProperty> options = new TreeMap<>();
        for (MetaProperty property : entityMetaClass.getProperties()) {
            options.put(messageTools.getPropertyCaption(property), property);
        }
        namePropertiesLookup.setOptionsMap(options);
        namePropertiesLookup.addValueChangeListener(e -> {
            MetaProperty property = (MetaProperty) e.getValue();
            if (property != null) {
                columnDs.getItem().setCaption(messageTools.getPropertyCaption(property));
                columnDs.getItem().setColumnId(property.getName());

                namePropertiesLookup.setValue(null);
            }
        });
    }

    private void initTableBehaviour() {
        columnsDs.addItemChangeListener(e -> {
            cleanupIfSame(getTemplate(e.getPrevItem()), e.getPrevItem());

            ScreenTableColumn select = null;
            Set<ScreenTableColumn> selected = columnsTable.getSelected();
            if (!CollectionUtils.isEmpty(selected)) {
                if (selected.size() == 1) {
                    select = IterableUtils.get(selected, 0);
                }
            }
            if (select != null) {
                setupIfEmpty(getTemplate(select), select);
            }
            columnDs.setItem(select);
            columnDs.refresh();
            columnDs.setModified(false);
        });
        columnsDs.addCollectionChangeListener(e -> {
            correctOrderIfNeed(columnsTable, "order");
        });
    }

    private void initColumnGeneratorEditor() {
        columnDs.addItemPropertyChangeListener(e -> {
            if ("columnId".equals(e.getProperty())) {
                boolean generatorRequired = false;
                ScreenTableColumn item = e.getItem();
                if (!StringUtils.isEmpty(item.getColumnId())) {
                    MetaProperty property = entityMetaClass.getProperties().stream()
                            .filter(i -> i.getName().equals(item.getColumnId()))
                            .findFirst()
                            .orElse(null);
                    generatorRequired = property == null;
                }
                generatorScriptEditor.setRequired(generatorRequired);
            }
        });
    }

    private void initTemplateField() {
        final boolean[] setup = new boolean[]{true};
        templateField.addValueChangeListener(e -> {
            if (setup[0]) {
                ScreenTableColumnTemplate template = (ScreenTableColumnTemplate) e.getValue();
                ScreenTableColumn column = columnDs.getItem();
                if (column != null) {
                    setupIfEmpty(template, column);
                    column.setTemplate(template == null ? null : template.getId());
                }
            }
        });
        columnDs.addItemChangeListener(e -> {
            setup[0] = false;
            try {
                templateField.setValue(getTemplate(e.getItem()));
            } finally {
                setup[0] = true;
            }
        });
    }

    @Nullable
    private ScreenTableColumnTemplate getTemplate(@Nullable ScreenTableColumn column) {
        return column == null || column.getTemplate() == null ? null : columnTemplatesDs.getItem(column.getTemplate());
    }

    private void setupIfEmpty(@Nullable ScreenTableColumnTemplate template, @Nullable ScreenTableColumn column) {
        if (template != null && column != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                Object value = column.getValue(property);
                if (value == null || StringUtils.isEmpty(value.toString())) {
                    column.setValue(property, template.getValue(property));
                }
            }
        }
    }

    private void cleanupIfSame(@Nullable ScreenTableColumnTemplate template, @Nullable ScreenTableColumn column) {
        if (template != null && column != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                if (Objects.equals(template.getValue(property), column.getValue(property))) {
                    column.setValue(property, null);
                }
            }
        }
    }

    private void enableEdit() {
        changeState(true);
    }

    private void disableEdit() {
        changeState(false);
    }

    private void changeState(boolean editing) {
        columnEditBox.setVisible(editing);
        columnFieldGroup.setEditable(editing);
        generatorScriptEditor.setEditable(editing);
        generatorScriptEditorBox.setVisible(editing);
        captionField.setEditable(editing);
        namePropertiesLookup.setEditable(editing);
        columnsTable.setEnabled(!editing);
    }

    public void generatorScriptHint() {
        createDialog(getMessage("screenConstructorBrowserFrame.generatorScriptTitle"),
                getMessage("screenConstructorBrowserFrame.generatorScriptContent"));
    }

    public void editGeneratorScript() {
        editGroovyInDialog(columnDs.getItem(), "generatorScript");
    }

    public void testGeneratorScript() {
        test(columnDs.getItem().getGeneratorScript(), true);
    }

    public void onOk() {
        if (validateAll()) {
            ScreenTableColumn item = columnDs.getItem();

            if (!isUnique(item)) {
                showNotification(getMessage("screenConstructorBrowserFrame.error.sameColumnExist"), Frame.NotificationType.WARNING);
                return;
            }

            if (editingItem != null) {
                metadataTools.copy(item, editingItem);
                item = editingItem;
                editingItem = null;
            } else {
                columnsDs.addItem(item);
            }

            sortTable(columnsTable, "order");
            columnsTable.setSelected(item);
            columnsDs.refresh();

            disableEdit();
        }
    }

    private boolean isUnique(ScreenTableColumn item) {
        Collection<ScreenTableColumn> exist = columnsDs.getItems();
        if (!CollectionUtils.isEmpty(exist)) {
            return exist.stream()
                    .filter(e -> Objects.equals(e.getColumnId(), item.getColumnId()) &&
                            !Objects.equals(e, item) &&
                            !Objects.equals(e, editingItem))
                    .findFirst()
                    .orElse(null) == null;
        }
        return true;
    }

    public void onCancel() {
        if (columnDs.isModified()) {
            showOptionDialog(
                    messages.getMainMessage("closeUnsaved.caption"),
                    messages.getMainMessage("closeUnsaved"),
                    Frame.MessageType.WARNING,
                    new Action[]{
                            new DialogAction(DialogAction.Type.YES).withHandler(event -> onCloseInternal()),
                            new DialogAction(DialogAction.Type.NO, Action.Status.PRIMARY)
                    }
            );
        } else {
            onCloseInternal();
        }
    }

    private void onCloseInternal() {
        editingItem = null;

        ScreenTableColumn select = null;
        Set<ScreenTableColumn> selected = columnsTable.getSelected();
        if (!CollectionUtils.isEmpty(selected) && selected.size() == 1) {
            select = IterableUtils.get(selected, 0);
        }
        columnDs.setItem(select);
        columnDs.refresh();
        columnDs.setModified(false);

        sortTable(columnsTable, "order");
        columnsDs.refresh();

        disableEdit();
    }
}
