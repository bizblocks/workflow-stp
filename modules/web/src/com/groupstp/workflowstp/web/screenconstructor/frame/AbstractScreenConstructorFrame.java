package com.groupstp.workflowstp.web.screenconstructor.frame;

import com.groupstp.workflowstp.entity.ScreenConstructor;
import com.groupstp.workflowstp.entity.Stage;
import com.groupstp.workflowstp.entity.WorkflowInstance;
import com.groupstp.workflowstp.entity.WorkflowInstanceTask;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.util.codedialog.CodeDialog;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Scripting;
import com.haulmont.cuba.gui.components.AbstractFrame;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.data.DsBuilder;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base frame which using in Screen Constructor Editor
 *
 * @author adiatullin
 */
public abstract class AbstractScreenConstructorFrame extends AbstractFrame {

    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected Metadata metadata;
    @Inject
    protected Scripting scripting;

    protected MetaClass entityMetaClass;
    protected ScreenConstructor screenConstructor;
    protected ScreenConstructor genericScreenConstructor;
    protected Window extendingWindow;

    protected boolean constructGeneric;

    /**
     * Setup editing entity meta class
     *
     * @param entityMetaClass editing entity meta class
     */
    public void setEntityMetaClass(MetaClass entityMetaClass) {
        this.entityMetaClass = entityMetaClass;
    }

    /**
     * Setup editing entity
     *
     * @param screenConstructor editing constructor entity
     */
    public void setScreenConstructor(ScreenConstructor screenConstructor) {
        this.screenConstructor = screenConstructor;
    }

    /**
     * Setup external generic screen constructor
     *
     * @param screenConstructor generic screen constructor
     */
    public void setGenericScreenConstructor(@Nullable ScreenConstructor screenConstructor) {
        this.genericScreenConstructor = genericScreenConstructor;
    }

    /**
     * Editing UI screen window
     *
     * @param extendingWindow UI screen window
     */
    public void setExtendingWindow(Window extendingWindow) {
        this.extendingWindow = extendingWindow;
    }

    /**
     * Setup flag what current frame opened for construction generic stuff
     *
     * @param constructGeneric is construct generic screen
     */
    public void setConstructGeneric(boolean constructGeneric) {
        this.constructGeneric = constructGeneric;
    }

    /**
     * Do useful stuff before screen constructor will be saved
     */
    public void prepare() {
    }

    /**
     * Edit groovy code in separate dialog
     *
     * @param item     entity which contains groovy script property
     * @param property groovy script property
     */
    protected void editGroovyInDialog(Entity item, String property) {
        CodeDialog dialog = CodeDialog.show(this, item.getValue(property), "groovy");
        dialog.addCloseWithCommitListener(() -> item.setValue(property, dialog.getCode()));
    }

    /**
     * Open up a separate dialog to show some additional information
     *
     * @param caption dialog caption
     * @param content dialog content
     */
    protected void createDialog(String caption, String content) {
        showMessageDialog(caption, content, MessageType.CONFIRMATION_HTML.modal(false).width("600px"));
    }

    /**
     * Checkup a groovy script
     *
     * @param script to check
     */
    protected void test(String script) {
        test(script, Boolean.TRUE.equals(screenConstructor.getIsBrowserScreen()));
    }

    protected void test(String script, boolean isBrowserScreen) {
        if (!StringUtils.isEmpty(script)) {
            Map<String, Object> params = new HashMap<>();
            params.put(WorkflowWebBean.SCREEN, extendingWindow);
            params.put(WorkflowWebBean.STAGE, metadata.create(Stage.class));
            if (isBrowserScreen) {
                Table table = componentsFactory.createComponent(Table.class);
                table.setDatasource(DsBuilder.create().setJavaClass(entityMetaClass.getJavaClass()).buildCollectionDatasource());
                params.put(WorkflowWebBean.TARGET, table);
                params.put(WorkflowWebBean.VIEW_ONLY, Boolean.FALSE);
            } else {
                params.put(WorkflowWebBean.TARGET, componentsFactory.createComponent(HBoxLayout.class));
                params.put(WorkflowWebBean.ENTITY, metadata.create(entityMetaClass));
                params.put(WorkflowWebBean.CONTEXT, new HashMap<>());
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE, metadata.create(WorkflowInstance.class));
                params.put(WorkflowWebBean.WORKFLOW_INSTANCE_TASK, metadata.create(WorkflowInstanceTask.class));
            }

            test(script, params);
        }
    }

    private void test(String script, Map<String, Object> params) {
        try {
            scripting.evaluateGroovy(script, params);
        } catch (CompilationFailedException e) {
            showMessageDialog(getMessage("action.script.error"),
                    String.format(messages.getMainMessage("action.script.compilationError"), e.toString()), MessageType.WARNING_HTML);
            return;
        } catch (Exception ignore) {
        }
        showNotification(messages.getMainMessage("action.script.success"));
    }

    protected void sortTable(Table table, String orderProperty) {
        table.sort(orderProperty, Table.SortDirection.ASCENDING);
    }

    protected void correctOrderIfNeed(Table table, String orderProperty) {
        //noinspection unchecked
        Collection<Entity> items = table.getDatasource().getItems();
        if (!CollectionUtils.isEmpty(items)) {
            int i = 1;
            for (Entity item : items) {
                Integer order = item.getValue(orderProperty);
                if (!Objects.equals(i, order)) {
                    item.setValue(orderProperty, i);
                }
                i += 1;
            }
        }
    }
}
