package com.groupstp.workflowstp.entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * UI screen table abstract column representation
 *
 * @author adiatullin
 */
@MetaClass(name = "wfstp$ScreenTableColumn")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = NONE, setterVisibility = NONE, getterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class ScreenTableColumn extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = 794861808913239255L;

    @MetaProperty
    @JsonProperty("id")
    protected UUID id;

    @MetaProperty
    @JsonProperty("template")
    private UUID template;

    @MetaProperty
    @JsonProperty("order")
    private Integer order;

    @NotNull
    @MetaProperty
    @JsonProperty("caption")
    private String caption;

    @NotNull
    @MetaProperty
    @JsonProperty("columnId")
    private String columnId;

    @MetaProperty
    @JsonProperty("generatorScript")
    private String generatorScript;

    @MetaProperty
    @JsonProperty("editable")
    private Boolean editable;


    @Override
    public UUID getId() {
        if (id == null) {
            id = super.getId();
        }
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTemplate() {
        return template;
    }

    public void setTemplate(UUID template) {
        this.template = template;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getColumnId() {
        return columnId;
    }

    public void setColumnId(String columnId) {
        this.columnId = columnId;
    }

    public String getGeneratorScript() {
        return generatorScript;
    }

    public void setGeneratorScript(String generatorScript) {
        this.generatorScript = generatorScript;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }
}
