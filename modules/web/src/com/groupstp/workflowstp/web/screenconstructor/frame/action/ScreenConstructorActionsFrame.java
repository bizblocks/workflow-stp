package com.groupstp.workflowstp.web.screenconstructor.frame.action;

import com.google.common.collect.Maps;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;
import com.groupstp.workflowstp.web.util.action.ItemMoveAction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.CollectionDatasourceImpl;
import com.haulmont.cuba.gui.data.impl.CollectionPropertyDatasourceImpl;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * Screen constructor actions frame
 *
 * @author adiatullin
 */
public class ScreenConstructorActionsFrame extends AbstractScreenConstructorFrame {

    protected static final List<String> TEMPLATE_PROPERTIES = Arrays.asList("alwaysEnabled", "caption", "icon", "style", "shortcut", "buttonAction",
            "script", "availableInExternalSystem", "externalScript", "permitRequired", "permitItemsCount", "permitItemsType", "permitScript", "externalPermitScript");

    @Inject
    protected DataManager dataManager;
    @Inject
    protected MetadataTools metadataTools;
    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected CollectionDatasource<ScreenActionTemplate, UUID> actionTemplatesDs;
    @Inject
    protected DatasourceImplementation<ScreenAction> actionDs;
    @Inject
    protected Table<ScreenAction> actionsTable;
    @Inject
    protected CollectionPropertyDatasourceImpl<ScreenAction, UUID> actionsDs;
    @Inject
    protected Table<ScreenAction> genericActionsTable;
    @Inject
    protected CollectionDatasourceImpl<ScreenAction, UUID> genericActionsDs;
    @Inject
    protected FieldGroup actionsFieldGroup;
    @Inject
    protected TabSheet scriptTabSheet;
    @Inject
    protected TextArea actionScriptEditor;
    @Inject
    protected TextArea actionExternalScriptEditor;
    @Inject
    protected FlowBoxLayout actionScriptEditorBox;
    @Inject
    protected FlowBoxLayout actionExternalScriptEditorBox;
    @Inject
    protected FlowBoxLayout permitScriptEditorBox;
    @Inject
    protected FlowBoxLayout externalPermitScriptEditorBox;
    @Inject
    protected CheckBox permitRequiredChBx;
    @Inject
    protected TabSheet permitScriptTabSheet;
    @Inject
    protected TextArea permitScriptEditor;
    @Inject
    protected TextArea externalPermitScriptEditor;
    @Inject
    protected BoxLayout permitRequiredBox;
    @Inject
    protected BoxLayout actionEditBox;
    @Inject
    protected BoxLayout tableBox;
    @Inject
    protected LookupField iconField;
    @Inject
    protected LookupField styleField;
    @Inject
    protected LookupField<ScreenActionTemplate> templateField;
    @Inject
    protected TextField permitItemsCountField;
    @Inject
    protected LookupField permitItemsTypeField;

    protected ScreenAction editingItem;

    @Override
    public void prepare() {
        super.prepare();

        Collection<ScreenAction> items = actionsDs.getItems();
        if (!CollectionUtils.isEmpty(items)) {
            for (ScreenAction action : items) {
                cleanupIfSame(getTemplate(action), action);
            }
        }
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initTemplates();
        initTableColumns();
        initTableActions();
        initGenericTableActions();
        initTableBehaviour();

        initTemplateField();
        initIconField();
        initStyleField();
        initExternalScripts();
        initPermitFields();

        disableEdit();
        sortTable(actionsTable, "order");
        sortTable(genericActionsTable, "order");

        if (constructGeneric) {
            genericActionsTable.setVisible(false);
        }
    }

    protected void initTemplates() {
        List<ScreenActionTemplate> templates = dataManager.load(ScreenActionTemplate.class)
                .query("select e from wfstp$ScreenActionTemplate e where e.entityName is null or e.entityName = :entityName")
                .parameter("entityName", entityMetaClass.getName())
                .view(View.LOCAL)
                .list();
        if (!CollectionUtils.isEmpty(templates)) {
            for (ScreenActionTemplate item : templates) {
                actionTemplatesDs.includeItem(item);
            }
        }
    }

