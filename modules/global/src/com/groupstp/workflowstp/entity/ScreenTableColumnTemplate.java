package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * UI screen table column template representation
 *
 * @author adiatullin
 */
@NamePattern("%s|name")
@Table(name = "WFSTP_SCREEN_TABLE_COLUMN_TEMPLATE")
@Entity(name = "wfstp$ScreenTableColumnTemplate")
public class ScreenTableColumnTemplate extends StandardEntity {
    private static final long serialVersionUID = -2744499429665423700L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "ENTITY_NAME")
    private String entityName;

    @NotNull
    @Column(name = "CAPTION", nullable = false)
    private String caption;

    @NotNull
    @Column(name = "COLUMN_ID", nullable = false)
    private String columnId;

    @NotNull
    @Lob
    @Column(name = "GENERATOR_SCRIPT", nullable = false)
    private String generatorScript;

    @Column(name = "EDITABLE")
    private Boolean editable = false;


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
