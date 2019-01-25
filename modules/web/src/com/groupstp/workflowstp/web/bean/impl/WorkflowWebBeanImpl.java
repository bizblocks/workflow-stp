package com.groupstp.workflowstp.web.bean.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupstp.workflowstp.dto.WorkflowExecutionContext;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.groupstp.workflowstp.web.config.WorkflowWebConfig;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.entity.UserRole;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang.StringUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Workflow screens extension base implementation
 *
 * @author adiatullin
 */
@Component(WorkflowWebBean.NAME)
public class WorkflowWebBeanImpl implements WorkflowWebBean {
    private static final Logger log = LoggerFactory.getLogger(WorkflowWebBeanImpl.class);

    @Inject
    protected Metadata metadata;
    @Inject
    protected ExtendedEntities extendedEntities;
    @Inject
    protected WindowConfig windowConfig;
    @Inject
    protected Scripting scripting;
    @Inject
    protected WorkflowService service;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected Messages messages;

    @Inject
    protected WorkflowWebConfig webConfig;

    @Override
    public List<MetaClass> getWorkflowEntities() {
        List<MetaClass> result = new ArrayList<>();
        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            if (WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                result.add(extendedEntities.getOriginalOrThisMetaClass(metaClass));
            }
        }
        return result;
    }

    @Override
    public WorkflowScreenInfo getWorkflowEntityScreens(MetaClass metaClass) {
        Preconditions.checkNotNullArgument(metaClass, getMessage("WorkflowWebBeanImpl.metaclassIsEmpty"));

        if (!WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
            throw new RuntimeException(getMessage("WorkflowWebBeanImpl.unsupportableMetaclass"));
        }

        return new WorkflowScreenInfo(metaClass, getBrowseScreenId(metaClass), getEditorScreenId(metaClass));
    }

    protected String getBrowseScreenId(MetaClass metaClass) {
        return windowConfig.getBrowseScreenId(metaClass);
    }

    protected String getEditorScreenId(MetaClass metaClass) {
        return windowConfig.getEditorScreenId(metaClass);
    }

    @Override
    public boolean isActor(User user, Stage stage) {
        if (stage != null && user != null) {
            stage = reloadIfNeed(stage, "stage-with-users");
            if (EqualsUtils.equalAny(stage.getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
                if (!CollectionUtils.isEmpty(stage.getActorsRoles())) {
                    user = reloadIfNeed(user, "user-with-roles");
                    if (!CollectionUtils.isEmpty(user.getUserRoles())) {
                        for (UserRole ur : user.getUserRoles()) {
                            if (stage.getActorsRoles().contains(ur.getRole())) {
                                return true;
                            }
                        }
                    }
                } else if (!CollectionUtils.isEmpty(stage.getActors())) {
                    return stage.getActors().contains(user);
                }
            }
        }
        return false;
    }

    @Override
    public boolean isViewer(User user, Stage stage) {
        if (stage != null && user != null) {
            stage = reloadIfNeed(stage, "stage-with-users");
            if (EqualsUtils.equalAny(stage.getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
                if (!CollectionUtils.isEmpty(stage.getViewersRoles())) {
                    user = reloadIfNeed(user, "user-with-roles");
                    if (!CollectionUtils.isEmpty(user.getUserRoles())) {
                        for (UserRole ur : user.getUserRoles()) {
                            if (stage.getViewersRoles().contains(ur.getRole())) {
                                return true;
                            }
                        }
                    }
                } else if (!CollectionUtils.isEmpty(stage.getViewers())) {
                    return stage.getViewers().contains(user);
                }
            }
        }
        return false;
    }

    private <T extends Entity> T reloadIfNeed(T entity, String view) {
        if (!PersistenceHelper.isLoadedWithView(entity, view)) {
            entity = dataManager.reload(entity, view);
        }
        return entity;
    }

    @Override
    public void extendBrowser(Stage stage, Frame screen, boolean viewOnly) throws WorkflowException {
        Preconditions.checkNotNullArgument(stage, getMessage("WorkflowWebBeanImpl.stageIsEmpty"));
        Preconditions.checkNotNullArgument(screen, getMessage("WorkflowWebBeanImpl.frameIsEmpty"));

        stage = reloadIfNeed(stage, "stage-process");

        if (EqualsUtils.equalAny(stage.getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
            StopWatch sw = new Slf4JStopWatch(log);
            try {
                String script = constructScript(screen, stage.getBrowserScreenConstructor(), stage.getScreenConstructor());
                if (StringUtils.isEmpty(script)) {
                    script = stage.getBrowseScreenGroovyScript();
                }
                if (!StringUtils.isEmpty(script)) {
                    final Map<String, Object> binding = new HashMap<>();
                    binding.put(STAGE, stage);
                    binding.put(SCREEN, screen);
                    binding.put(VIEW_ONLY, viewOnly);
                    binding.put(ENTITY, null);
                    binding.put(CONTEXT, null);
                    binding.put(WORKFLOW_INSTANCE, null);
                    binding.put(WORKFLOW_INSTANCE_TASK, null);
                    scripting.evaluateGroovy(script, binding);
                } else {
                    log.info(String.format("For stage %s(%s) browser screen extension not specified", stage.getName(), stage.getId()));
                }
            } catch (Exception e) {
                if (e instanceof WorkflowException) {
                    throw (WorkflowException) e;
                }
                throw new WorkflowException(String.format(getMessage("WorkflowWebBeanImpl.error.screenStageExtensionInternalError"), stage.getName(), stage.getId()), e);
            } finally {
                sw.stop("WorkflowWebBeanImpl", "Browser screen extended");
            }
        }
    }

    @Override
    public void extendEditor(WorkflowEntity entity, Frame screen) throws WorkflowException {
        Preconditions.checkNotNullArgument(entity, getMessage("WorkflowWebBeanImpl.entityIsEmpty"));
        Preconditions.checkNotNullArgument(screen, getMessage("WorkflowWebBeanImpl.frameIsEmpty"));

        entity = reloadIfNeed(entity, View.LOCAL);
        final Stage stage = service.getStage(entity);
        if (stage == null) {
            log.warn(String.format("Extension of editor ignored since stage for entity '%s' not found", entity.getInstanceName()));
            return;
        }

        final Workflow workflow = service.getWorkflow(entity);
        if (workflow == null || !Boolean.TRUE.equals(workflow.getActive())) {
            log.warn(String.format("Extension of editor ignored since workflow for entity '%s' are missing or deactivated", entity.getInstanceName()));
            return;
        }

        final WorkflowInstance instance = service.getWorkflowInstance(entity);
        if (instance == null) {
            log.warn(String.format("Extension of editor ignored since workflow instance for entity '%s' are missing or finished", entity.getInstanceName()));
            return;
        }

        final WorkflowInstanceTask task = service.getWorkflowInstanceTask(entity, stage);
        if (task == null || task.getEndDate() != null) {
            log.warn(String.format("Extension of editor ignored since workflow instance task for entity '%s' are missing", entity.getInstanceName()));
            return;
        }

        extendEditor(stage, entity, screen, instance, task);
    }

    @Override
    public void extendEditor(Stage stage, WorkflowEntity entity, Frame screen, WorkflowInstance workflowInstance, WorkflowInstanceTask task) throws WorkflowException {
        Preconditions.checkNotNullArgument(stage, getMessage("WorkflowWebBeanImpl.stageIsEmpty"));
        Preconditions.checkNotNullArgument(entity, getMessage("WorkflowWebBeanImpl.entityIsEmpty"));
        Preconditions.checkNotNullArgument(screen, getMessage("WorkflowWebBeanImpl.frameIsEmpty"));
        Preconditions.checkNotNullArgument(workflowInstance, getMessage("WorkflowWebBeanImpl.workflowInstanceIsEmpty"));
        Preconditions.checkNotNullArgument(task, getMessage("WorkflowWebBeanImpl.workflowInstanceTaskIsEmpty"));

        entity = reloadIfNeed(entity, View.LOCAL);
        stage = reloadIfNeed(stage, "stage-process");
        workflowInstance = reloadIfNeed(workflowInstance, "workflowInstance-process");
        task = reloadIfNeed(task, "workflowInstanceTask-process");

        if (EqualsUtils.equalAny(stage.getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
            StopWatch sw = new Slf4JStopWatch(log);
            try {
                String script = constructScript(screen, stage.getEditorScreenConstructor(), stage.getScreenConstructor());
                if (StringUtils.isEmpty(script)) {
                    script = stage.getEditorScreenGroovyScript();
                }
                if (!StringUtils.isEmpty(script)) {
                    WorkflowExecutionContext ctx = service.getExecutionContext(workflowInstance);

                    Map<String, Object> binding = new HashMap<>();
                    binding.put(ENTITY, entity);
                    binding.put(CONTEXT, ctx.getParams());
                    binding.put(SCREEN, screen);
                    binding.put(WORKFLOW_INSTANCE, workflowInstance);
                    binding.put(WORKFLOW_INSTANCE_TASK, task);
                    binding.put(STAGE, stage);
                    binding.put(VIEW_ONLY, null);
                    scripting.evaluateGroovy(script, binding);

                    service.setExecutionContext(ctx, workflowInstance);//save parameters since they can be changed
                } else {
                    log.info(String.format("For stage %s(%s) editor screen extension not specified", stage.getName(), stage.getId()));
                }
            } catch (Exception e) {
                if (e instanceof WorkflowException) {
                    throw (WorkflowException) e;
                }
                throw new WorkflowException(String.format(getMessage("WorkflowWebBeanImpl.error.screenStageExtensionInternalError"), stage.getName(), stage.getId()), e);
            } finally {
                sw.stop("WorkflowWebBeanImpl", "Editor screen extended");
            }
        }
    }

    @Override
    public void extendScreen(String templateKey, Frame screen) throws WorkflowException {
        Preconditions.checkNotEmptyString(templateKey, getMessage("WorkflowWebBeanImpl.screenTemplateKeyIsEmpty"));
        Preconditions.checkNotNullArgument(screen, getMessage("WorkflowWebBeanImpl.frameIsEmpty"));

        ScreenExtensionTemplate template = getTemplate(templateKey);
        if (template != null) {
            StopWatch sw = new Slf4JStopWatch(log);
            try {
                String script = constructScript(screen, template.getScreenConstructor(), null);
                if (!StringUtils.isEmpty(script)) {
                    Map<String, Object> binding = new HashMap<>();
                    binding.put(SCREEN, screen);
                    binding.put(STAGE, null);
                    binding.put(VIEW_ONLY, null);
                    binding.put(ENTITY, null);
                    binding.put(CONTEXT, null);
                    binding.put(WORKFLOW_INSTANCE, null);
                    binding.put(WORKFLOW_INSTANCE_TASK, null);

                    scripting.evaluateGroovy(script, binding);
                } else {
                    log.info(String.format("For screen extension template '%s' script not specified", templateKey));
                }
            } catch (Exception e) {
                if (e instanceof WorkflowException) {
                    throw (WorkflowException) e;
                }
                throw new WorkflowException(getMessage("WorkflowWebBeanImpl.error.internalError"), e);
            } finally {
                sw.stop("WorkflowWebBeanImpl", "Screen extended");
            }
        } else {
            log.warn(String.format("Screen extension for template key '%s' not found", templateKey));
        }
    }

    @Nullable
    private ScreenExtensionTemplate getTemplate(String key) {
        return dataManager.load(ScreenExtensionTemplate.class)
                .query("select e from wfstp$ScreenExtensionTemplate e where e.key = :key")
                .parameter("key", key)
                .view(View.LOCAL)
                .optional()
                .orElse(null);
    }

    @Nullable
    protected String constructScript(Frame screen, String constructorJson, @Nullable String genericConstructorJson) throws Exception {
        if (!StringUtils.isEmpty(constructorJson)) {
            ObjectMapper objectMapper = new ObjectMapper();

            ScreenConstructor constructor = objectMapper.readValue(constructorJson, ScreenConstructor.class);

            if (!StringUtils.isEmpty(genericConstructorJson)) {
                populateConstructor(constructor, objectMapper.readValue(genericConstructorJson, ScreenConstructor.class));
            }

            Set<String> imports = new HashSet<>();
            Set<String> initSection = new LinkedHashSet<>();
            StringBuilder sb = new StringBuilder();

            setupStandardImports(imports);

            setupCustomBeforeExtension(constructor, imports, initSection, sb);
            setupActions(constructor, imports, initSection, sb, screen);
            if (Boolean.TRUE.equals(constructor.getIsBrowserScreen())) {
                setupBrowserSettings(constructor, imports, initSection, sb, screen);
            } else {
                setupEditorSettings(constructor, imports, initSection, sb, screen);
            }
            setupCustomAfterExtension(constructor, imports, initSection, sb);

            String script = constructScript(imports, initSection, sb);
            if (Boolean.TRUE.equals(webConfig.getPrintScreenExtensionsScript())) {
                log.debug("\n##########Generation screen script start##########\n" + script + "\n##########Generation screen script end##########\n");
            }
            return script;
        }
        return null;
    }

    protected void populateConstructor(ScreenConstructor to, ScreenConstructor from) {
        if (!CollectionUtils.isEmpty(from.getActions())) {
            List<ScreenAction> genericActions = new ArrayList<>(from.getActions());
            orderBy(genericActions, "order");

            List<ScreenAction> actions = to.getActions();
            if (!CollectionUtils.isEmpty(actions)) {
                orderBy(actions, "order");
                genericActions.addAll(actions);

                for (int i = 0; i < genericActions.size(); i++) {
                    genericActions.get(i).setOrder(i);
                }
            }
            to.setActions(genericActions);
        }
    }

    protected void setupStandardImports(Set<String> imports) {
        imports.add("import java.util.*");
    }

    //create a total script
    protected String constructScript(Set<String> imports, Set<String> initSection, StringBuilder sb) {
        StringBuilder result = sb;
        if (!CollectionUtils.isEmpty(imports) || !CollectionUtils.isEmpty(initSection)) {
            result = new StringBuilder();

            if (!CollectionUtils.isEmpty(imports)) {
                for (String importLine : imports) {
                    if (!StringUtils.isEmpty(importLine)) {
                        result.append(importLine).append("\n");
                    }
                }
            }
            result.append("\n\n");

            if (!CollectionUtils.isEmpty(initSection)) {
                for (String initLine : initSection) {
                    if (!StringUtils.isEmpty(initLine)) {
                        result.append(initLine).append("\n\n");
                    }
                }
            }
            result.append("\n");

            result.append(sb.toString());
        }
        return result.toString();
    }

    protected void setupCustomBeforeExtension(ScreenConstructor constructor, Set<String> imports, Set<String> initSection, StringBuilder sb) {
        ScriptWithImports res = ScriptWithImports.parse(constructor.getCustomBeforeScript());
        if (res != null) {
            imports.addAll(res.imports);
            initSection.add(res.script);
        }
    }

    protected void setupActions(ScreenConstructor constructor, Set<String> imports, Set<String> initSection, StringBuilder sb, Frame screen) throws Exception {
        List<ScreenAction> items = constructor.getActions();
        if (!CollectionUtils.isEmpty(items)) {
            String target = null;
            if (screen instanceof com.haulmont.cuba.gui.components.Component.HasXmlDescriptor) {
                target = ((com.haulmont.cuba.gui.components.Component.HasXmlDescriptor) screen).getXmlDescriptor().attributeValue("actions");
            }
            if (StringUtils.isEmpty(target)) {
                throw new WorkflowException(getMessage("WorkflowWebBeanImpl.actionsComponentNotFound"));
            }
            com.haulmont.cuba.gui.components.Component targetComponent = screen.getComponent(target);
            if (targetComponent == null) {
                throw new WorkflowException(getMessage("WorkflowWebBeanImpl.actionsComponentNotFound"));
            }

            int actionsStartPosition = 0;
            if (targetComponent instanceof Table) {
                targetComponent = ((Table) targetComponent).getButtonsPanel();
            }
            if (targetComponent instanceof com.haulmont.cuba.gui.components.Component.Container) {
                Collection children = ((com.haulmont.cuba.gui.components.Component.Container) targetComponent).getComponents();
                actionsStartPosition = children == null ? 0 : children.size();
            }

            orderBy(items, "order");

            imports.add("import com.haulmont.cuba.gui.components.*;");
            imports.add("import com.haulmont.cuba.gui.components.actions.*;");
            imports.add("import com.haulmont.cuba.gui.xml.layout.*;");
            imports.add("import com.groupstp.workflowstp.web.util.*;");
            imports.add("import org.apache.commons.collections4.*;");
            imports.add("import com.haulmont.cuba.core.global.*;");

            String paramSet = "stage, viewOnly, entity, context, workflowInstance, workflowInstanceTask);";
            String argSet = "final def stage, final def viewOnly, final def entity, final def context, final def workflowInstance, final def workflowInstanceTask";

            initSection.add("initActions(screen, screen.getComponentNN(\"" + target + "\"), " + paramSet);

            sb.append("\n");
            sb.append("private void initActions(final def screen, final def target, ").append(argSet).append(") {\n");
            sb.append(" def componentsFactory = AppBeans.get(ComponentsFactory.NAME);\n\n");

            String notAvailable = getMessage(getMessage("WorkflowWebBeanImpl.notAvailable"));
            Map<UUID, ScreenActionTemplate> templatesCache = new HashMap<>();
            boolean first = true;
            for (int i = 0; i < items.size(); i++) {
                int index = i + actionsStartPosition;
                ScreenAction action = items.get(i);
                ScreenActionTemplate template = getTemplate(action, templatesCache);

                boolean withButton = getValue(action, template, "buttonAction", Boolean.TRUE);

                if (first) {
                    first = false;
                } else {
                    sb.append("\n");
                }

                String actionId = "action" + index;
                String buttonId = "button" + index;
                String actionClass = getValue(action, template, "alwaysEnabled", Boolean.TRUE) ?
                        "com.groupstp.workflowstp.web.util.action.AlwaysActiveBaseAction" :
                        "com.haulmont.cuba.gui.components.actions.BaseAction";

                sb.append(" Action ").append(actionId).append(" = new ").append(actionClass).append("(\"").append(actionId).append("\") {\n");

                sb.append("  @Override\n");
                sb.append("  public String getCaption(){\n");
                sb.append("   return \"").append(getValue(action, template, "caption", notAvailable)).append("\";\n");
                sb.append("  }\n");

                sb.append("  @Override\n");
                sb.append("  public String getIcon(){\n");
                sb.append("   return \"").append(getValue(action, template, "icon", notAvailable)).append("\";\n");
                sb.append("  }\n");

                ScriptWithImportAndMethods res = ScriptWithImportAndMethods.parse(getValue(action, template, "script", null));
                if (res != null) {
                    imports.addAll(res.imports);
                    sb.append("  @Override\n");
                    sb.append("  public void actionPerform(com.haulmont.cuba.gui.components.Component component) {\n");
                    sb.append(res.script);
                    sb.append("\n  }\n");
                    if (!StringUtils.isEmpty(res.methods)) {
                        sb.append("\n");
                        sb.append(res.methods);
                        sb.append("\n");
                    }
                }

                if (Boolean.TRUE.equals(getValue(action, template, "permitRequired", null))) {
                    Integer count = getValue(action, template, "permitItemsCount", null);
                    ScriptWithImportAndMethods script = ScriptWithImportAndMethods.parse(getValue(action, template, "permitScript", null));
                    if (count != null || script != null) {
                        sb.append("  @Override\n");
                        sb.append("  public boolean isPermitted() {\n");
                        sb.append("   if (super.isPermitted()) {\n");
                        sb.append("    boolean fine = true;\n");
                        if (count != null) {
                            ComparingType compareOperator = getValue(action, template, "permitItemsType", ComparingType.EQUALS);
                            sb.append("    if (target instanceof com.haulmont.cuba.gui.components.Table) {\n");
                            sb.append("     Collection c = target.getSelected();\n");
                            sb.append("     fine = (c != null && c.size()").append(compareOperator.getOperator()).append(count).append(");\n");
                            sb.append("    }\n");
                        }
                        if (script != null) {
                            imports.addAll(script.imports);
                            sb.append("    if (fine) {\n");
                            sb.append(script.script);
                            sb.append("\n    }\n");
                        }
                        sb.append("    return fine;\n");
                        sb.append("   }\n");
                        sb.append("   return false;\n");
                        sb.append("  }\n");

                        if (script != null && !StringUtils.isEmpty(script.methods)) {
                            sb.append("\n");
                            sb.append(script.methods);
                            sb.append("\n");
                        }
                    }
                }
                sb.append(" };\n");

                String shortcut = getValue(action, template, "shortcut", null);
                if (!StringUtils.isEmpty(shortcut)) {
                    sb.append(" ").append(actionId).append(".setShortcut(\"").append(shortcut.toUpperCase()).append("\");\n");
                }
                if (withButton) {
                    sb.append(" Button ").append(buttonId).append(" = componentsFactory.createComponent(Button.class);\n");
                    sb.append(" ").append(buttonId).append(".setAction(").append(actionId).append(");\n");
                    String styleName = getValue(action, template, "style", null);
                    if (!StringUtils.isEmpty(styleName)) {
                        sb.append(" ").append(buttonId).append(".setStyleName(\"").append(styleName).append("\");\n");
                    }
                    sb.append(" ").append(actionId).append(".refreshState();\n");
                }
                sb.append(" if (target instanceof com.haulmont.cuba.gui.components.Table) {\n");
                if (withButton) {
                    sb.append("  target.getButtonsPanel().add(").append(buttonId).append(",").append(index).append(");\n");
                }
                sb.append("  target.addAction(").append(actionId).append(");\n");
                sb.append(" } else {\n");
                sb.append("  target.add(").append(buttonId).append(",").append(index).append(");\n");
                sb.append(" }\n");
            }
            sb.append("}\n");
        }
    }

    @Nullable
    protected ScreenActionTemplate getTemplate(ScreenAction action, Map<UUID, ScreenActionTemplate> templatesCache) {
        if (action.getTemplate() != null) {
            if (templatesCache.containsKey(action.getTemplate())) {
                return templatesCache.get(action.getTemplate());
            } else {
                ScreenActionTemplate tmp = dataManager.load(ScreenActionTemplate.class)
                        .id(action.getTemplate())
                        .view(View.LOCAL)
                        .optional()
                        .orElse(null);
                templatesCache.put(action.getTemplate(), tmp);
                return tmp;
            }
        }
        return null;
    }

    protected void setupBrowserSettings(ScreenConstructor constructor, Set<String> imports, Set<String> initSection, StringBuilder sb, Frame screen) throws Exception {
        List<ScreenTableColumn> items = constructor.getBrowserTableColumns();
        if (!CollectionUtils.isEmpty(items)) {
            String target = null;
            if (screen instanceof com.haulmont.cuba.gui.components.Component.HasXmlDescriptor) {
                target = ((com.haulmont.cuba.gui.components.Component.HasXmlDescriptor) screen).getXmlDescriptor().attributeValue("actions");
            }
            if (StringUtils.isEmpty(target)) {
                throw new WorkflowException(getMessage("WorkflowWebBeanImpl.actionsComponentNotFound"));
            }
            com.haulmont.cuba.gui.components.Component targetComponent = screen.getComponent(target);
            if (targetComponent == null) {
                throw new WorkflowException(getMessage("WorkflowWebBeanImpl.actionsComponentNotFound"));
            }

            orderBy(items, "order");

            imports.add("import com.haulmont.cuba.gui.components.*;");
            imports.add("import com.groupstp.workflowstp.web.util.*;");
            imports.add("import java.util.*");

            initSection.add("initColumns(screen.getComponentNN(\"" + target + "\"), stage, viewOnly);");

            sb.append("\nprivate void initColumns(final def target, final def stage, final def viewOnly) {\n");

            Map<UUID, ScreenTableColumnTemplate> cache = new HashMap<>();

            sb.append(" Map<String, com.groupstp.workflowstp.web.util.data.ColumnGenerator> customGenerators = new HashMap<>();\n");
            sb.append(" def generator = null;\n\n");
            for (ScreenTableColumn item : items) {
                ScreenTableColumnTemplate template = getTemplate(item, cache);
                String script;
                ScriptWithImports generator = ScriptWithImports.parse(getValue(item, template, "generatorScript", null));
                if (generator != null) {
                    imports.addAll(generator.imports);
                    script = generator.script;
                    if (script.startsWith("return")) {
                        script = script.substring("return".length());
                    }
                } else {
                    continue;
                }
                sb.append(" generator = ").append(script).append("\n");
                sb.append(" if (generator != null && !(generator instanceof com.groupstp.workflowstp.web.util.data.ColumnGenerator)) {\n");
                sb.append("     generator = new com.groupstp.workflowstp.web.util.data.ColumnGenerator(generator, null);\n");
                sb.append(" }\n");
                sb.append(" customGenerators.put(\"").append(getValue(item, template, "columnId", StringUtils.EMPTY)).append("\", generator);\n");
            }
            sb.append("\n List all = Arrays.asList(").append(items.stream()
                    .map(e -> "\"" + getValue(e, getTemplate(e, cache), "columnId", StringUtils.EMPTY) + "\"")
                    .collect(Collectors.joining(",")))
                    .append(");\n");

            boolean inPlaceEdit = items.stream().map(e -> Boolean.TRUE.equals(getValue(e, getTemplate(e, cache), "editable", Boolean.FALSE)))
                    .filter(e -> e)
                    .findFirst()
                    .orElse(Boolean.FALSE);
            if (inPlaceEdit) {
                sb.append(" List editable = Arrays.asList(").append(items.stream()
                        .filter(e -> Boolean.TRUE.equals(getValue(e, getTemplate(e, cache), "editable", Boolean.FALSE)))
                        .map(e -> "\"" + getValue(e, getTemplate(e, cache), "columnId", StringUtils.EMPTY) + "\"")
                        .collect(Collectors.joining(",")))
                        .append(");\n");
                sb.append(" WebUiHelper.showColumns(target, all, editable, customGenerators, viewOnly);\n\n");
            } else {
                sb.append(" WebUiHelper.showColumns(target, all, customGenerators);\n\n");
            }

            for (ScreenTableColumn item : items) {
                String columnId = getValue(item, getTemplate(item, cache), "columnId", StringUtils.EMPTY);
                String caption = getValue(item, getTemplate(item, cache), "caption", StringUtils.EMPTY);
                sb.append(" target.setColumnCaption(\"").append(columnId).append("\", \"").append(caption).append("\");\n");
            }

            sb.append("}\n");
        }
    }

    @Nullable
    protected ScreenTableColumnTemplate getTemplate(ScreenTableColumn column, Map<UUID, ScreenTableColumnTemplate> templatesCache) {
        if (column.getTemplate() != null) {
            if (templatesCache.containsKey(column.getTemplate())) {
                return templatesCache.get(column.getTemplate());
            } else {
                ScreenTableColumnTemplate tmp = dataManager.load(ScreenTableColumnTemplate.class)
                        .id(column.getTemplate())
                        .view(View.LOCAL)
                        .optional()
                        .orElse(null);
                templatesCache.put(column.getTemplate(), tmp);
                return tmp;
            }
        }
        return null;
    }

    protected void setupEditorSettings(ScreenConstructor constructor, Set<String> imports, Set<String> initSection, StringBuilder sb, Frame screen) {
        List<ScreenField> items = constructor.getEditorEditableFields();
        if (!CollectionUtils.isEmpty(items)) {
            imports.add("import com.groupstp.workflowstp.web.util.*;");

            initSection.add("initFields();");

            sb.append("\nprivate void initFields() {\n");
            sb.append("final List ids = Arrays.asList(").append(items.stream().map(e -> "\"" + e.getFieldId() + "\"").collect(Collectors.joining(","))).append(");\n");
            sb.append("WebUiHelper.enableComponents(screen, ids);");
            sb.append("\n}\n");
        }
    }

    protected void setupCustomAfterExtension(ScreenConstructor constructor, Set<String> imports, Set<String> initSection, StringBuilder sb) {
        ScriptWithImports res = ScriptWithImports.parse(constructor.getCustomAfterScript());
        if (res != null) {
            imports.addAll(res.imports);
            initSection.add(res.script);
        }
    }

    protected <T> T getValue(Entity action, @Nullable Entity template, String property, T defaultValue) {
        T value = action.getValue(property);
        if (value == null || StringUtils.isEmpty(value.toString())) {
            value = template == null ? null : template.getValue(property);
            if (value == null || StringUtils.isEmpty(value.toString())) {
                value = defaultValue;
            }
        }
        return value;
    }

    @SuppressWarnings("all")
    protected void orderBy(List<? extends Entity> entities, String orderProperty) {
        entities.sort((o1, o2) -> {
            Object order1 = o1.getValue(orderProperty);
            Object order2 = o2.getValue(orderProperty);
            if (order1 == null) {
                return order2 == null ? 0 : 1;
            } else if (order2 == null) {
                return -1;
            } else {
                return ComparatorUtils.NATURAL_COMPARATOR.compare(order1, order2);
            }
        });
    }

    protected String getMessage(String messageKey) {
        return messages.getMessage(getClass(), messageKey);
    }

    /**
     * Prepared groovy script with separated imports
     */
    protected static class ScriptWithImports {
        protected final Set<String> imports;
        protected final String script;

        protected ScriptWithImports(Set<String> imports, String script) {
            this.imports = imports;
            this.script = script;
        }

        @Nullable
        public static ScriptWithImports parse(@Nullable String script) {
            if (!StringUtils.isEmpty(script)) {
                Set<String> imports = new HashSet<>();
                String clearedScript = script;

                Pattern pattern = Pattern.compile("import(\\s)+[^;]*;");
                Matcher matcher = pattern.matcher(script);
                while (matcher.find()) {
                    String group = matcher.group();

                    imports.add(group.trim());
                    clearedScript = clearedScript.replaceAll(group, StringUtils.EMPTY);
                }

                int codeIndex = -1;
                for (int i = 0; i < clearedScript.length(); i++) {
                    if (!Character.isWhitespace(clearedScript.charAt(i))) {
                        codeIndex = i;
                        break;
                    }
                }
                if (codeIndex != -1) {
                    clearedScript = clearedScript.substring(codeIndex);
                }

                return new ScriptWithImports(imports, clearedScript);
            }
            return null;
        }
    }

    /**
     * Prepared groovy script with separated imports and methods definitions
     */
    protected static class ScriptWithImportAndMethods extends ScriptWithImports {
        protected final String methods;

        protected ScriptWithImportAndMethods(Set<String> imports, String script, String methods) {
            super(imports, script);
            this.methods = methods;
        }

        @Nullable
        public static ScriptWithImportAndMethods parse(@Nullable String script) {
            if (!StringUtils.isEmpty(script)) {
                ScriptWithImports temp = ScriptWithImports.parse(script);
                if (temp != null) {
                    String scriptBody = temp.script;
                    String methods = StringUtils.EMPTY;

                    int publicIndex = Math.max(scriptBody.indexOf("public"), -1);
                    int privateIndex = Math.max(scriptBody.indexOf("private"), -1);
                    int protectedIndex = Math.max(scriptBody.indexOf("protected"), -1);

                    List<Integer> indexes = new ArrayList<>(3);
                    if (publicIndex > 0) {
                        indexes.add(publicIndex);
                    }
                    if (privateIndex > 0) {
                        indexes.add(privateIndex);
                    }
                    if (protectedIndex > 0) {
                        indexes.add(protectedIndex);
                    }

                    int methodsPart = indexes.stream().min(Integer::compareTo).orElse(-1);

                    if (methodsPart > 0) {
                        scriptBody = temp.script.substring(0, methodsPart);
                        methods = temp.script.substring(methodsPart, temp.script.length());
                    }

                    int codeIndex = -1;
                    for (int i = scriptBody.length() - 1; i > -1; i--) {
                        if (!Character.isWhitespace(scriptBody.charAt(i))) {
                            codeIndex = i;
                        } else {
                            codeIndex = -1;
                        }
                    }
                    if (codeIndex > 0) {
                        scriptBody = scriptBody.substring(0, codeIndex);
                    }

                    return new ScriptWithImportAndMethods(temp.imports, scriptBody, methods);
                }
            }
            return null;
        }
    }
}
