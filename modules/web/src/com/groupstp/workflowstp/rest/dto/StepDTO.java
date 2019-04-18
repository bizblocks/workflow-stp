package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Workflow step data transfer object
 *
 * @author adiatullin
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepDTO implements Serializable {
    private static final long serialVersionUID = -6076433699456048191L;

    public enum Permission {
        FULL, READ_ONLY
    }

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("entity_name")
    private String entityName;
    @JsonProperty("permission")
    private Permission permission;
    @JsonProperty("actions")
    private List<ActionDTO> actions;
    @JsonProperty("order")
    private Integer order;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public List<ActionDTO> getActions() {
        return actions;
    }

    public void setActions(List<ActionDTO> actions) {
        this.actions = actions;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
