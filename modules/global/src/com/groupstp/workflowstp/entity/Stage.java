package com.groupstp.workflowstp.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.Role;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,entityName,type")
@Table(name = "WFSTP_STAGE")
@Entity(name = "wfstp$Stage")
public class Stage extends StandardEntity {
    private static final long serialVersionUID = 1256356222180211224L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @NotNull
    @Column(name = "ENTITY_NAME", nullable = false)
    private String entityName;

    @NotNull
    @Column(name = "TYPE", nullable = false)
    private Integer type;

    @ManyToMany
    @JoinTable(name = "WFSTP_STAGE_ACTORS_LINK",
            joinColumns = @JoinColumn(name = "STAGE_ID"),
            inverseJoinColumns = @JoinColumn(name = "ACTOR_ID"))
    private List<User> actors;

    @ManyToMany
    @JoinTable(name = "WFSTP_STAGE_ACTORS_ROLES_LINK",
            joinColumns = @JoinColumn(name = "STAGE_ID"),
            inverseJoinColumns = @JoinColumn(name = "ACTOR_ROLE_ID"))
    private List<Role> actorsRoles;

    @ManyToMany
    @JoinTable(name = "WFSTP_STAGE_VIEWERS_LINK",
            joinColumns = @JoinColumn(name = "STAGE_ID"),
            inverseJoinColumns = @JoinColumn(name = "VIEWER_ID"))
    private List<User> viewers;

    @ManyToMany
    @JoinTable(name = "WFSTP_STAGE_VIEWERS_ROLES_LINK",
            joinColumns = @JoinColumn(name = "STAGE_ID"),
            inverseJoinColumns = @JoinColumn(name = "VIEWER_ROLE_ID"))
    private List<Role> viewersRoles;

    @Lob
    @Column(name = "EXECUTION_GROOVY_SCRIPT")
    private String executionGroovyScript;

    @Deprecated
    @Lob
    @Column(name = "BROWSE_SCREEN_GROOVY_SCRIPT")
    private String browseScreenGroovyScript;

    @Lob
    @Column(name = "BROWSER_SCREEN_CONSTRUCTOR")
    private String browserScreenConstructor;

    @Deprecated
    @Lob
    @Column(name = "EDITOR_SCREEN_GROOVY_SCRIPT")
    private String editorScreenGroovyScript;

    @Lob
    @Column(name = "EDITOR_SCREEN_CONSTRUCTOR")
    private String editorScreenConstructor;

    @Lob
    @Column(name = "DIRECTION_VARIABLES")
    private String directionVariables;


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

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public StageType getType() {
        return StageType.fromId(type);
    }

    public void setType(StageType type) {
        this.type = type == null ? null : type.getId();
    }

    public List<User> getActors() {
        return actors;
    }

    public void setActors(List<User> actors) {
        this.actors = actors;
    }

    public List<Role> getActorsRoles() {
        return actorsRoles;
    }

    public void setActorsRoles(List<Role> actorsRoles) {
        this.actorsRoles = actorsRoles;
    }

    public List<User> getViewers() {
        return viewers;
    }

    public void setViewers(List<User> viewers) {
        this.viewers = viewers;
    }

    public List<Role> getViewersRoles() {
        return viewersRoles;
    }

    public void setViewersRoles(List<Role> viewersRoles) {
        this.viewersRoles = viewersRoles;
    }

    public String getExecutionGroovyScript() {
        return executionGroovyScript;
    }

    public void setExecutionGroovyScript(String executionGroovyScript) {
        this.executionGroovyScript = executionGroovyScript;
    }

    @Deprecated
    public String getBrowseScreenGroovyScript() {
        return browseScreenGroovyScript;
    }

    @Deprecated
    public void setBrowseScreenGroovyScript(String browseScreenGroovyScript) {
        this.browseScreenGroovyScript = browseScreenGroovyScript;
    }

    public String getBrowserScreenConstructor() {
        return browserScreenConstructor;
    }

    public void setBrowserScreenConstructor(String browserScreenConstructor) {
        this.browserScreenConstructor = browserScreenConstructor;
    }

    @Deprecated
    public String getEditorScreenGroovyScript() {
        return editorScreenGroovyScript;
    }

    @Deprecated
    public void setEditorScreenGroovyScript(String editorScreenGroovyScript) {
        this.editorScreenGroovyScript = editorScreenGroovyScript;
    }

    public String getEditorScreenConstructor() {
        return editorScreenConstructor;
    }

    public void setEditorScreenConstructor(String editorScreenConstructor) {
        this.editorScreenConstructor = editorScreenConstructor;
    }

    public String getDirectionVariables() {
        return directionVariables;
    }

    public void setDirectionVariables(String directionVariables) {
        this.directionVariables = directionVariables;
    }
}
