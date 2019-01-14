package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDelete;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author adiatullin
 */
@NamePattern("%s|stage")
@Table(name = "WFSTP_STEP")
@Entity(name = "wfstp$Step")
public class Step extends StandardEntity {
    private static final long serialVersionUID = 5659734088709526091L;

    @Column(name = "ORDER_")
    private Integer order;

    @Column(name = "START")
    private Boolean start = false;

    @OnDeleteInverse(DeletePolicy.DENY)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STAGE_ID", nullable = false)
    private Stage stage;

    @OrderBy("order")
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "from")
    private List<StepDirection> directions;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORKFLOW_ID", nullable = false)
    private Workflow workflow;


    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Boolean getStart() {
        return start;
    }

    public void setStart(Boolean start) {
        this.start = start;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public List<StepDirection> getDirections() {
        return directions;
    }

    public void setDirections(List<StepDirection> directions) {
        this.directions = directions;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }
}
