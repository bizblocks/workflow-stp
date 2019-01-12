package com.groupstp.workflowstp.core.bean;

import com.haulmont.cuba.core.app.importexport.EntityImportExport;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author adiatullin
 */
public class ExtEntityImportExport extends EntityImportExport implements ExtEntityImportExportAPI {

    @Override
    public byte[] exportEntitiesSeparatelyToZIP(Collection<? extends Entity> entities, View view) {
        entities = reloadEntities(entities, view);

        Map<String, byte[]> items = new LinkedHashMap<>();
        for (Entity entity : entities) {
            byte[] json = exportEntitiesToJSON(Collections.singletonList(entity)).getBytes(StandardCharsets.UTF_8);
            String name = (entity.getId() == null ? "N/A" : entity.getId().toString()) + ".json";
            items.put(name, json);
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(byteArrayOutputStream);
        zipOutputStream.setMethod(ZipArchiveOutputStream.STORED);
        zipOutputStream.setEncoding(StandardCharsets.UTF_8.name());
        try {
            for (Map.Entry<String, byte[]> entry : items.entrySet()) {
                ArchiveEntry singleDesignEntry = newStoredEntry(entry.getKey(), entry.getValue());
                zipOutputStream.putArchiveEntry(singleDesignEntry);
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeArchiveEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error on creating zip archive during entities export", e);
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
        return byteArrayOutputStream.toByteArray();
    }
}
