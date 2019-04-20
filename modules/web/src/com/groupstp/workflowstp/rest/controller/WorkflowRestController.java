package com.groupstp.workflowstp.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupstp.workflowstp.bean.WorkflowSugarProcessor;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.rest.config.WorkflowRestConfig;
import com.groupstp.workflowstp.rest.dto.*;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.haulmont.bali.datastruct.Pair;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.restapi.exception.RestAPIException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

/**
 * Workflow communication REST controller
 *
 * @author adiatullin
 */
@RestController("wfstp_WorkflowRestController")
public class WorkflowRestController implements WorkflowRestAPI {
    private static final Logger log = LoggerFactory.getLogger(WorkflowRestController.class);

    //parameters for Groovy script execution
    protected static final String ENTITIES_PARAMETER = "entities";
    protected static final String STAGE_PARAMETER = "stage";
    protected static final String VIEW_ONLY_PARAMETER = "viewOnly";
    protected static final String PAYLOAD_PARAMETER = "payload";

    @Inject
    protected DataManager dataManager;
    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected Metadata metadata;
    @Inject
    protected Messages messages;
    @Inject
    protected Scripting scripting;
    @Inject
    protected ViewRepository viewRepository;
    @Inject
    protected WorkflowWebBean workflowWebBean;
    @Inject
    protected WorkflowService workflowService;
    @Inject
    protected WorkflowSugarProcessor sugar;

    @Inject
    protected WorkflowRestConfig config;

