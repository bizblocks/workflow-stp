package com.groupstp.workflowstp.web.stage;

import com.groupstp.workflowstp.entity.StageType;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.screenconstructor.ScreenConstructorEditor;
import com.groupstp.workflowstp.web.util.codedialog.CodeDialog;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.KeyValueEntity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.groupstp.workflowstp.entity.Stage;
import com.haulmont.cuba.gui.components.actions.AddAction;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.impl.ValueCollectionDatasourceImpl;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;
import com.vaadin.data.HasValue;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author adiatullin
 */
public class StageEdit extends AbstractEditor<Stage> {
    protected static final Logger log = LoggerFactory.getLogger(StageEdit.class);

    @Inject
    protected MessageTools messageTools;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected WorkflowWebBean workflowWebBean;
    @Inject
    protected WorkflowService workflowService;
    @Inject
    protected Metadata metadata;
    @Inject
    protected ComponentsFactory componentsFactory;

    @Inject
    protected Datasource<Stage> stageDs;
    @Inject
    protected FieldGroup generalFieldGroup;
    @Inject
    protected LookupField<String> entityNameField;
    @Inject
    protected LookupField<StageType> typeField;
    @Inject
    protected Component userInteractionBox;
    @Inject
    protected Component executionBox;
    @Inject
    protected LookupField<ActorsType> actorTypeAction;
    @Inject
    protected TokenList<Role> actorRolesList;
    @Inject
    protected TokenList<User> actorUsersList;
    @Inject
    protected LookupField<ActorsType> viewerTypeAction;
    @Inject
    protected TokenList viewerRolesList;
    @Inject
    protected TokenList viewerUsersList;
    @Inject
    protected SourceCodeEditor browseScreenGroovyScript;
    @Inject
    protected SourceCodeEditor editorScreenGroovyScript;
    @Inject
    protected LookupField<String> executionBeanNameField;
    @Inject
    protected SourceCodeEditor executionCode;
    @Inject
    protected BoxLayout scriptBox;
    @Inject
    protected TextArea<String> browserScreenConstructor;
    @Inject
    protected TextArea<String> editorScreenConstructor;
    @Inject
    protected BoxLayout mainBox;
    @Inject
    protected Table<KeyValueEntity> directionVariablesTable;
    @Inject
    protected ValueCollectionDatasourceImpl directionVariablesDs;
    @Inject
    protected TabSheet browseScreenTabSheet;
    @Inject
    protected TabSheet editorScreenTabSheet;

    protected boolean ignoreDirectionVariablesChanges = false;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initTypeSelectionBehaviour();
        initEntityNameBehaviour();
        initActorsSelectionBehaviour();
        initViewerSelectionBehaviour();
        initDirectionVariables();
        initGenericConstruct();
        initExecutionScripts();

