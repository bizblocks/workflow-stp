package com.groupstp.workflowstp.web.util;

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
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.formatters.DateFormatter;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.ConstraintOperationType;
import com.haulmont.cuba.security.entity.EntityAttrAccess;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * This util class which using for construct flexible screens
 *
 * @author adiatullin
 */
@SuppressWarnings("unchecked")
public final class WebUiHelper {

    private static final Element CREATE_TS_ELEMENT = Dom4j.readDocument("<createTs format=\"dd.MM.yyyy\" useUserTimezone=\"true\"/>").getRootElement();
    private static final String DATE_FORMAT = "dd.MM.yyyy";
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT);
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        DECIMAL_FORMAT = new DecimalFormat("#,###.##", formatSymbols);
    }

    private WebUiHelper() {
    }

    /**
     * Show related entity in table as link
     *
     * @param table          master entity table
     * @param entityProperty slave entity property name
     */
    public static void showLinkOnTable(Table table, String entityProperty) {
        showLinkOnTable(table, entityProperty, null);
    }

    /**
     * Show related entity in table as link
     *
     * @param table           master entity table
     * @param entityProperty  slave entity property name
     * @param captionFunction function to generate related entity link caption
     */
    public static void showLinkOnTable(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        ComponentsFactory factory = AppBeans.get(ComponentsFactory.NAME);
        Security security = AppBeans.get(Security.NAME);
        table.addGeneratedColumn(entityProperty, new Table.ColumnGenerator<Entity>() {
            @Override
            public Component generateCell(Entity entity) {
                LinkButton link = factory.createComponent(LinkButton.class);
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
     * @param window user opened window
     */
    public static void hideLookupActionInFields(AbstractWindow window) {
        Security security = AppBeans.get(Security.NAME);
        Collection<Component> components = window.getComponents();
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
    public static void enableComponents(com.haulmont.cuba.gui.components.Component.Container container, List<String> componentsIds) {
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
            }
        });
    }

    /**
     * Setup table columns from specified set
     *
     * @param table   UI table
     * @param columns which properties are should shown
     */
    public static void showColumns(Table table, List<String> columns, Map<String, ColumnGenerator> generators) {
        clearColumns(table);

        showSelectionColumn(table);

        if (generators == null) {
            generators = Collections.emptyMap();
        }

        if (!CollectionUtils.isEmpty(columns)) {
            MessageTools messageTools = AppBeans.get(MessageTools.NAME);
            MetaClass metaClass = table.getDatasource().getMetaClass();

            for (String property : columns) {
                ColumnGenerator custom = generators.get(property);
                if (custom != null && custom.getReadGenerator() != null) {
                    table.addGeneratedColumn(property, custom.getReadGenerator());
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
        clearColumns(table);

        showSelectionColumn(table);

        if (columns == null) {
            columns = Collections.emptyList();
        }
        if (editableColumns == null) {
            editableColumns = Collections.emptyList();
        }
        final Messages messages = AppBeans.get(Messages.NAME);
        final MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        final ComponentsFactory componentsFactory = AppBeans.get(ComponentsFactory.NAME);
        final Security security = AppBeans.get(Security.NAME);
        final DataManager dataManager = AppBeans.get(DataManager.NAME);
        final Metadata metadata = AppBeans.get(Metadata.NAME);

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
                        return getEditableComponent(table, path, entity, componentsFactory, metadata, dataManager);
                    } else {
                        if (generator != null && generator.getReadGenerator() != null) {
                            return generator.getReadGenerator().generateCell(entity);
                        }

                        assert path != null;
                        return getNotEditableComponent(path, entity, componentsFactory, messages);
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

            table.addColumn(column);
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

    private static Component getEditableComponent(Table table, MetaPropertyPath path, Entity entity,
                                                  ComponentsFactory componentsFactory, Metadata metadata, DataManager dataManager) {
        Field result;
        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            result = componentsFactory.createComponent(CheckBox.class);
        } else if (Entity.class.isAssignableFrom(path.getRangeJavaClass())) {
            LookupField field = componentsFactory.createComponent(LookupField.class);
            field.setOptionsList(getItemsList(metadata.getClassNN(path.getRangeJavaClass()), metadata, dataManager));
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

    private static Component getNotEditableComponent(MetaPropertyPath path, Entity entity,
                                                     ComponentsFactory componentsFactory, Messages messages) {
        final Object value = entity.getValue(path.getMetaProperty().getName());

        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            CheckBox checkBox = componentsFactory.createComponent(CheckBox.class);
            checkBox.setEditable(false);
            checkBox.setValue(value);
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

    public static List getItemsList(MetaClass metaClass, Metadata metadata, DataManager dataManager) {
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

    private static void clearColumns(Table table) {
        List<Table.Column> columns = table.getColumns();
        if (!CollectionUtils.isEmpty(columns)) {
            columns = new ArrayList<>(columns);
            for (Table.Column column : columns) {
                table.removeColumn(column);
            }
        }
    }

    private static void showSelectionColumn(Table table) {
        if (isShowSelection()) {
            if (!(table instanceof ExternalSelectionGroupTable)) {
                throw new UnsupportedOperationException("Table not support external selections");
            }

            ExternalSelectionGroupTable sTable = (ExternalSelectionGroupTable) table;
            sTable.setExternalSelectionEnabled(true);

            final Map<Entity, CheckBox> cache = new HashMap<>();
            final ComponentsFactory componentsFactory = AppBeans.get(ComponentsFactory.NAME);
            final Messages messages = AppBeans.get(Messages.NAME);

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

    private static boolean isShowSelection() {
        WorkflowWebConfig config = ((Configuration) AppBeans.get(Configuration.NAME)).getConfig(WorkflowWebConfig.class);
        return Boolean.TRUE.equals(config.getShowSelection());
    }

    /**
     * Correct CreateTS column in table to show it without the time part
     *
     * @param table UI table with CreateTS column
     */
    public static void createTsWithoutTime(Table table) {
        Table.Column column = table.getColumn("createTs");
        if (column != null) {
            column.setFormatter(new DateFormatter(CREATE_TS_ELEMENT));
        }
    }
}
