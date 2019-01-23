package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DeletePolicy;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * Working workflow instance representation
 *
 * @author adiatullin
 */
@NamePattern("%s-%s|workflow,entityId,entityName")
@Table(name = "WFSTP_WORKFLOW_INSTANCE")
@Entity(name = "wfstp$WorkflowInstance")
public class WorkflowInstance extends StandardEntity {
    private static final long serialVersionUID = -6348581277378426995L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORKFLOW_ID", nullable = false)
    private Workflow workflow;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false)
    private String entityName;

    @NotNull
    @Column(name = "ENTITY_ID", nullable = false)
    private String entityId;

    @Lob
    @Column(name = "CONTEXT")
    private String context;

    @OrderBy("createTs")
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "instance")
    private List<WorkflowInstanceComment> comments;

    @OrderBy("createTs")
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "instance")
    private List<WorkflowInstanceTask> tasks;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "START_DATE")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "END_DATE")
    private Date endDate;

    @Lob
    @Column(name = "ERROR_")
    private String error;

    @Column(name = "ERROR_IN_TASK")
    private Boolean errorInTask = false;


    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    @MetaProperty
    public String getEntityCaption() {
        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        Metadata metadata = AppBeans.get(Metadata.NAME);
        return messageTools.getEntityCaption(metadata.getClassNN(getEntityName()));
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public List<WorkflowInstanceComment> getComments() {
        return comments;
    }

    public void setComments(List<WorkflowInstanceComment> comments) {
        this.comments = comments;
    }

    public List<WorkflowInstanceTask> getTasks() {
        return tasks;
    }

    public void setTasks(List<WorkflowInstanceTask> tasks) {
        this.tasks = tasks;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Boolean getErrorInTask() {
        return errorInTask;
    }

    public void setErrorInTask(Boolean errorInTask) {
        this.errorInTask = errorInTask;
    }
}
