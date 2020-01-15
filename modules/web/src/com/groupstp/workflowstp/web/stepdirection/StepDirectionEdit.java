package com.groupstp.workflowstp.web.stepdirection;

import com.groupstp.workflowstp.entity.Step;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.filter.SecurityJpqlGenerator;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.*;
import com.groupstp.workflowstp.entity.StepDirection;
import com.haulmont.cuba.gui.components.autocomplete.JpqlSuggestionFactory;
import com.haulmont.cuba.gui.components.autocomplete.Suggestion;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FakeFilterSupport;
import com.haulmont.cuba.gui.components.filter.FilterParser;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.edit.FilterEditor;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.impl.DatasourceImpl;
import com.haulmont.cuba.security.entity.FilterEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Element;

import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class StepDirectionEdit extends AbstractEditor<StepDirection> {
    public static final String ORDER = "order";
    public static final String FROM_STEP = "from_step";
    public static final String POSSIBLE_TO_STEPS = "possible_to_steps";

    private static final String GROOVY_TAB_ID = "groovyTab";
    private static final String SQL_TAB_ID = "sqlTab";

    @Inject
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private ExtendedEntities extendedEntities;
    @Inject
    private WindowManagerProvider windowManagerProvider;
    @Inject
    private WindowConfig windowConfig;

    @Inject
    private DatasourceImpl<StepDirection> stepDirectionDs;
    @Inject
    private TextField entityName;
    @Inject
    private CollectionDatasource<Step, UUID> possibleToStepsDs;
    @Inject
    private SourceCodeEditor sqlClause;
    @Inject
    private SourceCodeEditor groovyClause;
    @Inject
    private TabSheet constraintTabSheet;

    @WindowParam(name = ORDER)
    private Integer order;

    @WindowParam(name = FROM_STEP, required = true)
    private Step fromStep;

    @WindowParam(name = POSSIBLE_TO_STEPS, required = true)
    private Collection<Step> possibleToSteps;

    private MetaClass metaclass;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initGeneralPart();
        initSqlConstraint();
        initGroovyConstraint();
    }

    private void initGeneralPart() {
        //setup possible steps to go
        if (!CollectionUtils.isEmpty(possibleToSteps)) {
            for (Step toStep : possibleToSteps) {
                possibleToStepsDs.includeItem(toStep);
            }
        }
    }

    private void initSqlConstraint() {
        //setup sql constructor for sql condition
        sqlClause.setSuggester((source, text, cursorPosition) -> requestHint(sqlClause, text, cursorPosition));
        sqlClause.setContextHelpIconClickHandler(event ->
                showMessageDialog(messages.getMessage(StepDirectionEdit.class, "stepDirectionEdit.sqlClause"),
                        messages.getMessage(StepDirectionEdit.class, "stepDirectionEdit.sqlClauseHelp"),
                        MessageType.CONFIRMATION_HTML
                                .modal(false)
                                .width("600px")));
    }

    //provide suggestions for sql condition
    private List<Suggestion> requestHint(SourceCodeEditor sender, String text, int cursorPosition) {
        if (entityName.getValue() != null) {
            String whereStr = sqlClause.getValue();

            // the magic entity name!  The length is three character to match "{E}" length in query
            String entityNameAlias = "a39";

            int position = 0;

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("select ");
            queryBuilder.append(entityNameAlias);
            queryBuilder.append(" from ");
            queryBuilder.append(metaclass.getName());
            queryBuilder.append(" ");
            queryBuilder.append(entityNameAlias);
            queryBuilder.append(" ");

            if (StringUtils.isNotEmpty(whereStr)) {
                if (sender == sqlClause) {
                    position = queryBuilder.length() + " WHERE ".length() + cursorPosition - 1;
                }
                queryBuilder.append(" WHERE ").append(whereStr);
            }

            String query = queryBuilder.toString();
            query = query.replace("{E}", entityNameAlias);

            List<Suggestion> suggestions = JpqlSuggestionFactory.requestHint(query, position, sender.getAutoCompleteSupport(), cursorPosition);
            addSpecificSuggestions(sender, text, cursorPosition, suggestions);
            return suggestions;
        }
        return Collections.emptyList();
    }

    //same as cuba constraint
    private void addSpecificSuggestions(SourceCodeEditor sender, String text, int cursorPosition, List<Suggestion> suggestions) {
        if (cursorPosition <= 0)
            return;
        int colonIdx = text.substring(0, cursorPosition).lastIndexOf(":");
        if (colonIdx < 0)
            return;

        List<String> values = new ArrayList<>();
        values.add("session$userGroupId");
        values.add("session$userId");
        values.add("session$userLogin");

        String entered = text.substring(colonIdx + 1, cursorPosition);
        for (String value : values) {
            if (value.startsWith(entered)) {
                suggestions.add(new Suggestion(sender.getAutoCompleteSupport(), value, value.substring(entered.length()),
                        StringUtils.EMPTY, cursorPosition, cursorPosition));
            }
        }
    }

    @Override
    public void postInit() {
        StepDirection item = getItem();

        if (PersistenceHelper.isNew(item)) {//setup base fields
            item.setFrom(fromStep);
            item.setOrder(order == null ? 1 : order);

            stepDirectionDs.setModified(false);
        }

        metaclass = extendedEntities.getOriginalOrThisMetaClass(
                metadata.getClassNN(item.getFrom().getWorkflow().getEntityName()));
        entityName.setValue(messageTools.getEntityCaption(metaclass) + " (" + metaclass.getName() + ")");

        boolean containsGroovy = !StringUtils.isEmpty(item.getConditionGroovyScript());
        boolean containsSql = !StringUtils.isEmpty(item.getConditionSqlScript());

        if (containsSql && containsGroovy) {//specified both - show groovy - same behaviour from worker bean
            constraintTabSheet.setSelectedTab(GROOVY_TAB_ID);
        } else if (containsGroovy) {
            constraintTabSheet.setSelectedTab(GROOVY_TAB_ID);
        } else {
            constraintTabSheet.setSelectedTab(SQL_TAB_ID);
        }
    }

    public void openSqlConstructor() {
        final StepDirection item = getItem();

        FakeFilterSupport fakeFilterSupport = new FakeFilterSupport(this, metaclass);
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

    private void initGroovyConstraint() {
        groovyClause.setContextHelpIconClickHandler(event ->
                showMessageDialog(messages.getMessage(StepDirectionEdit.class, "stepDirectionEdit.groovyClause"),
                        messages.getMessage(StepDirectionEdit.class, "stepDirectionEdit.groovyClauseHelp"),
                        MessageType.CONFIRMATION_HTML
                                .modal(false)
                                .width("600px")));
    }
}