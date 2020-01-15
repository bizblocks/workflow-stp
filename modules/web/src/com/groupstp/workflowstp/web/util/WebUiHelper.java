package com.groupstp.workflowstp.web.util;

import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.web.components.ExternalSelectionGroupTable;
import com.groupstp.workflowstp.web.config.WorkflowWebConfig;
import com.groupstp.workflowstp.web.util.action.AlwaysActiveBaseAction;
import com.groupstp.workflowstp.web.util.data.ColumnGenerator;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.ComponentsHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.formatters.DateFormatter;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Web side workflow screen extension util class
 *
 * @author adiatullin
 */
@SuppressWarnings("unchecked")
@org.springframework.stereotype.Component(WebUiHelper.NAME)
public class WebUiHelper {

    public static final String NAME = "WebUiHelper";

    protected static Element CREATE_TS_ELEMENT = Dom4j.readDocument("<createTs format=\"dd.MM.yyyy\" useUserTimezone=\"true\"/>").getRootElement();
    protected static String DATE_FORMAT = "dd.MM.yyyy";
    protected static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT);
    protected static DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        DECIMAL_FORMAT = new DecimalFormat("#,###.##", formatSymbols);
    }

    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected Security security;
    @Inject
    protected MessageTools messageTools;
    @Inject
    protected Messages messages;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected WorkflowService workflowService;
    @Inject
    protected UserSessionSource userSessionSource;

    @Inject
    protected WorkflowWebConfig workflowWebConfig;

    /**
     * Show related entity in table as link
     *
     * @param table          master entity table
     * @param entityProperty slave entity property name
     */
    public static void showLinkOnTable(Table table, String entityProperty) {
        ((WebUiHelper) AppBeans.get(NAME)).showLinkOnTableInner(table, entityProperty);
    }

    protected void showLinkOnTableInner(Table table, String entityProperty) {
        showLinkOnTableInternal(table, entityProperty, null);
    }

    /**
     * Show related entity in table as link
     *
     * @param table           master entity table
     * @param entityProperty  slave entity property name
     * @param captionFunction function to generate related entity link caption
     */
    public static void showLinkOnTable(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        ((WebUiHelper) AppBeans.get(NAME)).showLinkOnTableInternal(table, entityProperty, captionFunction);
    }

    protected void showLinkOnTableInternal(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        table.addGeneratedColumn(entityProperty, new Table.ColumnGenerator<Entity>() {
            @Override
            public Component generateCell(Entity entity) {
                LinkButton link = componentsFactory.createComponent(LinkButton.class);
                final Entity nested = entity.getValue(entityProperty);
                if (nested != null) {
                    link.setAction(new BaseAction(entityProperty + "Link") {
                        @Override
                        public void actionPerform(Component component) {
                            Window.Editor editor = table.getFrame().openEditor(nested, WindowManager.OpenType.THIS_TAB);
                            editor.addCloseListener(actionId -> table.getDatasource().refresh());
                        }

                        @Override
                        public boolean isPermitted() {
                            return super.isPermitted() && security.isPermitted(nested, ConstraintOperationType.READ);
                        }
                    });
                    link.setCaption(captionFunction == null ? nested.getInstanceName() : captionFunction.apply(nested));
                } else {
                    link.setCaption(StringUtils.EMPTY);
                }
                return link;
            }
        });
    }

    /**
     * Hide up all lookup actions from screen if user do not have permission to reach them
     *
     * @param screen user opened window
     */
    public static void hideLookupActionInFields(Frame screen) {
        ((WebUiHelper) AppBeans.get(NAME)).hideLookupActionInFieldsInternal(screen);
    }

    protected void hideLookupActionInFieldsInternal(Frame screen) {
        Collection<Component> components = screen.getComponents();
        if (!CollectionUtils.isEmpty(components)) {
            for (Component component : components) {
                if (component instanceof LookupPickerField) {
                    LookupPickerField field = (LookupPickerField) component;
                    if (field.getAction(LookupPickerField.LookupAction.NAME) != null) {
                        MetaClass metaClass = field.getMetaClass();
                        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
                            field.removeAction(LookupPickerField.LookupAction.NAME);
                        }
                    }
                }
            }
        }
    }

    /**
     * Enable special field to editable mode
     *
     * @param container     components container
     * @param componentsIds editable components ids
     */
    public static void enableComponents(com.haulmont.cuba.gui.components.ComponentContainer container, List<String> componentsIds) {
        ((WebUiHelper) AppBeans.get(NAME)).enableComponentsInternal(container, componentsIds);
    }

    protected void enableComponentsInternal(com.haulmont.cuba.gui.components.ComponentContainer container, List<String> componentsIds) {
        ComponentsHelper.walkComponents(container, (component, name) -> {
            if (component instanceof FieldGroup) {
                FieldGroup fg = (FieldGroup) component;
                if (fg.getId() != null) {
                    List<FieldGroup.FieldConfig> fields = fg.getFields();
                    if (!CollectionUtils.isEmpty(fields)) {
                        for (FieldGroup.FieldConfig field : fields) {
                            if (field.getId() != null && componentsIds.contains(fg.getId() + "." + field.getId())) {
                                field.setEditable(true);
                            }
                        }
                    }
                }
            }
            if (componentsIds.contains(component.getId())) {
                if (component instanceof Component.Editable) {
                    ((Component.Editable) component).setEditable(true);
                } else {
                    component.setEnabled(true);
                }
                if (component instanceof ActionsHolder) {
                    Collection<Action> actions = ((ActionsHolder) component).getActions();
                    if (!CollectionUtils.isEmpty(actions)) {
                        for (Action action : actions) {
                            action.setEnabled(true);
                        }
                    }
                }
                if (component instanceof HasButtonsPanel) {
                    ButtonsPanel bp = ((HasButtonsPanel) component).getButtonsPanel();
                    if (bp != null) {
                        Collection<Component> components = bp.getComponents();
                        if (!CollectionUtils.isEmpty(components)) {
                            for (Component c : components) {
                                c.setEnabled(true);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Setup table columns from specified set
     *
     * @param table      UI table
     * @param columns    which properties are should shown
     * @param generators custom column generators map
     */
    public static void showColumns(Table table, List<String> columns, Map<String, ColumnGenerator> generators) {
        ((WebUiHelper) AppBeans.get(NAME)).showColumnsInternal(table, columns, generators);
    }

    protected void showColumnsInternal(Table table, List<String> columns, Map<String, ColumnGenerator> generators) {
        clearColumns(table);

        showSelectionColumn(table);

        if (generators == null) {
            generators = Collections.emptyMap();
        }

        if (!CollectionUtils.isEmpty(columns)) {
            MetaClass metaClass = table.getDatasource().getMetaClass();

            for (String property : columns) {
                ColumnGenerator custom = generators.get(property);
                if (custom != null && custom.getReadGenerator() != null) {
                    table.addGeneratedColumn(property, custom.getReadGenerator());
                    try {
                        table.addColumn(table.getColumn(property));//to support table sort
                    } catch (UnsupportedOperationException ignore) {
                    }
                } else {
                    MetaProperty metaProperty = metaClass.getPropertyNN(property);
                    MetaPropertyPath path = metaClass.getPropertyPath(property);
                    assert path != null;

                    Table.Column column = new Table.Column(path, property);
                    column.setType(path.getRangeJavaClass());
                    column.setCaption(messageTools.getPropertyCaption(metaProperty));
                    table.addColumn(column);
                }
            }
        }
    }

    /**
     * Prepare table to show specified columns and mark some of them as editable with saving grouping and sorting feature
     *
     * @param table           UI table to prepare
     * @param columns         all showing table columns
     * @param editableColumns editable table columns
     * @param generators      custom column generators for showing the columns
     * @param viewOnly        is table showing for view only or not
     */
    public static void showColumns(Table table, List<String> columns, List<String> editableColumns,
                                   Map<String, ColumnGenerator> generators, Boolean viewOnly) {
        ((WebUiHelper) AppBeans.get(NAME)).showColumnsInternal(table, columns, editableColumns, generators, viewOnly);
    }

    protected void showColumnsInternal(Table table, List<String> columns, List<String> editableColumns,
                                       Map<String, ColumnGenerator> generators, Boolean viewOnly) {
        clearColumns(table);

        showSelectionColumn(table);

        if (columns == null) {
            columns = Collections.emptyList();
        }
        if (editableColumns == null) {
            editableColumns = Collections.emptyList();
        }

        final List editing = new ArrayList<>();
        final CollectionDatasource ds = table.getDatasource();
        final MetaClass metaClass = ds.getMetaClass();

        for (String property : columns) {
            final MetaPropertyPath path = metaClass.getPropertyPath(property);
            final MetaProperty metaProperty = metaClass.getProperty(property);

            Table.Column column;

            boolean currentEditable = !Boolean.TRUE.equals(viewOnly) && editableColumns.contains(property) &&
                    security.isEntityAttrPermitted(metaClass, property, EntityAttrAccess.MODIFY);
            if (currentEditable) {
                table.addGeneratedColumn(property, entity -> {
                    ColumnGenerator generator = generators.get(property);
                    if (editing.contains(entity.getId())) {
                        if (generator != null && generator.getEditGenerator() != null) {
                            return generator.getEditGenerator().generateCell(entity);
                        }

                        assert path != null;
                        return getEditableComponent(table, path, entity);
                    } else {
                        if (generator != null && generator.getReadGenerator() != null) {
                            return generator.getReadGenerator().generateCell(entity);
                        }

                        assert path != null;
                        return getNotEditableComponent(path, entity);
                    }
                });
                column = table.getColumn(property);
            } else {
                ColumnGenerator custom = generators.get(property);
                if (custom != null && custom.getReadGenerator() != null) {
                    table.addGeneratedColumn(property, custom.getReadGenerator());
                    column = table.getColumn(property);
                } else {
                    assert path != null;
                    column = new Table.Column(path, property);
                }
            }

            if (path != null && metaProperty != null) {
                column.setType(path.getRangeJavaClass());
                column.setCaption(messageTools.getPropertyCaption(metaProperty));
            }

            try {
                table.addColumn(column);
            } catch (UnsupportedOperationException ignore) {
            }
        }
        //listening items removing
        ds.addCollectionChangeListener(e -> {
            Collection<Entity> items = ds.getItems();
            if (!CollectionUtils.isEmpty(items)) {
                List remove = new ArrayList<>(editing);
                for (Entity item : items) {
                    remove.remove(item.getId());
                }
                editing.removeAll(remove);
            } else {
                editing.clear();
            }
        });
        if (!Boolean.TRUE.equals(viewOnly)) {
            AbstractAction action = new AbstractAction("inlineEdit") {
                @Override
                public String getCaption() {
                    return messages.getMainMessage("workflow.inlineEdit");
                }

                @Override
                public void actionPerform(Component component) {
                    Set<Entity> selectedSet = getSelected();
                    Entity selected = CollectionUtils.isEmpty(selectedSet) ? null : IterableUtils.get(selectedSet, selectedSet.size() - 1);
                    Object id = selected == null ? null : selected.getId();
                    if (id != null) {
                        if (editing.contains(id)) {//disable inline editing mode
                            editing.remove(id);
                            if (ds.isModified()) {
                                ds.commit();
                                setSelected(selected);
                            } else {
                                table.repaint();
                            }
                        } else {
                            if (!CollectionUtils.isEmpty(editing)) {
                                if (ds.isModified()) {
                                    ds.commit();
                                }
                                editing.clear();
                            }

                            editing.add(id);//activate inline editing mode
                            table.repaint();
                        }
                    }
                }

                private Set<Entity> getSelected() {
                    if (table instanceof ExternalSelectionGroupTable) {
                        return ((ExternalSelectionGroupTable) table).getSelectedInternal();
                    }
                    return table.getSelected();
                }

                private void setSelected(Entity entity) {
                    table.setSelected(entity);
                }
            };
            table.setItemClickAction(action);
            table.setEnterPressAction(action);
        }
    }

    protected Component getEditableComponent(Table table, MetaPropertyPath path, Entity entity) {
        Field result;
        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            result = componentsFactory.createComponent(CheckBox.class);
        } else if (Entity.class.isAssignableFrom(path.getRangeJavaClass())) {
            LookupField field = componentsFactory.createComponent(LookupField.class);
            field.setOptionsList(getItemsList(metadata.getClassNN(path.getRangeJavaClass())));
            result = field;
            result.setWidth("100%");
        } else if (Enum.class.isAssignableFrom(path.getRangeJavaClass())) {
            LookupField field = componentsFactory.createComponent(LookupField.class);
            field.setOptionsEnum(path.getRangeJavaClass());
            result = field;
            result.setWidth("100%");
        } else if (Date.class.isAssignableFrom(path.getRangeJavaClass())) {
            DateField field = componentsFactory.createComponent(DateField.class);
            field.setDateFormat(DATE_FORMAT);
            result = field;
            result.setWidth("100%");
        } else {
            TextField field = componentsFactory.createComponent(TextField.class);
            field.setDatatype(path.getRange().asDatatype());
            result = field;
            result.setWidth("100%");
        }

        result.setDatasource(table.getItemDatasource(entity), path.getMetaProperty().getName());

        return result;
    }

    protected Component getNotEditableComponent(MetaPropertyPath path, Entity entity) {
        final Object value = entity.getValue(path.getMetaProperty().getName());

        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            CheckBox checkBox = componentsFactory.createComponent(CheckBox.class);
            checkBox.setEditable(false);
            checkBox.setValue((Boolean) value);
            return checkBox;
        } else {
            String text = StringUtils.EMPTY;
            if (value != null) {
                if (Date.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = SIMPLE_DATE_FORMAT.format((Date) value);
                } else if (Entity.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = ((Entity) value).getInstanceName();
                } else if (Enum.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = messages.getMessage((Enum) value);
                } else if (BigDecimal.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = DECIMAL_FORMAT.format(((BigDecimal) value).doubleValue());
                } else {
                    text = value.toString();
                }
            }
            return new Table.PlainTextCell(text);
        }
    }

    protected List getItemsList(MetaClass metaClass) {
        Collection<MetaProperty> namePatternProperties = metadata.getTools().getNamePatternProperties(metaClass, true);
        if (CollectionUtils.isEmpty(namePatternProperties)) {
            throw new DevelopmentException(String.format("Unknown entity '%s' name pattern", metaClass.getName()));
        }
        String orderProperty = IterableUtils.get(namePatternProperties, 0).getName();

        List list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                .setQuery(LoadContext.createQuery("select e from " + metaClass.getName() + " e order by e." + orderProperty))
                .setView(View.MINIMAL));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    protected void clearColumns(Table table) {
        List<Table.Column> columns = table.getColumns();
        if (!CollectionUtils.isEmpty(columns)) {
            columns = new ArrayList<>(columns);
            for (Table.Column column : columns) {
                table.removeColumn(column);
            }
        }
    }

    protected void showSelectionColumn(Table table) {
        if (isShowSelection()) {
            if (!(table instanceof ExternalSelectionGroupTable)) {
                throw new UnsupportedOperationException("Table not support external selections");
            }

            ExternalSelectionGroupTable sTable = (ExternalSelectionGroupTable) table;
            sTable.setExternalSelectionEnabled(true);

            final Map<Entity, CheckBox> cache = new HashMap<>();

            sTable.addGeneratedColumn("selection", entity -> {
                CheckBox checkBox = cache.get(entity);
                if (checkBox == null) {
                    checkBox = componentsFactory.createComponent(CheckBox.class);

                    Set selected = sTable.getSelectedExternal();
                    checkBox.setValue(!CollectionUtils.isEmpty(selected) && selected.contains(entity));

                    checkBox.addValueChangeListener(e -> {
                        if (((CheckBox) e.getComponent()).isChecked()) {
                            sTable.addExternalSelection(entity);
                        } else {
                            sTable.removeExternalSelection(entity);
                        }
                    });
                    cache.put(entity, checkBox);
                }
                return checkBox;
            });
            sTable.setColumnCaption("selection", messages.getMainMessage("workflow.selection"));

            AlwaysActiveBaseAction selectAction = new AlwaysActiveBaseAction("selectAction") {
                @Override
                public String getCaption() {
                    return messages.getMainMessage("workflow.selection");
                }

                @Override
                public void actionPerform(Component component) {
                    Set<Entity> selected = sTable.getSelectedInternal();
                    if (!CollectionUtils.isEmpty(selected)) {
                        for (Entity e : selected) {
                            CheckBox checkBox = cache.get(e);
                            if (checkBox != null) {
                                checkBox.setValue(Boolean.TRUE);
                            }
                        }
                    }
                }
            };
            selectAction.setShortcut("CTRL-S");

            sTable.addAction(selectAction);

            sTable.getDatasource().addItemChangeListener(e -> {
                Set<Entity> toRemove = null;
                Collection all = table.getDatasource().getItems();
                for (Map.Entry<Entity, CheckBox> entry : cache.entrySet()) {
                    if (!all.contains(entry.getKey())) {
                        if (toRemove == null) {
                            toRemove = new HashSet<>();
                        }
                        toRemove.add(entry.getKey());
                    }
                }
                if (!CollectionUtils.isEmpty(toRemove)) {
                    for (Entity entity : toRemove) {
                        cache.remove(entity);
                        sTable.removeExternalSelection(entity);
                    }
                }
            });

            AlwaysActiveBaseAction action = new AlwaysActiveBaseAction("clearSelection") {
                @Override
                public String getCaption() {
                    return messages.getMainMessage("workflow.clearSelection");
                }

                @Override
                public String getIcon() {
                    return CubaIcon.ERASER.source();
                }

                @Override
                public void actionPerform(Component component) {
                    Set<Entity> selected = sTable.getSelectedExternal();
                    if (!CollectionUtils.isEmpty(selected)) {
                        for (Entity entity : selected) {
                            cache.remove(entity);
                            sTable.removeExternalSelection(entity);
                        }
                        sTable.setSelected((Entity) null);
                        sTable.repaint();
                    }
                }

                @Override
                protected boolean isPermitted() {
                    return super.isPermitted() && !CollectionUtils.isEmpty(sTable.getSelectedExternal());
                }
            };
            action.setShortcut("CTRL-Z");

            Button button = componentsFactory.createComponent(Button.class);
            button.setAction(action);
            table.addAction(action);
            table.getButtonsPanel().add(button);
        }
    }

    protected boolean isShowSelection() {
        return Boolean.TRUE.equals(workflowWebConfig.getShowSelection());
    }

    /**
     * Correct CreateTS column in table to show it without the time part
     *
     * @param table UI table with CreateTS column
     */
    public static void createTsWithoutTime(Table table) {
        ((WebUiHelper) AppBeans.get(NAME)).createTsWithoutTimeInternal(table);
    }

    protected void createTsWithoutTimeInternal(Table table) {
        Table.Column column = table.getColumn("createTs");
        if (column != null) {
            column.setFormatter(new DateFormatter(CREATE_TS_ELEMENT));
        }
    }

    /**
     * Default workflow execution action
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.web.util.WebUiHelper;
     * import com.groupstp.workflowstp.web.util.MapHelper;
     * <p>
     * Map params = MapHelper.asMap("solved", "true");//setup all
     * WebUiHelper.performWorkflowAction(entity, target, workflowInstanceTask, workflowInstance, stage, screen, params);
     * <p>
     *
     * @param entity   editor screen entity
     * @param target   target actions UI component
     * @param task     editor screen task
     * @param instance editor screen workflow instance
     * @param stage    browser screen stage
     * @param screen   UI calling frame
     * @param params   processing parameters
     */
    public static void performWorkflowAction(@Nullable WorkflowEntity entity,
                                             Object target,
                                             @Nullable WorkflowInstanceTask task,
                                             @Nullable WorkflowInstance instance,
                                             @Nullable Stage stage,
                                             Frame screen,
                                             Map<String, String> params) {
        performWorkflowAction(entity, target, task, instance, stage, screen, params, null);
    }

    /**
     * Default workflow execution action with predicate which can filter processing items
     *
     * @param entity    editor screen entity
     * @param target    target actions UI component
     * @param task      editor screen task
     * @param instance  editor screen workflow instance
     * @param stage     browser screen stage
     * @param screen    UI calling frame
     * @param params    processing parameters
     * @param predicate special entities filtering predicate
     */
    public static void performWorkflowAction(@Nullable WorkflowEntity entity,
                                             Object target,
                                             @Nullable WorkflowInstanceTask task,
                                             @Nullable WorkflowInstance instance,
                                             @Nullable Stage stage,
                                             Frame screen,
                                             Map<String, String> params,
                                             @Nullable Predicate<WorkflowEntity> predicate) {
        ((WebUiHelper) AppBeans.get(NAME)).performWorkflowActionInternal(
                entity, target, task, instance, stage, screen, params, predicate);
    }

    /**
     * Default workflow execution action with predicate which can filter processing items
     *
     * @param entity   editor screen entity
     * @param target   target actions UI component
     * @param task     editor screen task
     * @param instance editor screen workflow instance
     * @param stage    browser screen stage
     * @param screen   UI calling frame
     * @param params   processing parameters
     */
    protected void performWorkflowActionInternal(@Nullable WorkflowEntity entity,
                                                 Object target,
                                                 @Nullable WorkflowInstanceTask task,
                                                 @Nullable WorkflowInstance instance,
                                                 @Nullable Stage stage,
                                                 Frame screen,
                                                 Map<String, String> params,
                                                 @Nullable Predicate<WorkflowEntity> predicate) {
        try {
            if (entity != null) { //this is editor
                if (predicate == null || predicate.test(entity)) {
                    commitEditorIfNeed((AbstractEditor) screen);
                    workflowService.finishTask(task, params);
                    ((AbstractEditor) screen).close(Window.COMMIT_ACTION_ID, true);
                }
            } else if (target instanceof Table) { //this is browser screen
                Table<WorkflowEntity> table = (Table) target;
                Set<WorkflowEntity> selected = table.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    commitTableIfNeed(table);
                    try {
                        for (WorkflowEntity item : selected) {
                            if (predicate == null || predicate.test(item)) {
                                WorkflowInstanceTask itemTask = workflowService.getWorkflowInstanceTaskNN(item, stage);
                                workflowService.finishTask(itemTask, params);
                            }
                        }
                    } finally {
                        table.getDatasource().refresh();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(messages.getMainMessage("workflow.processingError"), e);
        }
    }

    /**
     * Cumulative workflow action perming
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.web.util.WebUiHelper;
     * import com.groupstp.workflowstp.web.util.MapHelper;
     * <p>
     * Map params = MapHelper.asMap("one", "true");
     * WebUiHelper.get().performDoubleWorkflowAction(entity, target, workflowInstanceTask, workflowInstance, stage, screen, "ctx_key", params);
     * <p>
     *
     * @param entity   editor screen entity
     * @param target   target actions UI component
     * @param task     editor screen task
     * @param instance editro screen workflow instance
     * @param stage    browser screen stage
     * @param screen   UI calling frame
     * @param key      cumulative context property key
     * @param params   processing parameters
     */
    public static void performDoubleWorkflowAction(@Nullable WorkflowEntity entity,
                                                   Object target,
                                                   @Nullable WorkflowInstanceTask task,
                                                   @Nullable WorkflowInstance instance,
                                                   @Nullable Stage stage,
                                                   Frame screen,
                                                   String key,
                                                   Map<String, String> params) {
        ((WebUiHelper) AppBeans.get(NAME)).performDoubleWorkflowActionInternal(
                entity, target, task, instance, stage, screen, key, params);
    }

    protected void performDoubleWorkflowActionInternal(@Nullable WorkflowEntity entity,
                                                       Object target,
                                                       @Nullable WorkflowInstanceTask task,
                                                       @Nullable WorkflowInstance instance,
                                                       @Nullable Stage stage,
                                                       Frame screen,
                                                       String key,
                                                       Map<String, String> params) {
        try {
            if (instance != null) {//this is editor
                commitEditorIfNeed((AbstractEditor) screen);

                WorkflowExecutionContext ctx = workflowService.getExecutionContext(instance);
                String[] performers = doubleActionPerformed(ctx, key);
                if (performers != null) {
                    for (Map.Entry<String, String> e : params.entrySet()) {
                        ctx.putParam(e.getKey(), e.getValue());
                    }
                    workflowService.finishTask(task, ctx.getParams(), performers);
                } else {
                    workflowService.setExecutionContext(ctx, instance);
                }
                ((AbstractEditor) screen).close(Window.COMMIT_ACTION_ID, true);
            } else if (target instanceof Table) {//this is browser
                Table<WorkflowEntity> table = (Table) target;
                Set<WorkflowEntity> selected = table.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    commitTableIfNeed(table);
                    try {
                        for (WorkflowEntity item : selected) {
                            WorkflowInstance itemInstance = workflowService.getWorkflowInstance(item);
                            WorkflowInstanceTask itemTask = workflowService.getWorkflowInstanceTaskNN(item, stage);
                            WorkflowExecutionContext ctx = workflowService.getExecutionContext(itemInstance);
                            String[] performers = doubleActionPerformed(ctx, key);
                            if (performers != null) {
                                for (Map.Entry<String, String> e : params.entrySet()) {
                                    ctx.putParam(e.getKey(), e.getValue());
                                }
                                workflowService.finishTask(itemTask, ctx.getParams(), performers);
                            } else {
                                workflowService.setExecutionContext(ctx, itemInstance);
                            }
                        }
                    } finally {
                        ((Table) target).getDatasource().refresh();
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(messages.getMainMessage("workflow.processingError"), e);
        }
    }

    private String[] doubleActionPerformed(WorkflowExecutionContext context, String key) {
        String[] performers = null;

        User user = userSessionSource.getUserSession().getUser();
        String value = context.getParam(key);
        if (StringUtils.isEmpty(value)) {
            value = user.getLogin();
        } else {
            performers = new String[]{value, user.getLogin()};
            value = value + "," + user.getLogin();
        }
        context.putParam(key, value);

        return performers;
    }

    /**
     * Checkup of performing double cumulative action
     * <p>
     * Usage:
     * <p>
     * import com.groupstp.workflowstp.web.util.WebUiHelper;
     * <p>
     * return WebUiHelper.isDoubleWorkflowActionPerformable(target, workflowInstance, workflowInstanceTask, stage, "ctx_key");
     * <p>
     *
     * @param target   target actions UI component
     * @param instance workflow instance reference
     * @param task     current performing workflow instance task
     * @param key      cumulative context property key
     * @param stage    processing stage
     * @return answer which should be used in "isPermit" actions method
     */
    public static boolean isDoubleWorkflowActionPerformable(Object target,
                                                     @Nullable WorkflowInstance instance,
                                                     @Nullable WorkflowInstanceTask task,
                                                     @Nullable Stage stage,
                                                     String key) {
        return ((WebUiHelper) AppBeans.get(NAME)).isDoubleWorkflowActionPerformableInternal(target, instance, task, stage, key);
    }

    protected boolean isDoubleWorkflowActionPerformableInternal(Object target,
                                                                @Nullable WorkflowInstance instance,
                                                                @Nullable WorkflowInstanceTask task,
                                                                @Nullable Stage stage,
                                                                String key) {
        if (target instanceof Table) {
            Table<WorkflowEntity> table = (Table) target;
            assert stage != null;
            Set<WorkflowEntity> selected = table.getSelected();
            if (!CollectionUtils.isEmpty(selected)) {
                for (WorkflowEntity item : selected) {
                    WorkflowInstance itemInstance = workflowService.getWorkflowInstance(item);
                    if (itemInstance == null) {
                        return false;
                    }
                    WorkflowExecutionContext ctx = workflowService.getExecutionContext(itemInstance);
                    if (!isDoubleWorkflowActionPerformable(ctx, key, stage)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        } else {
            if (instance == null || task == null || task.getStep() == null || task.getStep().getStage() == null) {
                return false;
            }
            WorkflowExecutionContext ctx = workflowService.getExecutionContext(instance);
            if (!isDoubleWorkflowActionPerformable(ctx, key, task.getStep().getStage())) {
                return false;
            }
            return true;
        }
    }

    protected boolean isDoubleWorkflowActionPerformable(WorkflowExecutionContext context, String key, Stage stage) {
        User user = userSessionSource.getUserSession().getUser();
        if (!isUserSatisfy(user, stage)) {
            return false;
        }
        String value = context.getParam(key);
        if (!StringUtils.isEmpty(value)) {
            String[] acceptedUsers = value.split(",");
            if (acceptedUsers.length > 0) {
                for (String acceptedUser : acceptedUsers) {
                    if (Objects.equals(acceptedUser, user.getLogin())) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check can user perform task on this stage
     *
     * @param user  current user
     * @param stage processing stage
     * @return can current user perform task on this stage
     */
    protected boolean isUserSatisfy(User user, Stage stage) {
        if (!PersistenceHelper.isLoadedWithView(stage, "stage-actors")) {
            stage = dataManager.reload(stage, "stage-actors");
        }
        if (!CollectionUtils.isEmpty(stage.getActors())) {
            return stage.getActors().contains(user);
        }
        if (!CollectionUtils.isEmpty(stage.getActorsRoles())) {
            if (!CollectionUtils.isEmpty(user.getUserRoles())) {
                for (UserRole userRole : user.getUserRoles()) {
                    if (stage.getActorsRoles().contains(userRole.getRole())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    protected void commitEditorIfNeed(AbstractEditor editor) {
        if (editor.isModified()) {
            editor.commit();
        }
    }

    protected void commitTableIfNeed(Table table) {
        if (table.getDatasource().isModified()) {
            table.getDatasource().commit();
        }
    }
}