    protected final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public List<WorkflowDTO> getWorkflows() {
        checkEnabled();

        try {
            List<WorkflowDTO> result = Collections.emptyList();

            List<Workflow> workflows = getWorkflowsEntities();
            if (!CollectionUtils.isEmpty(workflows)) {
                result = new ArrayList<>();

                User currentUser = userSessionSource.getUserSession().getCurrentOrSubstitutedUser();
                int i = 1;
                for (Workflow wf : workflows) {
                    if (!CollectionUtils.isEmpty(wf.getSteps())) {
                        List<StepDTO> steps = new ArrayList<>();
                        int j = 1;
                        for (Step step : wf.getSteps()) {
                            StepDTO.Permission permission = null;
                            if (EqualsUtils.equalAny(step.getStage().getType(), StageType.USERS_INTERACTION, StageType.ARCHIVE)) {
                                if (workflowWebBean.isActor(currentUser, step.getStage())) {
                                    permission = StepDTO.Permission.FULL;
                                } else if (workflowWebBean.isViewer(currentUser, step.getStage())) {
                                    permission = StepDTO.Permission.READ_ONLY;
                                }
                            }

                            if (permission != null) {
                                StepDTO stepDto = new StepDTO();
                                stepDto.setId(step.getId().toString());
                                stepDto.setName(step.getStage().getName());
                                stepDto.setEntityName(step.getStage().getEntityName());
                                stepDto.setPermission(permission);
                                stepDto.setOrder(j++);

                                ScreenConstructor constructor = getScreenConstructor(step.getStage());
                                if (constructor != null) {
                                    setupActions(stepDto, constructor);
                                    setupColumns(stepDto, constructor);
                                    setupFields(stepDto, constructor);
                                }

                                steps.add(stepDto);
                            }
                        }

                        if (!CollectionUtils.isEmpty(steps)) {
                            WorkflowDTO workflowDto = new WorkflowDTO();
                            workflowDto.setId(wf.getId().toString());
                            workflowDto.setName(wf.getName());
                            workflowDto.setCode(wf.getCode());
                            workflowDto.setEntityName(wf.getEntityName());
                            workflowDto.setSteps(steps);
                            workflowDto.setOrder(i++);

                            result.add(workflowDto);
                        }
                    }
                }
            }

            return result;
        } catch (Exception e) {
            if (e instanceof RestAPIException) {
                throw (RestAPIException) e;
            }
            log.error("Failed to prepare workflows items", e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected void setupActions(StepDTO stepDto, ScreenConstructor constructor) {
        List<ScreenAction> actions = constructor.getActions();
        if (!CollectionUtils.isEmpty(actions)) {
            Map<UUID, ScreenActionTemplate> cache = new HashMap<>();
            List<ActionDTO> dtos = new ArrayList<>();
            int i = 1;
            for (ScreenAction action : actions) {
                ScreenActionTemplate template = null;
                if (action.getTemplate() != null) {
                    template = cache.get(action.getTemplate());
                    if (template == null && !cache.containsKey(action.getTemplate())) {
                        template = dataManager.load(ScreenActionTemplate.class)
                                .id(action.getTemplate())
                                .view(View.LOCAL)
                                .optional()
                                .orElse(null);
                        cache.put(action.getTemplate(), template);
                    }
                }

                if (Boolean.TRUE.equals(getValue(action, template, "availableInExternalSystem", Boolean.FALSE))) {
                    ActionDTO dto = new ActionDTO();
                    dto.setId(action.getId().toString());
                    dto.setCaption(getValue(action, template, "caption", null));
                    dto.setIcon(convertIcon(getValue(action, template, "icon", null)));
                    dto.setStyle(convertStyle(getValue(action, template, "style", null)));
                    dto.setAlwaysEnabled(getValue(action, template, "alwaysEnabled", Boolean.FALSE));
                    dto.setOrder(i++);

                    dtos.add(dto);
                }
            }

            if (!CollectionUtils.isEmpty(dtos)) {
                stepDto.setActions(dtos);
            }
        }
    }

    protected void setupColumns(StepDTO stepDto, ScreenConstructor constructor) {
        List<ScreenTableColumn> columns = constructor.getBrowserTableColumns();
        if (!CollectionUtils.isEmpty(columns)) {
            Map<UUID, ScreenTableColumnTemplate> cache = new HashMap<>();
            List<BrowserColumnDTO> dtos = new ArrayList<>();
            int i = 1;
            for (ScreenTableColumn column : columns) {
                ScreenTableColumnTemplate template = null;
                if (column.getTemplate() != null) {
                    template = cache.get(column.getTemplate());
                    if (template == null && !cache.containsKey(column.getTemplate())) {
                        template = dataManager.load(ScreenTableColumnTemplate.class)
                                .id(column.getTemplate())
                                .view(View.LOCAL)
                                .optional()
                                .orElse(null);
                        cache.put(column.getTemplate(), template);
                    }
                }

                BrowserColumnDTO dto = new BrowserColumnDTO();
                dto.setId(getValue(column, template, "columnId", null));
                dto.setCaption(getValue(column, template, "caption", null));
                dto.setOrder(i++);

                dtos.add(dto);
            }

            stepDto.setBrowserColumns(dtos);
        }
    }

    protected void setupFields(StepDTO stepDto, ScreenConstructor constructor) {
        List<ScreenField> fields = constructor.getEditorEditableFields();
        if (!CollectionUtils.isEmpty(fields)) {
            List<EditorFieldDTO> dtos = new ArrayList<>();

            for (ScreenField field : fields) {
                EditorFieldDTO dto = new EditorFieldDTO();
                dto.setId(field.getFieldId());
                dto.setCaption(field.getName());

                dtos.add(dto);
            }

            stepDto.setEditorFields(dtos);
        }
    }

    @Override
    public ResponseDTO<String> start(String entityId, String entityName) {
        checkEnabled();

        WorkflowEntity entity = findEntity(entityId, entityName);

        if (!StringUtils.isBlank(getWorkflowInstanceId(entity))) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.entityAlreadyInProcess"), HttpStatus.BAD_REQUEST);
        }
        try {
            Workflow wf = workflowService.determinateWorkflow(entity);

            log.debug("Start entity {}:{} workflow {}", entity, entityId, wf.getInstanceName());

            String id = workflowService.startWorkflow(entity, wf).toString();

            ResponseDTO<String> result = new ResponseDTO<>();
            result.setResult(id);
            return result;
        } catch (Exception e) {
            log.error(String.format("Failed to start workflow process for entity %s:%s", entity, entityId), e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseDTO<String> isProcessing(String entityId, String entityName) {
        checkEnabled();

        WorkflowEntity entity = findEntity(entityId, entityName);

        ResponseDTO<String> response = new ResponseDTO<>();
        response.setResult(getWorkflowInstanceId(entity));

        return response;
    }

    @Nullable
    protected String getWorkflowInstanceId(WorkflowEntity entity) {
        try {
            WorkflowInstance instance = workflowService.getWorkflowInstance(entity);
            return instance == null ? null : instance.getId().toString();
        } catch (Exception e) {
            log.error("Failed to check entity workflow processing", e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseDTO<Boolean> isPerformable(String[] entityId, String workflowId, String stepId, String actionId) {
        checkEnabled();

        Step step = findWorkflowStep(workflowId, stepId);
        Pair<ScreenAction, ScreenActionTemplate> actionPair = getAction(step, actionId);
        boolean viewOnly = isViewOnly(step);

        List<WorkflowEntity> entities = new ArrayList<>();

        String entityName = step.getWorkflow().getEntityName();
        String stepName = step.getStage().getName();

        if (entityId != null) {
            for (String id : entityId) {
                WorkflowEntity entity = findEntity(id, entityName);
                if (!entities.contains(entity)) {
                    if (!stepName.equalsIgnoreCase(entity.getStepName())) {
                        throw new RestAPIException(getMessage("captions.error.general"),
                                format("WorkflowRestController.entityInAnotherStep", entity.getInstanceName(), stepName, entity.getStepName()),
                                HttpStatus.BAD_REQUEST);
                    }
                    entities.add(entity);
                }
            }
        }
        if (CollectionUtils.isEmpty(entities)) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.emptyEntities"),
                    HttpStatus.BAD_REQUEST);
        }

        ResponseDTO<Boolean> result = new ResponseDTO<>();

        ScreenAction action = actionPair.getFirst();
        ScreenActionTemplate actionTemplate = actionPair.getSecond();

        if (viewOnly && !Boolean.TRUE.equals(getValue(action, actionTemplate, "alwaysEnabled", null))) {
            result.setResult(Boolean.FALSE);
        }
        if (result.getResult() == null) {
            if (!Boolean.TRUE.equals(getValue(action, actionTemplate, "availableInExternalSystem", null))) {
                result.setResult(Boolean.FALSE);
            }
        }
        if (result.getResult() == null) {
            if (Boolean.TRUE.equals(getValue(action, actionTemplate, "permitRequired", null))) {
                Integer count = getValue(action, actionTemplate, "permitItemsCount", null);
                ComparingType type = getValue(action, actionTemplate, "permitItemsType", null);
                if (count != null && type != null) {
                    int currentCount = entities.size();
                    switch (type) {
                        case LESS: {
                            result.setResult(currentCount < count);
                            break;
                        }
                        case MORE: {
                            result.setResult(currentCount > count);
                            break;
                        }
                        case EQUALS: {
                            result.setResult(currentCount == count);
                            break;
                        }
                    }
                }
                if (result.getResult() == null) {
                    String script = getValue(action, actionTemplate, "externalPermitScript", null);
                    if (!StringUtils.isBlank(script)) {
                        try {
                            Map<String, Object> binding = new HashMap<>();
                            binding.put(ENTITIES_PARAMETER, entities);
                            binding.put(STAGE_PARAMETER, step.getStage());
                            binding.put(VIEW_ONLY_PARAMETER, viewOnly);

                            Object value = scripting.evaluateGroovy(prepareScript(script), binding);
                            if (value instanceof Boolean) {
                                result.setResult((Boolean) value);
                            } else if (value instanceof String) {
                                result.setResult(BooleanUtils.toBoolean((String) value));
                            } else {
                                //nothing return mean yes
                                result.setResult(Boolean.TRUE);
                            }
                        } catch (Exception e) {
                            log.error(String.format("Failed to check is action '%s|%s' performable or not", step.getStage().getName(), action.getCaption()), e);

                            throw new RestAPIException(getMessage("captions.error.general"), getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    }
                }
            }
        }
        if (result.getResult() == null) {
            result.setResult(Boolean.TRUE);
        }

        return result;
    }

    @Override
    public ResponseDTO<String> perform(String[] entityId, String workflowId, String stepId, String actionId, String payload) {
        checkEnabled();

        Step step = findWorkflowStep(workflowId, stepId);
        Pair<ScreenAction, ScreenActionTemplate> actionPair = getAction(step, actionId);
        boolean viewOnly = isViewOnly(step);

        List<WorkflowEntity> entities = new ArrayList<>();

        String entityName = step.getWorkflow().getEntityName();
        String stepName = step.getStage().getName();
        if (entityId != null) {
            for (String id : entityId) {
                WorkflowEntity entity = findEntity(id, entityName);
                if (!entities.contains(entity)) {
                    if (!stepName.equalsIgnoreCase(entity.getStepName())) {
                        throw new RestAPIException(getMessage("captions.error.general"),
                                format("WorkflowRestController.entityInAnotherStep", entity.getInstanceName(), stepName, entity.getStepName()),
                                HttpStatus.BAD_REQUEST);
                    }
                    entities.add(entity);
                }
            }
        }
        if (CollectionUtils.isEmpty(entities)) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.emptyEntities"),
                    HttpStatus.BAD_REQUEST);
        }


        ResponseDTO<String> result = new ResponseDTO<>();

        ScreenAction action = actionPair.getFirst();
        ScreenActionTemplate actionTemplate = actionPair.getSecond();

        if (viewOnly && !Boolean.TRUE.equals(getValue(action, actionTemplate, "alwaysEnabled", null))) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.accessDenied"), HttpStatus.NOT_ACCEPTABLE);
        }

        String script = getValue(action, actionTemplate, "externalScript", null);
        if (!StringUtils.isBlank(script)) {
            try {
                Map<String, Object> binding = new HashMap<>();
                binding.put(ENTITIES_PARAMETER, entities);
                binding.put(STAGE_PARAMETER, step.getStage());
                binding.put(VIEW_ONLY_PARAMETER, viewOnly);
                binding.put(PAYLOAD_PARAMETER, StringUtils.isBlank(payload) ? null : payload);

                scripting.evaluateGroovy(prepareScript(script), binding);

                result.setResult("Success");
            } catch (Exception e) {
                log.error(String.format("Failed evaluate action '%s|%s'", step.getStage().getName(), action.getCaption()), e);

                Throwable cause = e.getCause();
                if (cause instanceof RestAPIException) {
                    throw (RestAPIException) cause;
                }

                throw new RestAPIException(getMessage("captions.error.general"), getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error(String.format("For action %s external script not specified", action.getCaption()));

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return result;
    }

    protected WorkflowEntity findEntity(String idText, String entityName) {
        MetaClass metaClass = metadata.getClass(entityName);
        if (metaClass == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityClassNotFound", entityName), HttpStatus.BAD_REQUEST);
        }

        MetaProperty idProperty = metaClass.getProperty("id");
        if (idProperty == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityClassIdNotFound", entityName), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        Object id = parseId(metaClass, idText, idProperty.getJavaType());

        //noinspection unchecked
        View view = new View(new View.ViewParams()
                .entityClass(metaClass.getJavaClass())
                .src(viewRepository.getView(metaClass, View.LOCAL))
                .name("_wf"));
        view.addProperty("stepName");
        view.addProperty("workflow", viewRepository.getView(Workflow.class, View.MINIMAL));
        view.addProperty("status");

        //noinspection unchecked
        WorkflowEntity entity = (WorkflowEntity) dataManager.load(metaClass.getJavaClass())
                .id(id)
                .view(view)
                .optional()
                .orElse(null);
        if (entity == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityNotFound", entityName, idText), HttpStatus.NOT_FOUND);
        }
        return entity;
    }

    protected Step findWorkflowStep(String workflowIdText, String stepIdText) {
        UUID workflowId;
        try {
            workflowId = UUID.fromString(workflowIdText);
        } catch (Exception e) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.failedToParseId", "workflow", workflowIdText),
                    HttpStatus.BAD_REQUEST);
        }
        UUID stepId;
        try {
            stepId = UUID.fromString(stepIdText);
        } catch (Exception e) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.failedToParseId", "step", stepIdText),
                    HttpStatus.BAD_REQUEST);
        }
        Step step = dataManager.load(Step.class)
                .query("select e from wfstp$Step e where e.id = :stepId and e.workflow.id = :workflowId")
                .parameter("stepId", stepId)
                .parameter("workflowId", workflowId)
                .view("step-rest")
                .optional()
                .orElse(null);
        if (step == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.stepNotFound"),
                    HttpStatus.NOT_FOUND);
        }
        if (!Boolean.TRUE.equals(step.getWorkflow().getActive())) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.workflowDeactivated"),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return step;
    }

    protected Pair<ScreenAction, ScreenActionTemplate> getAction(Step step, String actionIdText) {
        UUID actionId;
        try {
            actionId = UUID.fromString(actionIdText);
        } catch (Exception e) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.failedToParseId", "action", actionIdText),
                    HttpStatus.BAD_REQUEST);
        }
        try {
            ScreenConstructor screenConstructor = getScreenConstructor(step.getStage());

            ScreenAction action = null;
            ScreenActionTemplate template = null;

            if (!CollectionUtils.isEmpty(screenConstructor.getActions())) {
                action = screenConstructor.getActions().stream()
                        .filter(e -> actionId.equals(e.getId()))
                        .findFirst()
                        .orElse(null);
            }
            if (action == null) {
                throw new RestAPIException(getMessage("captions.error.general"),
                        format("WorkflowRestController.actionWithIdNotFound", actionIdText),
                        HttpStatus.BAD_REQUEST);
            }
            if (action.getTemplate() != null) {
                template = dataManager.load(ScreenActionTemplate.class)
                        .id(action.getTemplate())
                        .view(View.LOCAL)
                        .optional()
                        .orElse(null);
            }

            return new Pair<>(action, template);
        } catch (Exception e) {
            if (e instanceof RestAPIException) {
                throw (RestAPIException) e;
            }
            log.error("Failed to retrieve action", e);
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.failedToRetrieveAction", actionIdText),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    protected boolean isViewOnly(Step step) {
        boolean viewOnly;
        User user = userSessionSource.getUserSession().getCurrentOrSubstitutedUser();
        if (workflowWebBean.isActor(user, step.getStage())) {
            viewOnly = false;
        } else if (workflowWebBean.isViewer(user, step.getStage())) {
            viewOnly = true;
        } else {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.accessDenied"), HttpStatus.NOT_ACCEPTABLE);
        }
        return viewOnly;
    }

    protected Object parseId(MetaClass metaClass, String idText, Class idClass) {
        Object id = null;
        try {
            if (UUID.class.isAssignableFrom(idClass)) {
                id = UUID.fromString(idText);
            } else if (Integer.class.isAssignableFrom(idClass)) {
                id = Integer.valueOf(idText);
            } else if (Long.class.isAssignableFrom(idClass)) {
                id = Long.valueOf(idText);
            } else if (String.class.isAssignableFrom(idClass)) {
                id = idText;
            }
        } catch (Exception e) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityIdCannotBeParsed", metaClass.getName(), idClass.getSimpleName()),
                    HttpStatus.BAD_REQUEST);
        }
        if (id == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityIdCannotBeParsed", metaClass.getName(), idClass.getSimpleName()),
                    HttpStatus.BAD_REQUEST);
        }
        return id;
    }

