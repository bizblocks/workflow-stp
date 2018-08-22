package com.groupstp.workflowstp.web.stage;

import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.StageType;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import org.apache.commons.collections4.CollectionUtils;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author adiatullin
 */
public class StageEdit extends AbstractEditor<Stage> {
    @Inject
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private ExtendedEntities extendedEntities;
    @Inject
    private DataManager dataManager;

    @Inject
    private FieldGroup generalFieldGroup;
    @Inject
    private LookupField entityNameField;
    @Inject
    private Component userInteractionBox;
    @Inject
    private Component executionBox;
    @Inject
    private LookupField userTypeAction;
    @Inject
    private TokenList rolesList;
    @Inject
    private TokenList usersList;
    @Inject
    private SourceCodeEditor browseScreenGroovyScript;
    @Inject
    private SourceCodeEditor editorScreenGroovyScript;
    @Inject
    private SourceCodeEditor executionCode;
    @Inject
    private BoxLayout mainBox;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initTypeSelectionBehaviour();
        initEntityNameBehaviour();
        initActorsSelectionBehaviour();

        browseScreenGroovyScript.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getBrowseScreenGroovyHint());
        editorScreenGroovyScript.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getEditorScreenGroovyHint());
        executionCode.setContextHelpIconClickHandler(contextHelpIconClickEvent -> getExecutionGroovyHint());
    }

    //Setup behaviour when selected one type some view must be hidden or showed
    private void initTypeSelectionBehaviour() {
        ((LookupField) generalFieldGroup.getFieldNN("type").getComponentNN()).addValueChangeListener(e -> {
            boolean userInteraction = StageType.USERS_INTERACTION.equals(e.getValue());
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
            editorScreenGroovyScript.setRequired(userInteraction);
            executionBox.setVisible(execution);
            executionCode.setRequired(execution);
        });
        mainBox.resetExpanded();
        //hide all
        userInteractionBox.setVisible(false);
        executionBox.setVisible(false);
    }

    //Setup entity name selection. Then name selected it's can't be changed
    private void initEntityNameBehaviour() {
        Map<String, Object> options = new TreeMap<>();
        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            if (WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                MetaClass mainMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = mainMetaClass.getName();
                options.put(messageTools.getEntityCaption(metaClass) + " (" + originalName + ")", originalName);
            }
        }
        entityNameField.setOptionsMap(options);
        entityNameField.addValueChangeListener(e -> {
            entityNameField.setEditable(e.getValue() == null);
        });
    }

    //Behaviour for selecting users or roles
    private void initActorsSelectionBehaviour() {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put(getMessage("stageEdit.selectUsers"), ActorsType.USER);
        options.put(getMessage("stageEdit.selectRole"), ActorsType.ROLE);

        userTypeAction.setOptionsMap(options);
        userTypeAction.setNullOptionVisible(false);
        userTypeAction.addValueChangeListener(e -> {
            rolesList.setVisible(ActorsType.ROLE.equals(e.getValue()));
            usersList.setVisible(ActorsType.USER.equals(e.getValue()));
        });
        userTypeAction.setValue(ActorsType.USER);
    }

    private void getBrowseScreenGroovyHint() {
        showMessageDialog(getMessage("stageEdit.browseScreenGroovyScript"), getMessage("stageEdit.browseScreenGroovyScriptHelp"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    private void getEditorScreenGroovyHint() {
        showMessageDialog(getMessage("stageEdit.editScreenGroovyScript"), getMessage("stageEdit.editorScreenGroovyScriptHelp"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    private void getExecutionGroovyHint() {
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

        if (StageType.USERS_INTERACTION.equals(getItem().getType())) {//setup view
            userTypeAction.setValue(CollectionUtils.isEmpty(getItem().getActorsRoles()) ? ActorsType.USER : ActorsType.ROLE);
        }
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            Stage item = getItem();

            if (!isUnique(item)) {
                generalFieldGroup.getFieldNN("name").getComponentNN().requestFocus();
                showNotification(getMessage("stageEdit.sameStepAlreadyExist"));
                return false;
            }

            if (StageType.ALGORITHM_EXECUTION.equals(item.getType())) {//cleanup user interaction fields
                item.setActorsRoles(null);
                item.setActors(null);
                item.setBrowseScreenGroovyScript(null);
                item.setEditorScreenGroovyScript(null);
            }
            if (StageType.USERS_INTERACTION.equals(item.getType())) {
                if (ActorsType.USER.equals(userTypeAction.getValue())) {
                    if (!isUsersSelected(item)) {
                        usersList.requestFocus();
                        showNotification(getMessage("stageEdit.usersEmpty"));
                        return false;
                    }
                    item.setActorsRoles(null);
                }
                if (ActorsType.ROLE.equals(userTypeAction.getValue())) {
                    if (!isRolesSelected(item)) {
                        rolesList.requestFocus();
                        showNotification(getMessage("stageEdit.roleEmpty"));
                        return false;
                    }
                    item.setActors(null);
                }
                item.setExecutionGroovyScript(null);
            }

            return true;
        }
        return false;
    }

    private boolean isUnique(Stage item) {
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

    private boolean isUsersSelected(Stage item) {
        return !CollectionUtils.isEmpty(item.getActors());
    }

    private boolean isRolesSelected(Stage item) {
        return !CollectionUtils.isEmpty(item.getActorsRoles());
    }

    private enum ActorsType {
        USER, ROLE;
    }
}