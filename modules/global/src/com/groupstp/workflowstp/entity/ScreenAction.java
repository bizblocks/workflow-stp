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
 * UI screen action representation
 *
 * @author adiatullin
 */
@MetaClass(name = "wfstp$ScreenAction")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = NONE, setterVisibility = NONE, getterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class ScreenAction extends BaseUuidEntity implements Serializable {

    private static final long serialVersionUID = -4907974240053342336L;

    @MetaProperty
    @JsonProperty("template")
    private UUID template;

    @MetaProperty
    @JsonProperty("order")
    private Integer order;

    @MetaProperty
    @JsonProperty("alwaysEnabled")
    private Boolean alwaysEnabled;

    @NotNull
    @MetaProperty
    @JsonProperty("caption")
    private String caption;

    @NotNull
    @MetaProperty
    @JsonProperty("icon")
    private String icon;

    @MetaProperty
    @JsonProperty("style")
    private String style;

    @MetaProperty
    @JsonProperty("shortcut")
    private String shortcut;

    @MetaProperty
    @JsonProperty("buttonAction")
    private Boolean buttonAction;

    @NotNull
    @MetaProperty
    @JsonProperty("script")
    private String script;

    @MetaProperty
    @JsonProperty("availableInExternalSystem")
    private Boolean availableInExternalSystem;

    @MetaProperty
    @JsonProperty("externalScript")
    private String externalScript;

    @MetaProperty
    @JsonProperty("permitRequired")
    private Boolean permitRequired;

    @MetaProperty
    @JsonProperty("permitItemsCount")
    private Integer permitItemsCount;

    @MetaProperty
    @JsonProperty("permitItemsType")
    private Integer permitItemsType;

    @MetaProperty
    @JsonProperty("permitScript")
    private String permitScript;

    @MetaProperty
    @JsonProperty("externalPermitScript")
    private String externalPermitScript;


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

    public Boolean getAlwaysEnabled() {
        return alwaysEnabled;
    }

    public void setAlwaysEnabled(Boolean alwaysEnabled) {
        this.alwaysEnabled = alwaysEnabled;
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

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public Boolean getButtonAction() {
        return buttonAction;
    }

    public void setButtonAction(Boolean buttonAction) {
        this.buttonAction = buttonAction;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public Boolean getAvailableInExternalSystem() {
        return availableInExternalSystem;
    }

    public void setAvailableInExternalSystem(Boolean availableInExternalSystem) {
        this.availableInExternalSystem = availableInExternalSystem;
    }

    public String getExternalScript() {
        return externalScript;
    }

    public void setExternalScript(String externalScript) {
        this.externalScript = externalScript;
    }

    public Boolean getPermitRequired() {
        return permitRequired;
    }

    public void setPermitRequired(Boolean permitRequired) {
        this.permitRequired = permitRequired;
    }

    public Integer getPermitItemsCount() {
        return permitItemsCount;
    }

    public void setPermitItemsCount(Integer permitItemsCount) {
        this.permitItemsCount = permitItemsCount;
    }

    public ComparingType getPermitItemsType() {
        return ComparingType.fromId(permitItemsType);
    }

    public void setPermitItemsType(ComparingType permitItemsType) {
        this.permitItemsType = permitItemsType == null ? null : permitItemsType.getId();
    }

    public String getPermitScript() {
        return permitScript;
    }

    public void setPermitScript(String permitScript) {
        this.permitScript = permitScript;
    }

    public String getExternalPermitScript() {
        return externalPermitScript;
    }

    public void setExternalPermitScript(String externalPermitScript) {
        this.externalPermitScript = externalPermitScript;
    }
}
