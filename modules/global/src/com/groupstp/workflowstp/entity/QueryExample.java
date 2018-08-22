package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;


/**
 * @author adiatullin
 */
@NamePattern("%s|comment")
@Table(name = "WFSTP_QUERY_EXAMPLE")
@Entity(name = "wfstp$QueryExample")
public class QueryExample extends StandardEntity implements WorkflowEntity<UUID> {

    private static final long serialVersionUID = 3375769695375401699L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "INITIATOR_ID", nullable = false)
    private User initiator;

    @Column(name = "STEP_NAME")
    private String stepName;

    @Column(name = "STATUS")
    private Integer status;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User getInitiator() {
        return initiator;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    @Override
    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    @Override
    public WorkflowEntityStatus getStatus() {
        return WorkflowEntityStatus.fromId(status);
    }

    @Override
    public void setStatus(WorkflowEntityStatus status) {
        this.status = status == null ? null : status.getId();
    }
}