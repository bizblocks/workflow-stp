package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,key")
@Table(name = "WFSTP_SCREEN_EXTENSION_TEMPLATE")
@Entity(name = "wfstp$ScreenExtensionTemplate")
public class ScreenExtensionTemplate extends StandardEntity {
    private static final long serialVersionUID = -6056866892811103084L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "KEY_", nullable = false, unique = true)
    private String key;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false)
    private String entityName;

    @NotNull
    @Column(name = "SCREEN_ID", nullable = false)
    private String screenId;

    @Column(name = "IS_BROWSER")
    private Boolean isBrowser = false;

    @NotNull
    @Lob
    @Column(name = "SCREEN_CONSTRUCTOR", nullable = false)
    private String screenConstructor;


    @MetaProperty
    public String getEntityCaption() {
        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        Metadata metadata = AppBeans.get(Metadata.NAME);
        return messageTools.getEntityCaption(metadata.getClassNN(getEntityName()));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public Boolean getIsBrowser() {
        return isBrowser;
    }

    public void setIsBrowser(Boolean isBrowser) {
        this.isBrowser = isBrowser;
    }

    public String getScreenConstructor() {
        return screenConstructor;
    }

    public void setScreenConstructor(String screenConstructor) {
        this.screenConstructor = screenConstructor;
    }
}
