package com.groupstp.workflowstp.core.bean;

import com.haulmont.cuba.core.app.importexport.EntityImportExportAPI;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;

import java.util.Collection;

/**
 * @author adiatullin
 */
public interface ExtEntityImportExportAPI extends EntityImportExportAPI {

    String NAME = EntityImportExportAPI.NAME;

    /**
     * Export entities separately to zip archive
     *
     * @param entities entities to export
     * @param view     import entities
     * @return zip archive bytes
     */
    byte[] exportEntitiesSeparatelyToZIP(Collection<? extends Entity> entities, View view);
}
