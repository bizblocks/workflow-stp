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
import java.util.List;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,entityName,code")
@Table(name = "WFSTP_WORKFLOW")
@Entity(name = "wfstp$Workflow")
public class Workflow extends StandardEntity {
    private static final long serialVersionUID = -6925865535999298172L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "CODE", nullable = false, length = 10)
    private String code;

    @Column(name = "ACTIVE")
    private Boolean active = false;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false)
    private String entityName;

    @OrderBy("order")
    @Composition
    @OnDelete(DeletePolicy.CASCADE)
    @OneToMany(mappedBy = "workflow")
    private List<Step> steps;

    @Column(name = "ORDER_")
    private Integer order;


    @MetaProperty
    public String getEntityCaption() {
        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        Metadata metadata = AppBeans.get(Metadata.NAME);
        return messageTools.getEntityCaption(metadata.getClassNN(getEntityName()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
