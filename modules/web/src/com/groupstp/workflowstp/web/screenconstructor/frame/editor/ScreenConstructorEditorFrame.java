package com.groupstp.workflowstp.web.screenconstructor.frame.editor;

import com.groupstp.workflowstp.entity.ScreenField;
import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class ScreenConstructorEditorFrame extends AbstractScreenConstructorFrame {

    @Inject
    private MessageTools messageTools;
    @Inject
    private MetadataTools metadataTools;

    @Inject
    private Table<ScreenField> fieldsTable;
    @Inject
    private DatasourceImplementation<ScreenField> fieldDs;
    @Inject
    private CollectionDatasource<ScreenField, UUID> fieldsDs;
    @Inject
    private FieldGroup fieldFieldGroup;
    @Inject
    private LookupField nameLookup;
    @Inject
    private BoxLayout fieldEditBox;

    private ScreenField editingItem;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initFieldsTable();
        initFieldsNamesLookup();
        initTableBehaviour();

        disableEdit();
        sortTable(fieldsTable, "name");
    }

    private void initFieldsTable() {
        fieldsTable.addAction(new CreateAction(fieldsTable) {
            @Override
            public void actionPerform(Component component) {
                ScreenField item = metadata.create(ScreenField.class);

                fieldDs.setItem(item);
                fieldDs.refresh();
                fieldDs.setModified(false);

                enableEdit();
            }
        });
        fieldsTable.addAction(new EditAction(fieldsTable) {
            @Override
            public void actionPerform(Component component) {
                Set<ScreenField> selected = fieldsTable.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    editingItem = IterableUtils.get(selected, 0);
                    fieldDs.setItem(metadataTools.copy(editingItem));//user can click cancel
                    fieldDs.refresh();
                    fieldDs.setModified(false);

                    enableEdit();
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set selected = fieldsTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
        fieldsTable.addAction(new RemoveAction(fieldsTable) {
            @Override
            protected void afterRemove(Set selected) {
                super.afterRemove(selected);
                fieldDs.setItem(null);
                fieldDs.refresh();
                fieldDs.setModified(false);
            }
        });
    }

    private void initFieldsNamesLookup() {
        final Map<String, FieldDescription> options = new TreeMap<>();
        ComponentsHelper.walkComponents(extendingWindow, (component, name) -> {
            if (component instanceof DatasourceComponent) {
                DatasourceComponent dsComponent = (DatasourceComponent) component;
                MetaPropertyPath propertyPath = dsComponent.getMetaPropertyPath();
                if (propertyPath != null) {
                    if (!StringUtils.isEmpty(component.getId())) {
                        if (entityMetaClass.getProperties().contains(propertyPath.getMetaProperty())) {
                            String caption = messageTools.getPropertyCaption(propertyPath.getMetaProperty());

                            FieldDescription desc = new FieldDescription();
                            desc.caption = caption;
                            desc.fieldId = component.getId();
                            desc.property = propertyPath.getMetaProperty().getName();

                            options.put(caption, desc);
                        }
                    }
                }
            } else if (component instanceof FieldGroup) {
                FieldGroup fg = (FieldGroup) component;
                if (fg.getId() != null) {
                    List<FieldGroup.FieldConfig> fields = fg.getFields();
                    if (!CollectionUtils.isEmpty(fields)) {
                        for (FieldGroup.FieldConfig field : fields) {
                            if (!StringUtils.isEmpty(field.getProperty())) {
                                if (field.getId() != null) {
                                    if (entityMetaClass.getProperty(field.getProperty()) != null) {
                                        if (entityMetaClass.equals(field.getDatasource().getMetaClass())) {
                                            String caption = messageTools.getPropertyCaption(entityMetaClass.getPropertyNN(field.getProperty()));

                                            FieldDescription desc = new FieldDescription();
                                            desc.caption = caption;
                                            desc.fieldId = fg.getId() + "." + field.getId();
                                            desc.property = field.getProperty();

                                            options.put(caption, desc);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                String id = component.getId();
                MetaProperty metaProperty = entityMetaClass.getProperty(id);
                if (metaProperty != null) {
                    String caption = messageTools.getPropertyCaption(metaProperty);

                    FieldDescription desc = new FieldDescription();
                    desc.caption = caption;
                    desc.fieldId = id;
                    desc.property = metaProperty.getName();

                    options.put(caption, desc);
                }
            }
        });
        nameLookup.setOptionsMap(options);
        boolean[] setup = new boolean[]{true};
        nameLookup.addValueChangeListener(e -> {
            if (setup[0]) {
                //noinspection unchecked
                FieldDescription desc = (FieldDescription) e.getValue();
                String name = desc == null ? null : desc.caption;
                String fieldId = desc == null ? null : desc.fieldId;
                String property = desc == null ? null : desc.property;

                fieldDs.getItem().setName(name);
                fieldDs.getItem().setFieldId(fieldId);
                fieldDs.getItem().setProperty(property);
            }
        });
        fieldDs.addItemChangeListener(e -> {
            setup[0] = false;
            try {
                ScreenField field = e.getItem();
                nameLookup.setValue(field == null ? null : field.getName());
            } finally {
                setup[0] = true;
            }
        });
    }

    private void initTableBehaviour() {
        fieldsDs.addItemChangeListener(e -> {
            ScreenField select = null;
            Set<ScreenField> selected = fieldsTable.getSelected();
            if (!CollectionUtils.isEmpty(selected)) {
                if (selected.size() == 1) {
                    select = IterableUtils.get(selected, 0);
                }
            }
            fieldDs.setItem(select);
            fieldDs.refresh();
            fieldDs.setModified(false);
        });
    }

    private void enableEdit() {
        changeState(true);
    }

    private void disableEdit() {
        changeState(false);
    }

    private void changeState(boolean editing) {
        fieldEditBox.setVisible(editing);
        fieldFieldGroup.setEditable(editing);
        fieldsTable.setEnabled(!editing);
    }

    public void onOk() {
        if (validateAll()) {
            ScreenField item = fieldDs.getItem();

            if (!isUnique(item)) {
                showNotification(getMessage("screenConstructorEditorFrame.error.sameFieldExist"), Frame.NotificationType.WARNING);
                return;
            }

            if (editingItem != null) {
                metadataTools.copy(item, editingItem);
                item = editingItem;
                editingItem = null;
            } else {
                fieldsDs.addItem(item);
            }

            sortTable(fieldsTable, "name");
            fieldsTable.setSelected(item);
            fieldsDs.refresh();

            disableEdit();
        }
    }

    private boolean isUnique(ScreenField item) {
        Collection<ScreenField> exist = fieldsDs.getItems();
        if (!CollectionUtils.isEmpty(exist)) {
            return exist.stream()
                    .filter(e -> Objects.equals(e.getFieldId(), item.getFieldId()) &&
                            !Objects.equals(e, item) &&
                            !Objects.equals(e, editingItem))
                    .findFirst()
                    .orElse(null) == null;
        }
        return true;
    }

    public void onCancel() {
        if (fieldDs.isModified()) {
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

        ScreenField select = null;
        Set<ScreenField> selected = fieldsTable.getSelected();
        if (!CollectionUtils.isEmpty(selected) && selected.size() == 1) {
            select = IterableUtils.get(selected, 0);
        }
        fieldDs.setItem(select);
        fieldDs.refresh();
        fieldDs.setModified(false);

        sortTable(fieldsTable, "name");
        fieldsDs.refresh();

        disableEdit();
    }

    private static final class FieldDescription {
        String caption;
        String fieldId;
        String property;
    }
}