    protected List<Workflow> getWorkflowsEntities() {
        return dataManager.load(Workflow.class)
                .query("select e from wfstp$Workflow e where e.active = true order by e.order asc")
                .view("workflow-rest")
                .list();
    }

    protected ScreenConstructor getScreenConstructor(Stage stage) throws IOException {
        String genericConstructorJson = stage.getScreenConstructor();
        String browseConstructorJson = stage.getBrowserScreenConstructor();
        String editorConstructorJson = stage.getEditorScreenConstructor();

        //right now we are not split actions for browser and editor screens
        ScreenConstructor result = StringUtils.isEmpty(genericConstructorJson) ?
                metadata.create(ScreenConstructor.class) : objectMapper.readValue(genericConstructorJson, ScreenConstructor.class);

        List<ScreenAction> actions = result.getActions();
        if (actions == null) {
            actions = new ArrayList<>();
            result.setActions(actions);
        }

        if (!StringUtils.isEmpty(browseConstructorJson)) {
            ScreenConstructor browseConstructor = objectMapper.readValue(browseConstructorJson, ScreenConstructor.class);
            result.setBrowserTableColumns(browseConstructor.getBrowserTableColumns());
            orderBy(result.getBrowserTableColumns(), "order");

            if (!CollectionUtils.isEmpty(browseConstructor.getActions())) {
                actions.addAll(browseConstructor.getActions());
            }
        }
        if (!StringUtils.isEmpty(editorConstructorJson)) {
            ScreenConstructor editorConstruction = objectMapper.readValue(editorConstructorJson, ScreenConstructor.class);
            result.setEditorEditableFields(editorConstruction.getEditorEditableFields());

            if (!CollectionUtils.isEmpty(editorConstruction.getActions())) {
                actions.addAll(editorConstruction.getActions());
            }
        }
        if (!CollectionUtils.isEmpty(actions)) {
            orderBy(actions, "order");
        }

        return result;
    }

