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
@NamePattern("%s-%s|from,to")
@Table(name = "WFSTP_STEP_DIRECTION")
@Entity(name = "wfstp$StepDirection")
public class StepDirection extends StandardEntity {
    private static final long serialVersionUID = -2403604726600820573L;

    @Column(name = "ORDER_")
    private Integer order;

    @NotNull
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FROM_ID", nullable = false)
    private Step from;

    @NotNull
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TO_ID", nullable = false)
    private Step to;

    @Lob
    @Column(name = "CONDITION_SQL_SCRIPT")
    private String conditionSqlScript;

    @Lob
    @Column(name = "CONDITION_XML")
    private String conditionXml;

    @Lob
    @Column(name = "CONDITION_GROOVY_SCRIPT")
    private String conditionGroovyScript;


    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Step getFrom() {
        return from;
    }

    public void setFrom(Step from) {
        this.from = from;
    }

    public Step getTo() {
        return to;
    }

    public void setTo(Step to) {
        this.to = to;
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
