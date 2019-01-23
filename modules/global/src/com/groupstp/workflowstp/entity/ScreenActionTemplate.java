package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * UI screen action template
 *
 * @author adiatullin
 */
@NamePattern("%s|name")
@Table(name = "WFSTP_SCREEN_ACTION_TEMPLATE")
@Entity(name = "wfstp$ScreenActionTemplate")
public class ScreenActionTemplate extends StandardEntity {
    private static final long serialVersionUID = 7324379641892596368L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ENTITY_NAME")
    private String entityName;

    @Column(name = "ALWAYS_ENABLED")
    private Boolean alwaysEnabled = false;

    @NotNull
    @Column(name = "CAPTION", nullable = false)
    private String caption;

    @NotNull
    @Column(name = "ICON", nullable = false)
    private String icon;

    @Column(name = "STYLE")
    private String style;

    @Column(name = "SHORTCUT")
    private String shortcut;

    @Column(name = "BUTTON_ACTION")
    private Boolean buttonAction = false;

    @NotNull
    @Lob
    @Column(name = "SCRIPT", nullable = false)
    private String script;

    @Column(name = "PERMIT_REQUIRED")
    private Boolean permitRequired = false;

    @Column(name = "PERMIT_ITEMS_COUNT")
    private Integer permitItemsCount;

    @Column(name = "PERMIT_ITEMS_TYPE")
    private Integer permitItemsType;

    @Lob
    @Column(name = "PERMIT_SCRIPT")
    private String permitScript;

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
}
