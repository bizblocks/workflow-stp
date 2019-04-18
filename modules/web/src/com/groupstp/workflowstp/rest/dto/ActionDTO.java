package com.groupstp.workflowstp.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Step action class representation
 *
 * @author adiatullin
 */
public class ActionDTO implements Serializable {
    private static final long serialVersionUID = 8641784479409760516L;

    @JsonProperty("id")
    private String id;
    @JsonProperty("caption")
    private String caption;
    @JsonProperty("icon")
    private String icon;
    @JsonProperty("style")
    private String style;
    @JsonProperty("always_enabled")
    private Boolean alwaysEnabled;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Boolean getAlwaysEnabled() {
        return alwaysEnabled;
    }

    public void setAlwaysEnabled(Boolean alwaysEnabled) {
        this.alwaysEnabled = alwaysEnabled;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