    protected void initTableColumns() {
        Table.ColumnGenerator<ScreenAction> generator;

        generator = entity -> {
            String value = entity.getCaption();
            if (StringUtils.isEmpty(value)) {
                ScreenActionTemplate template = getTemplate(entity);
                if (template != null) {
                    value = template.getCaption();
                }
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        };
        actionsTable.addGeneratedColumn("caption", generator);
        genericActionsTable.addGeneratedColumn("caption", generator);

        generator = entity -> {
            String value = entity.getIcon();
            if (StringUtils.isEmpty(value)) {
                ScreenActionTemplate template = getTemplate(entity);
                if (template != null) {
                    value = template.getIcon();
                }
            }
            Label label = componentsFactory.createComponent(Label.class);
            label.setIcon(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
            return label;
        };
        actionsTable.addGeneratedColumn("icon", generator);
        genericActionsTable.addGeneratedColumn("icon", generator);

        generator = entity -> {
            String value = StringUtils.EMPTY;
            ScreenActionTemplate template = getTemplate(entity);
            if (template != null) {
                value = template.getName();
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        };
        actionsTable.addGeneratedColumn("template", generator);
        genericActionsTable.addGeneratedColumn("template", generator);

        generator = entity -> {
            Boolean value = entity.getAvailableInExternalSystem();
            if (value == null) {
                ScreenActionTemplate template = getTemplate(entity);
                if (template != null) {
                    value = Boolean.TRUE.equals(template.getAvailableInExternalSystem());
                }
            }
            return new Table.PlainTextCell(Boolean.TRUE.equals(value) ?
                    messages.getMainMessage("trueString") : messages.getMainMessage("falseString"));
        };
        actionsTable.addGeneratedColumn("availableInExternalSystem", generator);
        genericActionsTable.addGeneratedColumn("availableInExternalSystem", generator);
    }

    protected void initTableActions() {
        actionsTable.addAction(new CreateAction(actionsTable) {
            @Override
            public void actionPerform(Component component) {
                ScreenAction item = metadata.create(ScreenAction.class);
                item.setOrder(actionsDs.size() + 1);

                actionDs.setItem(item);
                actionDs.refresh();
                actionDs.setModified(false);

                enableEdit();
            }
        });
        actionsTable.addAction(new EditAction(actionsTable) {
            @Override
            public void actionPerform(Component component) {
                Set<ScreenAction> selected = actionsTable.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    editingItem = IterableUtils.get(selected, 0);
                    actionDs.setItem(metadataTools.copy(editingItem));//user can click cancel
                    actionDs.refresh();
                    actionDs.setModified(false);

                    enableEdit();
                }
            }

            @Override
            public boolean isPermitted() {
                if (super.isPermitted()) {
                    Set<ScreenAction> selected = actionsTable.getSelected();
                    return !CollectionUtils.isEmpty(selected) && selected.size() == 1;
                }
                return false;
            }
        });
        actionsTable.addAction(new RemoveAction(actionsTable) {
            @Override
            protected void afterRemove(Set selected) {
                super.afterRemove(selected);
                actionDs.setItem(null);
                actionDs.refresh();
                actionDs.setModified(false);
            }
        });
        actionsTable.addAction(new ItemMoveAction(actionsTable, true));
        actionsTable.addAction(new ItemMoveAction(actionsTable, false));
    }

    protected void initGenericTableActions() {
        if (genericScreenConstructor != null && !CollectionUtils.isEmpty(genericScreenConstructor.getActions())) {
            for (ScreenAction action : genericScreenConstructor.getActions()) {
                genericActionsDs.includeItem(action);
            }
        }
    }

    protected void initTableBehaviour() {
        actionsDs.addItemChangeListener(e -> selected(actionsDs, actionsTable, e));
        genericActionsDs.addItemChangeListener(e -> selected(genericActionsDs, genericActionsTable, e));

        actionsDs.addCollectionChangeListener(e -> correctOrderIfNeed(actionsTable, "order"));
    }

    protected void selected(DatasourceImplementation ds, Table<ScreenAction> table, Datasource.ItemChangeEvent<ScreenAction> e) {
        boolean modified = ds.isModified();

        cleanupIfSame(getTemplate(e.getPrevItem()), e.getPrevItem());

        ScreenAction select = null;
        Set<ScreenAction> selected = table.getSelected();
        if (!CollectionUtils.isEmpty(selected)) {
            if (selected.size() == 1) {
                select = IterableUtils.get(selected, 0);
            }
        }
        if (select != null) {
            setupIfEmpty(getTemplate(select), select);
        }
        actionDs.setItem(select);
        actionDs.refresh();
        actionDs.setModified(false);

        ds.setModified(modified);

        checkExternalTabs();
    }

    protected void initTemplateField() {
        final boolean[] setup = new boolean[]{true};
        templateField.addValueChangeListener(e -> {
            if (setup[0]) {
                ScreenActionTemplate template = (ScreenActionTemplate) e.getValue();
                ScreenAction action = actionDs.getItem();
                if (action != null) {
                    setupIfEmpty(template, action);
                    action.setTemplate(template == null ? null : template.getId());
                }
            }
        });
        actionDs.addItemChangeListener(e -> {
            setup[0] = false;
            try {
                templateField.setValue(getTemplate(e.getItem()));
            } finally {
                setup[0] = true;
            }
        });
    }

    @Nullable
    protected ScreenActionTemplate getTemplate(@Nullable ScreenAction action) {
        return action == null || action.getTemplate() == null ? null : actionTemplatesDs.getItem(action.getTemplate());
    }

    protected void setupIfEmpty(@Nullable ScreenActionTemplate template, @Nullable ScreenAction action) {
        if (template != null && action != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                Object value = action.getValue(property);
                if (value == null || StringUtils.isEmpty(value.toString())) {
                    action.setValue(property, template.getValue(property));
                }
            }
        }
    }

    protected void cleanupIfSame(@Nullable ScreenActionTemplate template, @Nullable ScreenAction action) {
        if (template != null && action != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                if (Objects.equals(template.getValue(property), action.getValue(property))) {
                    action.setValue(property, null);
                }
            }
        }
    }

