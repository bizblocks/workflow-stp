package com.groupstp.workflowstp.web.screenpalette.action;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.util.codedialog.CodeDialog;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsBuilder;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.User;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author adiatullin
 */
public class ScreenActionTemplateEdit extends AbstractEditor<ScreenActionTemplate> {

    @Inject
    protected Metadata metadata;
    @Inject
    protected MessageTools messageTools;
    @Inject
    protected ExtendedEntities extendedEntities;
    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected Scripting scripting;

    @Inject
    protected Datasource<ScreenActionTemplate> screenActionTemplateDs;
    @Inject
    protected LookupField iconField;
    @Inject
    protected LookupField styleField;
    @Inject
    protected Button sampleBtn;
    @Inject
    protected Label sampleBtnLabel;
    @Inject
    protected LookupField entityNameField;
    @Inject
    protected CheckBox permitRequiredChBx;
    @Inject
    protected CheckBox buttonActionChb;
    @Inject
    protected BoxLayout permitBoxDetails;
    @Inject
    protected TabSheet permitScriptTabSheet;
    @Inject
    protected SourceCodeEditor permitScriptEditor;
    @Inject
    protected SourceCodeEditor externalPermitScriptEditor;
    @Inject
    protected BoxLayout permitBox;
    @Inject
    protected Label permitExpandLabel;
    @Inject
    protected BoxLayout permitActionBox;
    @Inject
    private TabSheet scriptTabSheet;
    @Inject
    protected SourceCodeEditor scriptEditor;
    @Inject
    protected SourceCodeEditor externalScriptEditor;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initIconField();
        initStyleField();
        initEntityNameField();
        initPermitRequired();
        initScripts();
        initButtonActionBehaviour();

