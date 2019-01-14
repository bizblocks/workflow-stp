package com.groupstp.workflowstp.web.components;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.web.gui.components.WebGroupTable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extending group table to support external selections
 *
 * @param <E> any entity
 * @author adiatullin
 */
public class ExternalSelectionGroupTable<E extends Entity> extends WebGroupTable<E> {
    private boolean externalSelectionEnabled = false;
    private Set<Object> externalSelection = new HashSet<>();

    /**
     * @return is custom selection enabled
     */
    public boolean getExternalSelectionEnabled() {
        return externalSelectionEnabled;
    }

    /**
     * Enable or disable custom selection
     *
     * @param externalSelectionEnabled value
     */
    public void setExternalSelectionEnabled(boolean externalSelectionEnabled) {
        this.externalSelectionEnabled = externalSelectionEnabled;

        if (!externalSelectionEnabled) {
            externalSelection.clear();
            refreshActionsState();
        }
    }

    /**
     * @return external (with checkboxes only) selection
     */
    public Set<E> getSelectedExternal() {
        if (!externalSelection.isEmpty()) {
            Set res = new LinkedHashSet<>();
            for (Object id : externalSelection) {
                Entity item = datasource.getItem(id);
                if (item != null) {
                    res.add(item);
                }
            }
            return res;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * @return default cuba selection
     */
    public Set<E> getSelectedInternal() {
        Set<Object> itemIds = super.getSelectedItemIds();

        if (itemIds != null) {
            Set res = new LinkedHashSet<>();
            for (Object id : itemIds) {
                Entity item = datasource.getItem(id);
                if (item != null) {
                    res.add(item);
                }
            }
            return res;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    protected Set<Object> getSelectedItemIds() {
        Set<Object> defaultSelection = super.getSelectedItemIds();

        if (externalSelectionEnabled) {
            if (CollectionUtils.isEmpty(externalSelection)) {
                if (!CollectionUtils.isEmpty(defaultSelection) && defaultSelection.size() == 1) {
                    return defaultSelection;
                }
            }
            return externalSelection;
        } else {
            return defaultSelection;
        }
    }

    /**
     * Append custom selection
     *
     * @param item selected entity
     */
    public void addExternalSelection(E item) {
        if (item != null) {
            if (!externalSelectionEnabled) {
                throw new DevelopmentException("External selection not enabled");
            }

            if (!datasource.containsItem(item.getId())) {
                throw new IllegalStateException("Datasource doesn't contain item to select: " + item);
            }

            externalSelection.add(item.getId());
            datasource.setItem(item);
            refreshActionsState();
        }
    }

    /**
     * Remove selection
     *
     * @param item unselected entity
     */
    public void removeExternalSelection(E item) {
        if (item != null) {
            if (!externalSelectionEnabled) {
                throw new DevelopmentException("External selection not enabled");
            }

            externalSelection.remove(item.getId());
            E entity = null;
            if (!CollectionUtils.isEmpty(externalSelection)) {
                entity = (E) datasource.getItem(IterableUtils.get(externalSelection, 0));
            } else {
                Set internalSelected = super.getSelectedItemIds();
                if (!CollectionUtils.isEmpty(internalSelected) && internalSelected.size() == 1) {
                    entity = (E) datasource.getItem(IterableUtils.get(internalSelected, 0));
                }
            }
            datasource.setItem(entity);
            refreshActionsState();
        }
    }
}
