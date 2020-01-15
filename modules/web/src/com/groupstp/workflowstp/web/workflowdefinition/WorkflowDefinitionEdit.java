package com.groupstp.workflowstp.web.workflowdefinition;

import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowDefinition;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.filter.SecurityJpqlGenerator;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.autocomplete.JpqlSuggestionFactory;
import com.haulmont.cuba.gui.components.autocomplete.Suggestion;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FakeFilterSupport;
import com.haulmont.cuba.gui.components.filter.FilterParser;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.edit.FilterEditor;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.security.entity.FilterEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.control.CompilationFailedException;
import org.dom4j.Element;

import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class WorkflowDefinitionEdit extends AbstractEditor<WorkflowDefinition> {

    private static final String GROOVY_TAB_ID = "groovyTab";
    private static final String SQL_TAB_ID = "sqlTab";

    @Inject
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private DataManager dataManager;
    @Inject
    private ExtendedEntities extendedEntities;
    @Inject
    private WindowManagerProvider windowManagerProvider;
    @Inject
    private WindowConfig windowConfig;
    @Inject
    private Scripting scripting;

    @Inject
    private LookupField<String> entityNameField;
    @Inject
    private TabSheet conditionTabSheet;
    @Inject
    private SourceCodeEditor sqlCondition;
    @Inject
    private SourceCodeEditor groovyClause;
    @Inject
    private CollectionDatasource<Workflow, UUID> workflowsDs;

    private MetaClass metaClass;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntityNameField();
        initSqlConstraint();
        initGroovyConstraint();
    }

    private void initEntityNameField() {
        //fill the entity names who extend WorkflowEntity
        Map<String, String> options = new TreeMap<>();
        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            if (WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                MetaClass mainMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = mainMetaClass.getName();
                options.put(messageTools.getEntityCaption(metaClass) + " (" + originalName + ")", originalName);
            }
        }
        entityNameField.setOptionsMap(options);
        entityNameField.addValueChangeListener(e -> {
            //disable/enable condition editors
            String entityName = (String) e.getValue();
            entityNameField.setEditable(entityName == null);
            conditionTabSheet.setVisible(entityName != null);
            metaClass = entityName == null ? null : metadata.getClassNN(entityName);
            initWorkflowField();
        });
        conditionTabSheet.setVisible(false);
    }

    private void initWorkflowField() {
        workflowsDs.clear();
        if (metaClass != null) {
            List<Workflow> list = dataManager.load(Workflow.class)
                    .query("select e from wfstp$Workflow e where e.active = true and e.entityName = :entityName order by e.order")
                    .parameter("entityName", metaClass.getName())
                    .view(View.MINIMAL)
                    .list();
            if (!CollectionUtils.isEmpty(list)) {
                for (Workflow item : list) {
                    workflowsDs.includeItem(item);
                }
            }
        }
    }

    private void initSqlConstraint() {
        sqlCondition.setSuggester((source, text, cursorPosition) -> requestHint(sqlCondition, text, cursorPosition));
        sqlCondition.setContextHelpIconClickHandler(event ->
                showMessageDialog(messages.getMessage(WorkflowDefinition.class, "WorkflowDefinition.conditionSqlScript"),
                        messages.getMessage(WorkflowDefinitionEdit.class, "workflowDefinitionEdit.sqlHelp"),
                        MessageType.CONFIRMATION_HTML
                                .modal(false)
                                .width("600px")));
    }

    private List<Suggestion> requestHint(SourceCodeEditor sender, String text, int cursorPosition) {
        if (metaClass != null) {
            String whereStr = sqlCondition.getValue();

            // the magic entity name!  The length is three character to match "{E}" length in query
            String entityNameAlias = "a39";

            int position = 0;

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("select ");
            queryBuilder.append(entityNameAlias);
            queryBuilder.append(" from ");
            queryBuilder.append(metaClass.getName());
            queryBuilder.append(" ");
            queryBuilder.append(entityNameAlias);
            queryBuilder.append(" ");

            if (StringUtils.isNotEmpty(whereStr)) {
                if (sender == sqlCondition) {
                    position = queryBuilder.length() + " WHERE ".length() + cursorPosition - 1;
                }
                queryBuilder.append(" WHERE ").append(whereStr);
            }

            String query = queryBuilder.toString();
            query = query.replace("{E}", entityNameAlias);

            return JpqlSuggestionFactory.requestHint(query, position, sender.getAutoCompleteSupport(), cursorPosition);
        }
        return Collections.emptyList();
    }

    private void initGroovyConstraint() {
        groovyClause.setContextHelpIconClickHandler(event ->
                showMessageDialog(messages.getMessage(WorkflowDefinition.class, "WorkflowDefinition.conditionGroovyScript"),
                        messages.getMessage(WorkflowDefinitionEdit.class, "workflowDefinitionEdit.groovyHelp"),
                        MessageType.CONFIRMATION_HTML
                                .modal(false)
                                .width("600px")));
    }

    @Override
    public void postInit() {
        super.postInit();

        WorkflowDefinition item = getItem();
        if (!StringUtils.isEmpty(item.getConditionGroovyScript())) {
            conditionTabSheet.setSelectedTab(GROOVY_TAB_ID);
        } else {
            conditionTabSheet.setSelectedTab(SQL_TAB_ID);
        }
    }

    public void openSqlConstructor() {
        final WorkflowDefinition item = getItem();

        FakeFilterSupport fakeFilterSupport = new FakeFilterSupport(this, metaClass);
        final Filter fakeFilter = fakeFilterSupport.createFakeFilter();
        final FilterEntity filterEntity = fakeFilterSupport.createFakeFilterEntity(item.getConditionXml());
        final ConditionsTree conditionsTree = fakeFilterSupport.createFakeConditionsTree(fakeFilter, filterEntity);

        Map<String, Object> params = new HashMap<>();
        params.put("filter", fakeFilter);
        params.put("filterEntity", filterEntity);
        params.put("conditions", conditionsTree);
        params.put("useShortConditionForm", true);

        FilterEditor filterEditor = (FilterEditor) windowManagerProvider.get().
                openWindow(windowConfig.getWindowInfo("filterEditor"), WindowManager.OpenType.DIALOG, params);
        filterEditor.addCloseListener(actionId -> {
            if (COMMIT_ACTION_ID.equals(actionId)) {
                FilterParser filterParser = AppBeans.get(FilterParser.class);
                filterEntity.setXml(filterParser.getXml(filterEditor.getConditions(), Param.ValueProperty.DEFAULT_VALUE));
                if (filterEntity.getXml() != null) {
                    Element element = Dom4j.readDocument(filterEntity.getXml()).getRootElement();
                    com.haulmont.cuba.core.global.filter.FilterParser filterParserObject = new com.haulmont.cuba.core.global.filter.FilterParser(element);

                    String jpql = new SecurityJpqlGenerator().generateJpql(filterParserObject.getRoot());

                    item.setConditionSqlScript(jpql);
                    item.setConditionXml(filterEntity.getXml());
                }
            }
        });
    }

    public void testGroovy() {
        String script = getItem().getConditionGroovyScript();
        if (!StringUtils.isEmpty(script)) {
            try {
                Map<String, Object> params = new HashMap<>();
                params.put("entity", metadata.create(metaClass));

                scripting.evaluateGroovy(script, params);
            } catch (CompilationFailedException e) {
                showMessageDialog(getMessage("workflowDefinitionEdit.error"),
                        formatMessage("workflowDefinitionEdit.scriptCompilationError", e.toString()), MessageType.WARNING_HTML);
                return;
            } catch (Exception e) {
                // ignore
            }
            showNotification(getMessage("workflowDefinitionEdit.success"));
        }
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            WorkflowDefinition item = getItem();
            if (!isSpecifiedOneCondition(item)) {
                showNotification(getMessage("workflowDefinitionEdit.error.specifiedMoreThanOneCondition"), NotificationType.WARNING);
                conditionTabSheet.setSelectedTab(SQL_TAB_ID);
                sqlCondition.requestFocus();
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isSpecifiedOneCondition(WorkflowDefinition item) {
        if (StringUtils.isEmpty(item.getConditionSqlScript())) {
            return !StringUtils.isEmpty(item.getConditionGroovyScript());
        }
        if (StringUtils.isEmpty(item.getConditionGroovyScript())) {
            return !StringUtils.isEmpty(item.getConditionSqlScript());
        }
        return false;
    }
}