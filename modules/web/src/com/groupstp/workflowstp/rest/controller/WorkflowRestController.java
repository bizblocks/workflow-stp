package com.groupstp.workflowstp.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.rest.dto.*;
import com.groupstp.workflowstp.rest.dto.generic.MessageDTO;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.restapi.exception.RestAPIException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ComparatorUtils;
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
 * Workflow REST controller
 *
 * @author adiatullin
 */
@RestController("wfstp_WorkflowRestController")
public class WorkflowRestController implements WorkflowRestAPI {
    private static final Logger log = LoggerFactory.getLogger(WorkflowRestController.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    protected WorkflowWebBean workflowWebBean;
    @Inject
    protected WorkflowService workflowService;
    @Inject
    protected UserSessionSource userSessionSource;
    @Inject
    protected Metadata metadata;
    @Inject
    protected Messages messages;

    protected final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public List<WorkflowDTO> getWorkflows() {
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
            log.error("Failed to prepare workflows", e);

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

                if (Boolean.TRUE.equals(getValue(action, template, "availableInExternalSystem", Boolean.TRUE))) {
                    ActionDTO dto = new ActionDTO();
                    dto.setId(action.getId().toString());
                    dto.setCaption(getValue(action, template, "caption", null));
                    dto.setIcon(getValue(action, template, "icon", null));
                    dto.setStyle(getValue(action, template, "style", null));
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
            if (!CollectionUtils.isEmpty(dtos)) {
                stepDto.setBrowserColumns(dtos);
            }
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
            if (!CollectionUtils.isEmpty(dtos)) {
                stepDto.setEditorFields(dtos);
            }
        }
    }

    @Override
    public void start(String entityId, String entityName) {
        WorkflowEntity entity = findEntity(entityId, entityName);

        if (isProcessing(entity)) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.entityAlreadyInProcess"), HttpStatus.BAD_REQUEST);
        }
        try {
            Workflow wf = workflowService.determinateWorkflow(entity);

            log.debug("Start entity {}:{} workflow {}", entity, entityId, wf.getInstanceName());

            workflowService.startWorkflow(entity, wf);
        } catch (WorkflowException e) {
            log.error(String.format("Failed to start workflow process for entity %s:%s", entity, entityId), e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseDTO<Boolean> isProcessing(String entityId, String entityName) {
        WorkflowEntity entity = findEntity(entityId, entityName);

        ResponseDTO<Boolean> response = new ResponseDTO<>();
        response.setResult(isProcessing(entity));

        return response;
    }

    protected boolean isProcessing(WorkflowEntity entity) {
        try {
            return workflowService.isProcessing(entity);
        } catch (Exception e) {
            log.error("Failed to check entity workflow processing", e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseDTO<Boolean> isPerformable(String[] entityIds, String workflowId, String stageId, String actionId) {
        return null;
    }

    @Override
    public ResponseDTO<String> perform(String[] entityIds, String workflowId, String stageId, String actionId) {
        return null;
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
        WorkflowEntity entity = (WorkflowEntity) dataManager.load(metaClass.getJavaClass())
                .id(id)
                .view(View.MINIMAL)
                .optional()
                .orElse(null);
        if (entity == null) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    format("WorkflowRestController.entityNotFound", entityName, idText), HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return entity;
    }

    protected Object parseId(MetaClass metaClass, String idText, Class idClass) {
        Object id = null;
        try {
            if (UUID.class.isAssignableFrom(idClass)) {
                id = UuidProvider.fromString(idText);
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
                .query("select e from wfstp$Workflow e where e.active = true order by e.order")
                .view("some_view")//TODO determinate view
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

    protected String getMessage(String mesageKey) {
        return messages.getMessage(getClass(), mesageKey);
    }

    protected String format(String messageKey, Object... args) {
        return String.format(getMessage(messageKey), args);
    }
}
