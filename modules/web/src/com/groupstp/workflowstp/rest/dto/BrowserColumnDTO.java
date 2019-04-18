package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Stage specified column
 *
 * @author adiatullin
 * @see com.groupstp.workflowstp.rest.dto.StepDTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrowserColumnDTO implements Serializable {
    private static final long serialVersionUID = -8594189634310859815L;

    @JsonProperty("id")
    private String id;
    @JsonProperty("caption")
    private String caption;
    @JsonProperty("order")
    private Integer order;


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

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
