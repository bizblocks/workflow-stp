package com.groupstp.workflowstp.web.screenpalette.screen;

import com.groupstp.workflowstp.entity.ScreenExtensionTemplate;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.screenconstructor.ScreenConstructorEditor;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class ScreenExtensionTemplateEdit extends AbstractEditor<ScreenExtensionTemplate> {
    private static final Logger log = LoggerFactory.getLogger(ScreenExtensionTemplateEdit.class);

    @Inject
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private WorkflowWebBean workflowWebBean;
    @Inject
    private DataManager dataManager;

    @Inject
    private LookupField<String> entityNameField;
    @Inject
    private LookupField<String> screenIdField;
    @Inject
    private BoxLayout constructorBox;
    @Inject
    private FieldGroup fieldGroup;
    @Inject
    private TextArea<String> browserScreenConstructor;

    private MetaClass metaClass;
    private WorkflowWebBean.WorkflowScreenInfo screenInfo;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntityNameField();
        initScreenIdField();
    }

    private void initEntityNameField() {
        Map<String, String> options = new TreeMap<>();
        for (MetaClass metaClass : workflowWebBean.getWorkflowEntities()) {
            options.put(messageTools.getEntityCaption(metaClass) + " (" + metaClass.getName() + ")", metaClass.getName());
        }
        entityNameField.setOptionsMap(options);
        entityNameField.addValueChangeListener(e -> {
            entityNameField.setEditable(e.getValue() == null);
            screenIdField.setEditable(e.getValue() != null);

            metaClass = e.getValue() == null ? null : metadata.getClassNN((String) e.getValue());
            screenInfo = metaClass == null ? null : workflowWebBean.getWorkflowEntityScreens(metaClass);
            screenIdField.setOptionsList(screenInfo == null ?
                    Collections.emptyList() :
                    Arrays.asList(screenInfo.getBrowserScreenId(), screenInfo.getEditorScreenId()));
        });
        entityNameField.setEditable(true);
        screenIdField.setEditable(false);
    }

    private void initScreenIdField() {
        screenIdField.addValueChangeListener(e -> {
            boolean browser = screenInfo == null || Objects.equals(screenInfo.getBrowserScreenId(), getItem().getScreenId());
            getItem().setIsBrowser(browser);
            constructorBox.setEnabled(e.getValue() != null);
        });
        constructorBox.setEnabled(false);
    }

    public void editScreenConstructor() {
        String entityName = metaClass.getName();
        String screenId = getItem().getScreenId();
        String constructorJson = getItem().getScreenConstructor();
        boolean isBrowser = Boolean.TRUE.equals(getItem().getIsBrowser());

        ScreenConstructorEditor screen = ScreenConstructorEditor.show(this, entityName, screenId, isBrowser, constructorJson, null);
        screen.addCloseWithCommitListener(() -> getItem().setScreenConstructor(screen.getScreenConstructor()));
        screen.addCloseWithCommitListener(() -> {
            getItem().setScreenConstructor(screen.getScreenConstructor());
            browserScreenConstructor.setValue(pettyPrint(getItem().getScreenConstructor()));
        });
    }

    public void removeScreenConstructor() {
        getItem().setScreenConstructor(null);
        browserScreenConstructor.setValue(null);
    }

    @Nullable
    private String pettyPrint(@Nullable String json) {
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

    @Override
    public void postInit() {
        super.postInit();

        browserScreenConstructor.setValue(pettyPrint(getItem().getScreenConstructor()));
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            ScreenExtensionTemplate item = getItem();
            if (!isUnique(item)) {
                showNotification(messages.getMainMessage("notification.alert"), getMessage("screenExtensionTemplateEdit.sameTemplateAlreadyExist"), NotificationType.TRAY);
                fieldGroup.getFieldNN("key").getComponentNN().requestFocus();
                return false;
            }
            if (!isScreenConstructorSpecified(item)) {
                showNotification(messages.getMainMessage("notification.alert"), getMessage("screenExtensionTemplateEdit.emptyScreenConstructorScript"), NotificationType.TRAY);
                constructorBox.requestFocus();
                return false;
            }

            return true;
        }
        return false;
    }

    private boolean isUnique(ScreenExtensionTemplate item) {
        List same = dataManager.loadList(LoadContext.create(ScreenExtensionTemplate.class)
                .setQuery(new LoadContext.Query("select e from wfstp$ScreenExtensionTemplate e where e.key = :key and e.id <> :id")
                        .setParameter("key", item.getKey())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }

    private boolean isScreenConstructorSpecified(ScreenExtensionTemplate item) {
        return !StringUtils.isBlank(item.getScreenConstructor());
    }
}