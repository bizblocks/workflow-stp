package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Simple result data transfer object
 *
 * @author adiatullin
 */
public class ResultDTO implements Serializable {
    private static final long serialVersionUID = -4735565725004463374L;

    @JsonProperty("result")
    private String result;


    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