        browseScreenGroovyScript.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getBrowseScreenGroovyHint());
        editorScreenGroovyScript.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getEditorScreenGroovyHint());
        executionCode.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getExecutionGroovyHint());
    }

    //Setup behaviour when selected one type some view must be hidden or showed
    protected void initTypeSelectionBehaviour() {
        ((LookupField<StageType>) generalFieldGroup.getFieldNN("type").getComponentNN()).addValueChangeListener(e -> {
            boolean userInteraction = EqualsUtils.equalAny(e.getValue(), StageType.USERS_INTERACTION, StageType.ARCHIVE);
            boolean execution = StageType.ALGORITHM_EXECUTION.equals(e.getValue());

            //pretty view
            Component expanding = null;
            if (userInteraction) {
                expanding = userInteractionBox;
            } else if (execution) {
                expanding = executionBox;
            }
            if (expanding != null) {
                mainBox.expand(expanding);
            }

            userInteractionBox.setVisible(userInteraction);
            executionBox.setVisible(execution);
        });
        mainBox.resetExpanded();
        //hide all
        userInteractionBox.setVisible(false);
        executionBox.setVisible(false);
    }

    //Setup entity name selection. Then name selected it's can't be changed
    protected void initEntityNameBehaviour() {
        Map<String, String> options = new TreeMap<>();
        for (MetaClass metaClass : workflowWebBean.getWorkflowEntities()) {
            options.put(messageTools.getEntityCaption(metaClass) + " (" + metaClass.getName() + ")", metaClass.getName());
        }
        entityNameField.setOptionsMap(options);
        entityNameField.addValueChangeListener(e -> {
            entityNameField.setEditable(e.getValue() == null);
            typeField.setEditable(e.getValue() != null);
        });
        typeField.setEditable(false);
    }

    //Behaviour for selecting users or roles
    protected void initActorsSelectionBehaviour() {
        Map<String, ActorsType> options = new LinkedHashMap<>();
        options.put(getMessage("stageEdit.selectUsers"), ActorsType.USER);
        options.put(getMessage("stageEdit.selectRole"), ActorsType.ROLE);

        actorTypeAction.setOptionsMap(options);
        actorTypeAction.setNullOptionVisible(false);
        actorTypeAction.addValueChangeListener(e -> {
            actorRolesList.setVisible(ActorsType.ROLE.equals(e.getValue()));
            actorUsersList.setVisible(ActorsType.USER.equals(e.getValue()));
        });
        actorTypeAction.setValue(ActorsType.USER);
    }

    protected void initViewerSelectionBehaviour() {
        Map<String, ActorsType> options = new LinkedHashMap<>();
        options.put(getMessage("stageEdit.selectUsers"), ActorsType.USER);
        options.put(getMessage("stageEdit.selectRole"), ActorsType.ROLE);

        viewerTypeAction.setOptionsMap(options);
        viewerTypeAction.setNullOptionVisible(false);
        viewerTypeAction.addValueChangeListener(e -> {
            viewerRolesList.setVisible(ActorsType.ROLE.equals(e.getValue()));
            viewerUsersList.setVisible(ActorsType.USER.equals(e.getValue()));
        });
        viewerTypeAction.setValue(ActorsType.USER);
    }

    protected void initDirectionVariables() {
        directionVariablesTable.addAction(new AddAction(directionVariablesTable) {
            @Override
            public void actionPerform(Component component) {
                KeyValueEntity entity = metadata.create(KeyValueEntity.class);
                entity.setMetaClass(directionVariablesDs.getMetaClass());
                directionVariablesDs.addItem(entity);
            }
        });
        directionVariablesTable.addAction(new RemoveAction(directionVariablesTable) {
            @Override
            public void actionPerform(Component component) {
                Set<KeyValueEntity> selected = directionVariablesTable.getSelected();
                if (!CollectionUtils.isEmpty(selected)) {
                    for (KeyValueEntity entity : selected) {
                        directionVariablesDs.removeItem(entity);
                    }
                }
            }
        });
        directionVariablesTable.addGeneratedColumn("name", entity -> {
            TextField textField = componentsFactory.createComponent(TextField.class);
            textField.setWidth("100%");
            textField.setValue(entity.getValue("name"));
            textField.addValueChangeListener(e -> {
                entity.setValue("name", textField.getValue());
                onDirectionVariablesChanged();
            });
            return textField;
        });
        directionVariablesDs.addCollectionChangeListener(e -> onDirectionVariablesChanged());
    }

    protected void initGenericConstruct() {
        directionVariablesTable.addAction(new AbstractAction("genericConstruct") {
            @Override
            public String getCaption() {
                return getMessage("stageEdit.genericActions");
            }

            @Override
            public String getIcon() {
                return CubaIcon.GEAR.source();
            }

            @Override
            public void actionPerform(Component component) {
                MetaClass metaClass = getMetaClassNN();
                String entityName = metaClass.getName();
                String screenId = workflowWebBean.getWorkflowEntityScreens(metaClass).getBrowserScreenId();
                String genericJson = getItem().getScreenConstructor();

                ScreenConstructorEditor screen = ScreenConstructorEditor.show(StageEdit.this, entityName, screenId, genericJson);
                screen.addCloseWithCommitListener(() -> getItem().setScreenConstructor(screen.getScreenConstructor()));
            }
        });
    }

    protected void initExecutionScripts() {
        executionBeanNameField.setOptionsMap(workflowService.getWorkflowExecutionDelegates());
        HasValue.ValueChangeListener listener = e -> {
            if (!StringUtils.isEmpty(executionCode.getValue())) {
                executionBeanNameField.setValue(null);
            } else if (!StringUtils.isEmpty(executionBeanNameField.getValue())) {
                executionCode.setValue(null);
            }
            executionCode.setEditable(StringUtils.isEmpty(executionBeanNameField.getValue()));
            executionCode.setEnabled(StringUtils.isEmpty(executionBeanNameField.getValue()));
            executionBeanNameField.setEditable(StringUtils.isEmpty(executionCode.getValue()));
            executionBeanNameField.setEnabled(StringUtils.isEmpty(executionCode.getValue()));
            scriptBox.setEnabled(StringUtils.isEmpty(executionBeanNameField.getValue()));
        };
        executionBeanNameField.addValueChangeListener((Consumer<com.haulmont.cuba.gui.components.HasValue.ValueChangeEvent<String>>) listener);
        executionCode.addValueChangeListener((Consumer<com.haulmont.cuba.gui.components.HasValue.ValueChangeEvent<String>>) listener);
    }

    protected void getBrowseScreenGroovyHint() {
        showMessageDialog(getMessage("stageEdit.browseScreenGroovyScript"), getMessage("stageEdit.browseScreenGroovyScriptHelp"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    protected void getEditorScreenGroovyHint() {
        showMessageDialog(getMessage("stageEdit.editScreenGroovyScript"), getMessage("stageEdit.editorScreenGroovyScriptHelp"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    protected void getExecutionGroovyHint() {
        showMessageDialog(getMessage("stageEdit.executionGroovyScript"), getMessage("stageEdit.executionGroovyScriptHelp"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    @Override
    public void postInit() {
        super.postInit();

        if (!PersistenceHelper.isNew(getItem())) {
            entityNameField.setEditable(false);
            generalFieldGroup.getFieldNN("name").setEditable(false);
        }

        if (EqualsUtils.equalAny(getItem().getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {//setup view
            actorTypeAction.setValue(CollectionUtils.isEmpty(getItem().getActorsRoles()) ? ActorsType.USER : ActorsType.ROLE);
            viewerTypeAction.setValue(CollectionUtils.isEmpty(getItem().getViewersRoles()) ? ActorsType.USER : ActorsType.ROLE);
        }

        initConstructors();
        setupDirectionVariables();
        initShowDirectGroovyExtensionScript();
    }

    protected void initConstructors() {
        stageDs.addItemPropertyChangeListener(e -> {
            if ("browserScreenConstructor".equals(e.getProperty())) {
                browserScreenConstructor.setValue(pettyPrint(e.getItem().getBrowserScreenConstructor()));
            } else if ("editorScreenConstructor".equals(e.getProperty())) {
                editorScreenConstructor.setValue(pettyPrint(e.getItem().getEditorScreenConstructor()));
            }
        });
        browserScreenConstructor.setValue(pettyPrint(getItem().getBrowserScreenConstructor()));
        editorScreenConstructor.setValue(pettyPrint(getItem().getEditorScreenConstructor()));
    }

    protected void setupDirectionVariables() {
        ignoreDirectionVariablesChanges = true;
        try {
            directionVariablesDs.clear();

            String variables = getItem().getDirectionVariables();
            if (!StringUtils.isEmpty(variables)) {
                String[] items = variables.split(",");
                for (String item : items) {
                    KeyValueEntity entity = metadata.create(KeyValueEntity.class);
                    entity.setMetaClass(directionVariablesDs.getMetaClass());
                    entity.setValue("name", item);
                    directionVariablesDs.includeItem(entity);
                }
            }
        } finally {
            ignoreDirectionVariablesChanges = false;
        }
    }

    protected void onDirectionVariablesChanged() {
        if (ignoreDirectionVariablesChanges)
            return;
        String value = null;
        Collection<KeyValueEntity> items = directionVariablesDs.getItems();
        if (!CollectionUtils.isEmpty(items)) {
            StringBuilder sb = new StringBuilder();
            for (KeyValueEntity item : items) {
                String itemValue = item.getValue("name");
                if (!StringUtils.isEmpty(itemValue)) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(itemValue);
                }
            }
            value = sb.toString();
        }
        getItem().setDirectionVariables(StringUtils.isEmpty(value) ? null : value);
    }

    @Nullable
    protected String pettyPrint(@Nullable String json) {
        if (!StringUtils.isEmpty(json)) {
            try {
                JSONObject jsObject = new JSONObject(json);
                return jsObject.toString(4);
            } catch (Exception e) {
                log.warn("Failed to print json", e);
            }
        }
        return null;
    }

    protected void initShowDirectGroovyExtensionScript() {
        if (!showDirectGroovyScriptsEditor()) {
            browseScreenTabSheet.removeTab("browserScreenScriptTab");
            editorScreenTabSheet.removeTab("editorScreenScriptTab");
        }
    }

    protected boolean showDirectGroovyScriptsEditor() {
        return !PersistenceHelper.isNew(getItem()) &&
                (!StringUtils.isEmpty(getItem().getBrowseScreenGroovyScript()) || !StringUtils.isEmpty(getItem().getEditorScreenGroovyScript()));
    }

    public void editBrowseScreenGroovy() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getBrowseScreenGroovyScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setBrowseScreenGroovyScript(dialog.getCode()));
    }

    public void editEditorScreenGroovy() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getEditorScreenGroovyScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setEditorScreenGroovyScript(dialog.getCode()));
    }

    public void editExecutionGroovy() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getExecutionGroovyScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setExecutionGroovyScript(dialog.getCode()));
    }

    public void editBrowserScreenConstructor() {
        MetaClass metaClass = getMetaClassNN();
        String entityName = metaClass.getName();
        String screenId = workflowWebBean.getWorkflowEntityScreens(metaClass).getBrowserScreenId();
        String constructorJson = getItem().getBrowserScreenConstructor();
        String genericJson = getItem().getScreenConstructor();

        ScreenConstructorEditor screen = ScreenConstructorEditor.show(this, entityName, screenId, true, constructorJson, genericJson);
        screen.addCloseWithCommitListener(() -> getItem().setBrowserScreenConstructor(screen.getScreenConstructor()));
    }

    public void removeBrowserScreenConstructor() {
        getItem().setBrowserScreenConstructor(null);
    }

    public void editEditorScreenConstructor() {
        MetaClass metaClass = getMetaClassNN();
        String entityName = metaClass.getName();
        String screenId = workflowWebBean.getWorkflowEntityScreens(metaClass).getEditorScreenId();
        String constructorJson = getItem().getEditorScreenConstructor();
        String genericJson = getItem().getScreenConstructor();

        ScreenConstructorEditor screen = ScreenConstructorEditor.show(this, entityName, screenId, false, constructorJson, genericJson);
        screen.addCloseWithCommitListener(() -> getItem().setEditorScreenConstructor(screen.getScreenConstructor()));
    }

    public void removeEditorScreenGroovy() {
        getItem().setEditorScreenConstructor(null);
    }

    protected MetaClass getMetaClassNN() {
        return metadata.getClassNN(getItem().getEntityName());
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            Stage item = getItem();

            if (!isUnique(item)) {
                generalFieldGroup.getFieldNN("name").getComponentNN().requestFocus();
                showNotification(getMessage("stageEdit.sameStepAlreadyExist"), NotificationType.TRAY);
                return false;
            }

            if (!isExecutionCodeSpecified(item)) {
                executionBeanNameField.requestFocus();
                showNotification(getMessage("stageEdit.setExecutionLogic"), NotificationType.TRAY);
                return false;
            }

            if (StageType.ALGORITHM_EXECUTION.equals(item.getType())) {//cleanup user interaction fields
                item.setActorsRoles(null);
                item.setActors(null);
                item.setViewersRoles(null);
                item.setViewers(null);
                item.setBrowseScreenGroovyScript(null);
                item.setBrowserScreenConstructor(null);
                item.setEditorScreenGroovyScript(null);
                item.setEditorScreenConstructor(null);
            }
            if (EqualsUtils.equalAny(item.getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
                if (ActorsType.USER.equals(actorTypeAction.getValue())) {
                    if (!isUsersSelected(item)) {
                        actorUsersList.requestFocus();
                        showNotification(getMessage("stageEdit.usersEmpty"));
                        return false;
                    }
                    item.setActorsRoles(null);
                }
                if (ActorsType.ROLE.equals(actorTypeAction.getValue())) {
                    if (!isRolesSelected(item)) {
                        actorRolesList.requestFocus();
                        showNotification(getMessage("stageEdit.roleEmpty"));
                        return false;
                    }
                    item.setActors(null);
                }
                item.setExecutionGroovyScript(null);
                item.setExecutionBeanName(null);

                //cleanup viewers
                if (ActorsType.USER.equals(viewerTypeAction.getValue())) {//selected users only
                    item.setViewersRoles(null);
                } else {//selected roles
                    item.setViewers(null);
                }
            }

            return true;
        }
        return false;
    }

    protected boolean isUnique(Stage item) {
        List same = dataManager.loadList(LoadContext.create(Stage.class)
                .setQuery(new LoadContext.Query("select e from wfstp$Stage e where " +
                        "e.name = :name and e.entityName = :entityName and e.id <> :id")
                        .setParameter("name", item.getName())
                        .setParameter("entityName", item.getEntityName())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }

    protected boolean isExecutionCodeSpecified(Stage item) {
        if (StageType.ALGORITHM_EXECUTION.equals(item.getType())) {
            return !StringUtils.isEmpty(item.getExecutionBeanName()) ||
                    !StringUtils.isEmpty(item.getExecutionGroovyScript());
        }
        return true;
    }

    protected boolean isUsersSelected(Stage item) {
        return !CollectionUtils.isEmpty(item.getActors());
    }

    protected boolean isRolesSelected(Stage item) {
        return !CollectionUtils.isEmpty(item.getActorsRoles());
    }

    protected enum ActorsType {
        USER, ROLE
    }
}