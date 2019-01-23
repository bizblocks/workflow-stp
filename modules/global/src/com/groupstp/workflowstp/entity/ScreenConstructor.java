package com.groupstp.workflowstp.entity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.haulmont.chile.core.annotations.MetaClass;
import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.BaseUuidEntity;

import java.io.Serializable;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

/**
 * UI screen extension constructor representation
 *
 * @author adiatullin
 */
@MetaClass(name = "wfstp$ScreenConstructor")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = NONE, setterVisibility = NONE, getterVisibility = NONE, isGetterVisibility = NONE, creatorVisibility = NONE)
public class ScreenConstructor extends BaseUuidEntity implements Serializable {
    private static final long serialVersionUID = -4643105100783290854L;

    @MetaProperty
    @JsonProperty("isBrowserScreen")
    private Boolean isBrowserScreen;

    @MetaProperty
    @JsonProperty("actions")
    private List<ScreenAction> actions;

    @MetaProperty
    @JsonProperty("browserTableColumns")
    private List<ScreenTableColumn> browserTableColumns;

    @MetaProperty
    @JsonProperty("editorEditableFields")
    private List<ScreenField> editorEditableFields;

    @MetaProperty
    @JsonProperty("customBeforeScript")
    private String customBeforeScript;

    @MetaProperty
    @JsonProperty("customAfterScript")
    private String customAfterScript;


    public Boolean getIsBrowserScreen() {
        return isBrowserScreen;
    }

    public void setIsBrowserScreen(Boolean isBrowserScreen) {
        this.isBrowserScreen = isBrowserScreen;
    }

    public List<ScreenAction> getActions() {
        return actions;
    }

    public void setActions(List<ScreenAction> actions) {
        this.actions = actions;
    }

    public List<ScreenTableColumn> getBrowserTableColumns() {
        return browserTableColumns;
    }

    public void setBrowserTableColumns(List<ScreenTableColumn> browserTableColumns) {
        this.browserTableColumns = browserTableColumns;
    }

    public List<ScreenField> getEditorEditableFields() {
        return editorEditableFields;
    }

    public void setEditorEditableFields(List<ScreenField> editorEditableFields) {
        this.editorEditableFields = editorEditableFields;
    }

    public String getCustomBeforeScript() {
        return customBeforeScript;
    }

    public void setCustomBeforeScript(String customBeforeScript) {
        this.customBeforeScript = customBeforeScript;
    }

    public String getCustomAfterScript() {
        return customAfterScript;
    }

    public void setCustomAfterScript(String customAfterScript) {
        this.customAfterScript = customAfterScript;
    }
}