    protected <T> T getValue(Entity entity, @Nullable Entity template, String property, T defaultValue) {
        T value = entity.getValue(property);
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
        if (!CollectionUtils.isEmpty(entities)) {
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
    }

    protected String prepareScript(String script) {
        return sugar.prepareScript(script);
    }

    protected void checkEnabled() {
        if (!Boolean.TRUE.equals(config.getRestEnabled())) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.disabled"),
                    HttpStatus.BAD_GATEWAY);
        }
    }

    protected String getMessage(String mesageKey) {
        return messages.getMessage(getClass(), mesageKey);
    }

    protected String format(String messageKey, Object... args) {
        return String.format(getMessage(messageKey), args);
    }

    protected String convertIcon(String icon) {
        if (!StringUtils.isBlank(icon)) {
            try {
                String lowerCaseIcon = icon.toLowerCase();
                for (CubaIcon at : CubaIcon.values()) {
                    if (lowerCaseIcon.endsWith(at.name().toLowerCase())) {
                        return at.name();
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve icon", e.getMessage());
            }
        }
        return icon;
    }

    protected String convertStyle(String style) {
        if (!StringUtils.isBlank(style)) {
            try {
                //make conversion if need
            } catch (Exception e) {
                log.warn("Failed to retrieve style", e.getMessage());
            }
        }
        return style;
    }

}
