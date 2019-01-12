package com.groupstp.workflowstp.web.screenconstructor.frame.action;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;
import com.groupstp.workflowstp.web.util.action.ItemMoveAction;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.CreateAction;
import com.haulmont.cuba.gui.components.actions.EditAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * Screen constructor actions frame
 *
 * @author adiatullin
 */
public class ScreenConstructorActionsFrame extends AbstractScreenConstructorFrame {

    private static final List<String> TEMPLATE_PROPERTIES = Arrays.asList("alwaysEnabled", "caption", "icon", "style", "shortcut", "buttonAction",
            "script", "permitRequired", "permitItemsCount", "permitItemsType", "permitScript");

    @Inject
    private DataManager dataManager;
    @Inject
    private MetadataTools metadataTools;
    @Inject
    private ComponentsFactory componentsFactory;

    @Inject
    private CollectionDatasource<ScreenActionTemplate, UUID> actionTemplatesDs;
    @Inject
    private DatasourceImplementation<ScreenAction> actionDs;
    @Inject
    private Table<ScreenAction> actionsTable;
    @Inject
    private CollectionDatasource<ScreenAction, UUID> actionsDs;
    @Inject
    private FieldGroup actionsFieldGroup;
    @Inject
    private TextArea actionScriptEditor;
    @Inject
    private FlowBoxLayout actionScriptEditorBox;
    @Inject
    private FlowBoxLayout permitScriptEditorBox;
    @Inject
    private CheckBox permitRequiredChBx;
    @Inject
    private TextArea permitScriptEditor;
    @Inject
    private BoxLayout permitRequiredBox;
    @Inject
    private BoxLayout actionEditBox;
    @Inject
    private BoxLayout tableBox;
    @Inject
    private LookupField iconField;
    @Inject
    private LookupField styleField;
    @Inject
    private LookupField templateField;
    @Inject
    private TextField permitItemsCountField;
    @Inject
    private LookupField permitItemsTypeField;

    private ScreenAction editingItem;

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
        initTableBehaviour();

        initTemplateField();
        initIconField();
        initStyleField();
        initPermitFields();

        disableEdit();
        sortTable(actionsTable, "order");
    }

    private void initTemplates() {
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

    private void initTableColumns() {
        actionsTable.addGeneratedColumn("caption", entity -> {
            String value = entity.getCaption();
            if (StringUtils.isEmpty(value)) {
                ScreenActionTemplate template = getTemplate(entity);
                if (template != null) {
                    value = template.getCaption();
                }
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        });
        actionsTable.addGeneratedColumn("icon", entity -> {
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
        });
        actionsTable.addGeneratedColumn("template", entity -> {
            String value = StringUtils.EMPTY;
            ScreenActionTemplate template = getTemplate(entity);
            if (template != null) {
                value = template.getName();
            }
            return new Table.PlainTextCell(StringUtils.isEmpty(value) ? StringUtils.EMPTY : value);
        });
    }

    private void initTableActions() {
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
        actionsTable.addAction(new ItemMoveAction(actionsTable, true));
        actionsTable.addAction(new ItemMoveAction(actionsTable, false));
    }

    private void initTableBehaviour() {
        actionsDs.addItemChangeListener(e -> {
            cleanupIfSame(getTemplate(e.getPrevItem()), e.getPrevItem());

            ScreenAction select = null;
            Set<ScreenAction> selected = actionsTable.getSelected();
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
        });
        actionsDs.addCollectionChangeListener(e -> {
            correctOrderIfNeed(actionsTable, "order");
        });
    }

    private void initTemplateField() {
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
    private ScreenActionTemplate getTemplate(@Nullable ScreenAction action) {
        return action == null || action.getTemplate() == null ? null : actionTemplatesDs.getItem(action.getTemplate());
    }

    private void setupIfEmpty(@Nullable ScreenActionTemplate template, @Nullable ScreenAction action) {
        if (template != null && action != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                Object value = action.getValue(property);
                if (value == null || StringUtils.isEmpty(value.toString())) {
                    action.setValue(property, template.getValue(property));
                }
            }
        }
    }

    private void cleanupIfSame(@Nullable ScreenActionTemplate template, @Nullable ScreenAction action) {
        if (template != null && action != null) {
            for (String property : TEMPLATE_PROPERTIES) {
                if (Objects.equals(template.getValue(property), action.getValue(property))) {
                    action.setValue(property, null);
                }
            }
        }
    }

    private void initIconField() {
        Map<String, Object> options = new TreeMap<>();
        for (CubaIcon icon : CubaIcon.values()) {
            options.put(icon.name(), icon.source());
        }
        iconField.setOptionsMap(options);
        iconField.setOptionIconProvider(item -> (String) item);
    }

    private void initStyleField() {
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

    private void initPermitFields() {
        permitRequiredChBx.addValueChangeListener(e -> {
            permitRequiredBox.setVisible(permitRequiredChBx.isChecked());
            permitScriptEditorBox.setVisible(permitRequiredChBx.isEditable());
        });
        permitRequiredBox.setVisible(false);
    }

    public void editScript() {
        editGroovyInDialog(actionDs.getItem(), "script");
    }

    public void testScript() {
        test(actionDs.getItem().getScript());
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

    public void editPermitScript() {
        editGroovyInDialog(actionDs.getItem(), "permitScript");
    }

    public void testPermitScript() {
        test(actionDs.getItem().getPermitScript());
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

    private void enableEdit() {
        changeState(true);
    }

    private void disableEdit() {
        changeState(false);
    }

    private void changeState(boolean editing) {
        actionEditBox.setVisible(editing);
        actionsFieldGroup.setEditable(editing);
        actionScriptEditor.setEditable(editing);
        actionScriptEditorBox.setVisible(editing);
        permitRequiredChBx.setEditable(editing);
        permitItemsCountField.setEditable(editing);
        permitItemsTypeField.setEditable(editing);
        permitScriptEditor.setEditable(editing);
        permitScriptEditorBox.setVisible(editing && permitRequiredChBx.isChecked());
        permitRequiredBox.setVisible(permitRequiredChBx.isChecked());
        tableBox.setEnabled(!editing);
    }

    public void onOk() {
        if (validateAll()) {
            ScreenAction action = actionDs.getItem();

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

    private void onCloseInternal() {
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
