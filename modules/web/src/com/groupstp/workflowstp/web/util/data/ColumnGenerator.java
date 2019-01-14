package com.groupstp.workflowstp.web.util.data;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.Table;

import java.io.Serializable;

/**
 * Special 2 state column generator to extend the UI table
 *
 * @author adiatullin
 */
public final class ColumnGenerator<T extends Entity> implements Serializable {
    private static final long serialVersionUID = -359928629900028748L;

    protected Table.ColumnGenerator<T> readGenerator;
    protected Table.ColumnGenerator<T> editGenerator;

    public ColumnGenerator(Table.ColumnGenerator<T> readGenerator, Table.ColumnGenerator<T> editGenerator) {
        this.readGenerator = readGenerator;
        this.editGenerator = editGenerator;
    }

    /**
     * @return read mode column generator
     */
    public Table.ColumnGenerator<T> getReadGenerator() {
        return readGenerator;
    }

    /**
     * @return edit mode column generator
     */
    public Table.ColumnGenerator<T> getEditGenerator() {
        return editGenerator;
    }
}
