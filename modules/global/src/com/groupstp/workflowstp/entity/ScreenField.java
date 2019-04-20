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
 * UI screen abstract field with data
 *
 * @author adiatullin
 */
@MetaClass(name = "wfstp$ScreenField")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = NONE, setterVisibility = NONE, getterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class ScreenField extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = -3256039669663389827L;

    @MetaProperty
    @JsonProperty("id")
    protected UUID id;

    @NotNull
    @MetaProperty
    @JsonProperty("name")
    private String name;

    @NotNull
    @MetaProperty
    @JsonProperty("fieldId")
    private String fieldId;


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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }
}