    protected void initIconField() {
        Map<String, Object> options = new TreeMap<>();
        Set<Object> values = new HashSet<>();
        for (CubaIcon icon : CubaIcon.values()) {
            if (!values.contains(icon.source())){
                options.put(icon.name(), icon.source());
                values.add(icon.source());
            }
        }

        iconField.setOptionsMap(options);
        iconField.setOptionIconProvider(item -> (String) item);
    }

    protected void initStyleField() {
        Map<String, Object> options = new TreeMap<>();
        options.put(ValoTheme.BUTTON_DANGER, ValoTheme.BUTTON_DANGER);
        options.put(ValoTheme.BUTTON_FRIENDLY, ValoTheme.BUTTON_FRIENDLY);
        options.put(ValoTheme.BUTTON_HUGE, ValoTheme.BUTTON_HUGE);
        options.put(ValoTheme.BUTTON_ICON_ONLY, ValoTheme.BUTTON_ICON_ONLY);
        options.put(ValoTheme.BUTTON_PRIMARY, ValoTheme.BUTTON_PRIMARY);
        options.put(ValoTheme.BUTTON_TINY, ValoTheme.BUTTON_TINY);
        options.put(ValoTheme.BUTTON_BORDERLESS, ValoTheme.BUTTON_BORDERLESS);
        options.put(ValoTheme.BUTTON_BORDERLESS_COLORED, ValoTheme.BUTTON_BORDERLESS_COLORED);
        options.put(ValoTheme.BUTTON_ICON_ALIGN_RIGHT, ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
        options.put(ValoTheme.BUTTON_ICON_ALIGN_TOP, ValoTheme.BUTTON_ICON_ALIGN_TOP);
        options.put(ValoTheme.BUTTON_LARGE, ValoTheme.BUTTON_LARGE);
        options.put(ValoTheme.BUTTON_LINK, ValoTheme.BUTTON_LINK);
        options.put(ValoTheme.BUTTON_QUIET, ValoTheme.BUTTON_QUIET);
        options.put(ValoTheme.BUTTON_SMALL, ValoTheme.BUTTON_SMALL);
        styleField.setOptionsMap(options);
    }

    protected void initExternalScripts() {
        actionDs.addItemPropertyChangeListener(e -> {
            checkExternalTabs();
        });
        checkExternalTabs();
    }

    protected void initPermitFields() {
        permitRequiredChBx.addValueChangeListener(e -> {
            permitRequiredBox.setVisible(permitRequiredChBx.isChecked());
            permitScriptEditorBox.setVisible(permitRequiredChBx.isEditable());
            externalPermitScriptEditorBox.setVisible(permitRequiredChBx.isEditable());
        });
        permitRequiredBox.setVisible(false);
    }

    public void editScript() {
        editGroovyInDialog(actionDs.getItem(), "script");
    }

    public void editExternalScript() {
        editGroovyInDialog(actionDs.getItem(), "externalScript");
    }

    public void testScript() {
        test(actionDs.getItem().getScript());
    }

    public void testExternalScript() {
        testExternal(actionDs.getItem().getExternalScript());
    }

    public void scriptHint() {
        if (Boolean.TRUE.equals(screenConstructor.getIsBrowserScreen())) {
            createDialog(getMessage("screenConstructorActionsFrame.browse.scriptHintTitle"),
                    getMessage("screenConstructorActionsFrame.browse.scriptHintContent"));
        } else {
            createDialog(getMessage("screenConstructorActionsFrame.edit.scriptHintTitle"),
                    getMessage("screenConstructorActionsFrame.edit.scriptHintContent"));
        }
    }

    public void externalScriptHint() {
        createDialog(getMessage("screenConstructorActionsFrame.browse.scriptHintTitle"),
                getMessage("screenConstructorActionsFrame.browse.externalScriptHint"));
    }

    public void editPermitScript() {
        editGroovyInDialog(actionDs.getItem(), "permitScript");
    }

    public void editExternalPermitScript() {
        editGroovyInDialog(actionDs.getItem(), "externalPermitScript");
    }

    public void testPermitScript() {
        test(actionDs.getItem().getPermitScript());
    }

    public void testExternalPermitScript() {
        testExternal(actionDs.getItem().getExternalPermitScript());
    }

    public void permitScriptHint() {
        if (Boolean.TRUE.equals(screenConstructor.getIsBrowserScreen())) {
            createDialog(getMessage("screenConstructorActionsFrame.browse.permitScriptHintTitle"),
                    getMessage("screenConstructorActionsFrame.browse.permitScriptHintContent"));
        } else {
            createDialog(getMessage("screenConstructorActionsFrame.edit.permitScriptHintTitle"),
                    getMessage("screenConstructorActionsFrame.edit.permitScriptHintContent"));
        }
    }

    public void externalPermitScriptHint() {
        createDialog(getMessage("screenConstructorActionsFrame.browse.scriptHintTitle"),
                getMessage("screenConstructorActionsFrame.edit.permitExternalScriptHintContent"));
    }

    protected void enableEdit() {
        changeState(true);
    }

    protected void disableEdit() {
        changeState(false);
    }

    protected void changeState(boolean editing) {
        actionEditBox.setVisible(editing);
        actionsFieldGroup.setEditable(editing);
        actionScriptEditor.setEditable(editing);
        actionScriptEditorBox.setVisible(editing);
        actionExternalScriptEditor.setEditable(editing);
        actionExternalScriptEditorBox.setVisible(editing);
        permitRequiredChBx.setEditable(editing);
        permitItemsCountField.setEditable(editing);
        permitItemsTypeField.setEditable(editing);
        permitScriptEditor.setEditable(editing);
        permitScriptEditorBox.setVisible(editing && permitRequiredChBx.isChecked());
        externalPermitScriptEditor.setEditable(editing);
        externalPermitScriptEditorBox.setVisible(editing && permitRequiredChBx.isChecked());
        permitRequiredBox.setVisible(permitRequiredChBx.isChecked());
        tableBox.setEnabled(!editing);

        checkExternalTabs();
    }

    protected void checkExternalTabs() {
        boolean externalEnabled = Boolean.TRUE.equals(actionDs.getItem() == null ? null : actionDs.getItem().getAvailableInExternalSystem());
        scriptTabSheet.getTab("externalScriptTab").setVisible(externalEnabled);
        permitScriptTabSheet.getTab("externalPermitScriptTab").setVisible(externalEnabled);
    }

    public void onOk() {
        if (validateAll()) {
            ScreenAction action = actionDs.getItem();

            if (Boolean.TRUE.equals(action.getAvailableInExternalSystem())) {
                if (StringUtils.isEmpty(action.getExternalScript())) {
                    scriptTabSheet.setSelectedTab("externalScriptTab");
                    showNotification(getMessage("screenConstructorActionsFrame.edit.pleaseSetupExternalScript"), NotificationType.WARNING);
                    return;
                }
            } else {
                action.setExternalScript(null);
                action.setExternalPermitScript(null);
            }

            if (editingItem != null) {
                metadataTools.copy(action, editingItem);
                action = editingItem;
                editingItem = null;
            } else {
                actionsDs.addItem(action);
            }

            sortTable(actionsTable, "order");
            actionsTable.setSelected(action);
            actionsDs.refresh();

            disableEdit();
        }
    }

    public void onCancel() {
        if (actionDs.isModified()) {
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

    protected void onCloseInternal() {
        editingItem = null;

        ScreenAction select = null;
        Set<ScreenAction> selected = actionsTable.getSelected();
        if (!CollectionUtils.isEmpty(selected) && selected.size() == 1) {
            select = IterableUtils.get(selected, 0);
        }
        actionDs.setItem(select);
        actionDs.refresh();
        actionDs.setModified(false);

        sortTable(actionsTable, "order");
        actionsDs.refresh();

        disableEdit();
    }
}
