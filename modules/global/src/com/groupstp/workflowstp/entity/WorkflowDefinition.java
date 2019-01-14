package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * @author adiatullin
 */
@NamePattern("%s - %s|entityName,priority")
@Table(name = "WFSTP_WORKFLOW_DEFINITION")
@Entity(name = "wfstp$WorkflowDefinition")
public class WorkflowDefinition extends StandardEntity {
    private static final long serialVersionUID = 4418956540782790555L;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false)
    private String entityName;

    @OnDeleteInverse(DeletePolicy.CASCADE)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WORKFLOW_ID", nullable = false)
    private Workflow workflow;

    @NotNull
    @Column(name = "PRIORITY_", nullable = false)
    private Integer priority;

    @Lob
    @Column(name = "CONDITION_SQL_SCRIPT")
    private String conditionSqlScript;

    @Lob
    @Column(name = "CONDITION_XML")
    private String conditionXml;

    @Lob
    @Column(name = "CONDITION_GROOVY_SCRIPT")
    private String conditionGroovyScript;


    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getConditionSqlScript() {
        return conditionSqlScript;
    }

    public void setConditionSqlScript(String conditionSqlScript) {
        this.conditionSqlScript = conditionSqlScript;
    }

    public String getConditionXml() {
        return conditionXml;
    }

    public void setConditionXml(String conditionXml) {
        this.conditionXml = conditionXml;
    }

    public String getConditionGroovyScript() {
        return conditionGroovyScript;
    }

    public void setConditionGroovyScript(String conditionGroovyScript) {
        this.conditionGroovyScript = conditionGroovyScript;
    }
}
