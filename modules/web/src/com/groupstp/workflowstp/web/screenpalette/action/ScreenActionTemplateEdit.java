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
import org.apache.commons.lang.StringUtils;
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
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private ExtendedEntities extendedEntities;
    @Inject
    private ComponentsFactory componentsFactory;
    @Inject
    private Scripting scripting;

    @Inject
    private Datasource<ScreenActionTemplate> screenActionTemplateDs;
    @Inject
    private LookupField iconField;
    @Inject
    private LookupField styleField;
    @Inject
    private Button sampleBtn;
    @Inject
    private Label sampleBtnLabel;
    @Inject
    private LookupField entityNameField;
    @Inject
    private CheckBox permitRequiredChBx;
    @Inject
    private LookupField permitItemsTypeField;
    @Inject
    private CheckBox buttonActionChb;
    @Inject
    private TextField permitItemsCountField;
    @Inject
    private SourceCodeEditor permitScriptEditor;
    @Inject
    private BoxLayout permitBox;
    @Inject
    private Label permitExpandLabel;
    @Inject
    private BoxLayout permitActionBox;
    @Inject
    private SourceCodeEditor scriptEditor;

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
        });
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

    private void initEntityNameField() {
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

    private void initPermitRequired() {
        permitRequiredChBx.addValueChangeListener(e -> {
            permitBox.resetExpanded();

            permitItemsCountField.setVisible(permitRequiredChBx.isChecked());
            permitItemsTypeField.setVisible(permitRequiredChBx.isChecked());
            permitScriptEditor.setVisible(permitRequiredChBx.isChecked());
            permitActionBox.setVisible(permitRequiredChBx.isChecked());
            permitExpandLabel.setVisible(!permitRequiredChBx.isChecked());

            permitBox.expand(permitRequiredChBx.isChecked() ? permitScriptEditor : permitExpandLabel);
        });
    }

    private void initScripts() {
        scriptEditor.setContextHelpIconClickHandler(e -> getScriptHint());
        permitScriptEditor.setContextHelpIconClickHandler(e -> getPermitScriptHint());
    }

    private void initButtonActionBehaviour() {
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
    }

    private void repaintSampleBtn() {
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

    public void editScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setScript(dialog.getCode()));
    }

    public void editPermitScript() {
        CodeDialog dialog = CodeDialog.show(this, getItem().getPermitScript(), "groovy");
        dialog.addCloseWithCommitListener(() -> getItem().setPermitScript(dialog.getCode()));
    }

    public void testScript() {
        test(getItem().getScript());
    }

    public void testPermitScript() {
        test(getItem().getPermitScript());
    }

    private void test(String script) {
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

    private void getScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.scriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    private void getPermitScriptHint() {
        showMessageDialog(getMessage("screenActionTemplateEdit.groovyScript"), getMessage("screenActionTemplateEdit.permitScriptHint"),
                MessageType.CONFIRMATION_HTML
                        .modal(false)
                        .width("600px"));
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            ScreenActionTemplate item = getItem();

            cleanupPermitSettingsIfNeed(item);

            return true;
        }
        return false;
    }

    private void cleanupPermitSettingsIfNeed(ScreenActionTemplate item) {
        if (!Boolean.TRUE.equals(item.getPermitRequired())) {
            item.setPermitItemsCount(null);
            item.setPermitScript(null);
        }
    }
}