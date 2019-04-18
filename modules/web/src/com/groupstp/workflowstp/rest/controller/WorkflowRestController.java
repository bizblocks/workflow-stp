package com.groupstp.workflowstp.rest.controller;

import com.groupstp.workflowstp.entity.StageType;
import com.groupstp.workflowstp.entity.Step;
import com.groupstp.workflowstp.entity.Workflow;
import com.groupstp.workflowstp.entity.WorkflowEntity;
import com.groupstp.workflowstp.exception.WorkflowException;
import com.groupstp.workflowstp.rest.dto.generic.MessageDTO;
import com.groupstp.workflowstp.rest.dto.StepDTO;
import com.groupstp.workflowstp.rest.dto.WorkflowDTO;
import com.groupstp.workflowstp.service.WorkflowService;
import com.groupstp.workflowstp.util.EqualsUtils;
import com.groupstp.workflowstp.web.bean.WorkflowWebBean;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.restapi.exception.RestAPIException;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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


    @Override
    public List<WorkflowDTO> getWorkflows() {
        List<WorkflowDTO> result = Collections.emptyList();

        List<Workflow> workflows = getWorkflowsEntities();
        if (!CollectionUtils.isEmpty(workflows)) {
            result = new ArrayList<>();

            User currentUser = userSessionSource.getUserSession().getCurrentOrSubstitutedUser();
            for (Workflow wf : workflows) {
                if (!CollectionUtils.isEmpty(wf.getSteps())) {
                    List<StepDTO> steps = new ArrayList<>();

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
                            stepDto.setName(step.getStage().getName());
                            stepDto.setEntityName(step.getStage().getEntityName());
                            stepDto.setOrder(step.getOrder());
                            stepDto.setPermission(permission);

                            steps.add(stepDto);
                        }
                    }
                    if (!CollectionUtils.isEmpty(steps)) {
                        WorkflowDTO workflowDto = new WorkflowDTO();
                        workflowDto.setName(wf.getName());
                        workflowDto.setCode(wf.getCode());
                        workflowDto.setEntityName(wf.getEntityName());
                        workflowDto.setOrder(wf.getOrder());
                        workflowDto.setSteps(steps);

                        result.add(workflowDto);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void start(String id, String entityName) {
        WorkflowEntity entity = findEntity(id, entityName);

        if (isProcessing(entity)) {
            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("WorkflowRestController.entityAlreadyInProcess"), HttpStatus.BAD_REQUEST);
        }
        try {
            Workflow wf = workflowService.determinateWorkflow(entity);

            log.debug("Start entity {}:{} workflow {}", entity, id, wf.getInstanceName());

            workflowService.startWorkflow(entity, wf);
        } catch (WorkflowException e) {
            log.error(String.format("Failed to start workflow process for entity %s:%s", entity, id), e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public MessageDTO isProcessing(String id, String entityName) {
        WorkflowEntity entity = findEntity(id, entityName);

        MessageDTO result = new MessageDTO();
        result.setProperty("result", isProcessing(entity));

        return result;
    }

    protected boolean isProcessing(WorkflowEntity entity) {
        try {
            return workflowService.isProcessing(entity);
        } catch (Exception e) {
            log.error("Failed to check entity worflow processing", e);

            throw new RestAPIException(getMessage("captions.error.general"),
                    getMessage("captions.error.internal"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public MessageDTO isPerformable(String workflowId, String stageId, String actionId) {
        return null;
    }

    @Override
    public MessageDTO perform(String workflowId, String stageId, String actionId) {
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

    protected String getMessage(String mesageKey) {
        return messages.getMessage(getClass(), mesageKey);
    }

    protected String format(String messageKey, Object... args) {
        return String.format(getMessage(messageKey), args);
    }
}
