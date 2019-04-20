package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Simple result data transfer object
 *
 * @author adiatullin
 */
public class ResponseDTO<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = -4735565725004463374L;

    @JsonProperty("result")
    private T result;


    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
