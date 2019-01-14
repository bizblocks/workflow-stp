package com.groupstp.workflowstp.web.util.action;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;

import java.util.Collection;
import java.util.Set;

/**
 * Generic move up/down action
 *
 * @author adiatullin
 */
public class ItemMoveAction extends ItemTrackingAction {
    protected boolean up;
    protected String orderProperty;

    public ItemMoveAction(Table table, boolean up) {
        this(table, "order", up);
    }

    public ItemMoveAction(Table table, String orderProperty, boolean up) {
        super(table, up ? "up" : "down");

        this.up = up;
        this.orderProperty = orderProperty;

        Messages messages = AppBeans.get(Messages.NAME);

        setCaption(messages.getMainMessage(up ? "action.up" : "action.down"));
    }

    @Override
    public void actionPerform(Component component) {
        Entity entity = target.getSingleSelected();
        assert entity != null;
        Integer currentOrder = entity.getValue(orderProperty);
        assert currentOrder != null;
        Integer newOrder = up ? currentOrder - 1 : currentOrder + 1;

        //noinspection unchecked
        Collection<Entity> items = target.getDatasource().getItems();//already ordered

        Entity changing = IterableUtils.get(items, currentOrder - 1);
        Entity neighbor = IterableUtils.get(items, newOrder - 1);
        changing.setValue(orderProperty, newOrder);
        neighbor.setValue(orderProperty, currentOrder);

        sort();
    }

    protected void sort() {
        CollectionDatasource.Sortable.SortInfo<Object> sortInfo = new CollectionDatasource.Sortable.SortInfo<>();
        sortInfo.setOrder(CollectionDatasource.Sortable.Order.ASC);
        sortInfo.setPropertyPath(getTarget().getDatasource().getMetaClass().getPropertyPath(orderProperty));
        ((CollectionDatasource.Sortable) getTarget().getDatasource()).sort(new CollectionDatasource.Sortable.SortInfo[]{sortInfo});
    }

    @Override
    public boolean isPermitted() {
        if (super.isPermitted()) {
            //noinspection unchecked
            Set<Entity> items = target.getSelected();
            if (!CollectionUtils.isEmpty(items) && items.size() == 1) {
                Integer order = IterableUtils.get(items, 0).getValue(orderProperty);
                if (order != null) {
                    return up ? order > 1 : order < target.getDatasource().size();
                }
            }
        }
        return false;
    }
}