        screenActionTemplateDs.addItemPropertyChangeListener(e -> {
            if (EqualsUtils.equalAny(e.getProperty(), "style", "icon", "caption", "buttonAction")) {
                repaintSampleBtn();
            }
            if ("availableInExternalSystem".equals(e.getProperty())) {
                checkExternalScripts();
            }
        });
    }

    protected void initIconField() {
        Map<String, Object> options = new TreeMap<>();
        for (CubaIcon icon : CubaIcon.values()) {
            options.put(icon.name(), icon.source());
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

    protected void initEntityNameField() {
        Map<String, Object> options = new TreeMap<>();
        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            if (WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                MetaClass mainMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = mainMetaClass.getName();
                options.put(messageTools.getEntityCaption(metaClass) + " (" + originalName + ")", originalName);
            }
        }
        entityNameField.setOptionsMap(options);
    }

    protected void initPermitRequired() {
        permitRequiredChBx.addValueChangeListener(e -> {
            permitBox.resetExpanded();

            permitBoxDetails.setVisible(permitRequiredChBx.isChecked());
            permitScriptTabSheet.setVisible(permitRequiredChBx.isChecked());
            permitActionBox.setVisible(permitRequiredChBx.isChecked());
            permitExpandLabel.setVisible(!permitRequiredChBx.isChecked());

            permitBox.expand(permitRequiredChBx.isChecked() ? permitScriptTabSheet : permitExpandLabel);
        });
    }

    protected void initScripts() {
        scriptEditor.setContextHelpIconClickHandler(e -> getScriptHint());
        externalScriptEditor.setContextHelpIconClickHandler(e -> getExternalScriptHint());
        permitScriptEditor.setContextHelpIconClickHandler(e -> getPermitScriptHint());
        externalPermitScriptEditor.setContextHelpIconClickHandler(e -> getExternalPermitScriptHint());
    }

    protected void initButtonActionBehaviour() {
        buttonActionChb.addValueChangeListener(e -> {
            styleField.setEnabled(buttonActionChb.isChecked());
            if (!buttonActionChb.isChecked()) {
                getItem().setStyle(null);
            }
        });
        styleField.setEnabled(false);
    }

    @Override
    public void initNewItem(ScreenActionTemplate item) {
        super.initNewItem(item);

        item.setPermitItemsType(ComparingType.EQUALS);
    }

    @Override
    public void postInit() {
        super.postInit();

        repaintSampleBtn();
        checkExternalScripts();
    }

    protected void repaintSampleBtn() {
        if (Boolean.TRUE.equals(getItem().getButtonAction())) {
            sampleBtn.setVisible(true);
            sampleBtnLabel.setVisible(true);
            sampleBtn.setIcon(getItem().getIcon());
            sampleBtn.setStyleName(getItem().getStyle());
            sampleBtn.setCaption(getItem().getCaption());
        } else {
            sampleBtn.setVisible(false);
            sampleBtnLabel.setVisible(false);
        }
    }

    protected void checkExternalScripts() {
        boolean externalAvailable = Boolean.TRUE.equals(getItem().getAvailableInExternalSystem());

        scriptTabSheet.getTab("externalScriptTab").setVisible(externalAvailable);
        permitScriptTabSheet.getTab("externalPermitScriptTab").setVisible(externalAvailable);
    }

    public void editScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setScript(dialog.getCode()));
    }

    public void editExternalScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getExternalScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setExternalScript(dialog.getCode()));
    }

    public void editPermitScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getPermitScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setPermitScript(dialog.getCode()));
    }

    public void editExternalPermitScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getExternalPermitScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setExternalPermitScript(dialog.getCode()));
    }

    public void testScript() {
        test(getItem().getScript());
    }

    public void testExternalScript() {
        testExternal(getItem().getExternalScript());
    }

    public void testPermitScript() {
        test(getItem().getPermitScript());
    }

    public void testExternalPermitScript() {
        testExternal(getItem().getExternalPermitScript());
    }

    protected void test(String script) {
        if (!StringUtils.isEmpty(script)) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(WorkflowWebBean.STAGE, metadata.create(Stage.class));
                params.put(WorkflowWebBean.CONTEXT, new HashMap<>());
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE, metadata.create(WorkflowInstance.class));
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE_TASK, metadata.create(WorkflowInstanceTask.class));
                params.put(WorkflowWebBean.SCREEN, this);
                params.put(WorkflowWebBean.VIEW_ONLY, Boolean.FALSE);
                Table table = componentsFactory.createComponent(Table.class);
                table.setDatasource(DsBuilder.create().setJavaClass(User.class).buildCollectionDatasource());
                params.put(WorkflowWebBean.TARGET, table);
                params.put(WorkflowWebBean.ENTITY, metadata.create(User.class));

                scripting.evaluateGroovy(script, params);
            } catch (CompilationFailedException e) {
                showMessageDialog(getMessage("action.script.error"),
                        String.format(messages.getMainMessage("action.script.compilationError"), e.toString()), MessageType.WARNING_HTML);
                return;
            } catch (Exception ignore) {
            }
            showNotification(messages.getMainMessage("action.script.success"));
        }
    }

    protected void testExternal(String script) {
        if (!StringUtils.isEmpty(script)) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put(WorkflowWebBean.STAGE, metadata.create(Stage.class));
                params.put(WorkflowWebBean.CONTEXT, new HashMap<>());
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE, metadata.create(WorkflowInstance.class));
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE_TASK, metadata.create(WorkflowInstanceTask.class));
                params.put(WorkflowWebBean.VIEW_ONLY, Boolean.FALSE);
                params.put(WorkflowWebBean.ENTITY, metadata.create(User.class));

                scripting.evaluateGroovy(script, params);
            } catch (CompilationFailedException e) {
                showMessageDialog(getMessage("action.script.error"),
                        String.format(messages.getMainMessage("action.script.compilationError"), e.toString()), MessageType.WARNING_HTML);
                return;
            } catch (Exception ignore) {
            }
            showNotification(messages.getMainMessage("action.script.success"));
        }
    }

    protected void getScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.scriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    protected void getExternalScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.externalScriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    protected void getPermitScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.permitScriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    protected void getExternalPermitScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.externalPermitScriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            ScreenActionTemplate item = getItem();

            if (Boolean.TRUE.equals(item.getAvailableInExternalSystem())) {
                if (StringUtils.isEmpty(item.getExternalScript())) {
                    scriptTabSheet.setSelectedTab("externalScriptTab");

                    showNotification(getMessage("screenActionTemplateEdit.pleaseSetupExternalScript"), NotificationType.WARNING);
                    return false;
                }
            }

            cleanupScriptsIfNeed(item);
            cleanupPermitSettingsIfNeed(item);

            return true;
        }
        return false;
    }

    protected void cleanupScriptsIfNeed(ScreenActionTemplate item) {
        if (!Boolean.TRUE.equals(item.getAvailableInExternalSystem())) {
            item.setExternalScript(null);
            item.setExternalPermitScript(null);
        }
    }

    protected void cleanupPermitSettingsIfNeed(ScreenActionTemplate item) {
        if (!Boolean.TRUE.equals(item.getPermitRequired())) {
            item.setPermitItemsCount(null);
            item.setPermitScript(null);
            item.setExternalPermitScript(null);
        }
    }
}