package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Stage specified editable field
 *
 * @author adiatullin
 * @see com.groupstp.workflowstp.rest.dto.StepDTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditorFieldDTO implements Serializable {
    private static final long serialVersionUID = 7571157234854475008L;

    @JsonProperty("id")
    private String id;
    @JsonProperty("caption")
    private String caption;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }
}
