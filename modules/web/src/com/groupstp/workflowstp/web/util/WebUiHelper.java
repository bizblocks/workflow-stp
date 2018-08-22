package com.groupstp.workflowstp.web.util;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.LinkButton;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.ConstraintOperationType;
import org.apache.commons.lang.StringUtils;

import java.util.function.Function;

/**
 * This util class which using for construct flexible screens
 *
 * @author adiatullin
 */
public final class WebUiHelper {
    private WebUiHelper() {
    }

    /**
     * Show related entity in table as link
     *
     * @param table          master entity table
     * @param entityProperty slave entity property name
     */
    public static void showLinkOnTable(Table table, String entityProperty) {
        showLinkOnTable(table, entityProperty, null);
    }

    /**
     * Show related entity in table as link
     *
     * @param table           master entity table
     * @param entityProperty  slave entity property name
     * @param captionFunction function to generate related entity link caption
     */
    public static void showLinkOnTable(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        ComponentsFactory factory = AppBeans.get(ComponentsFactory.NAME);
        Security security = AppBeans.get(Security.NAME);
        table.addGeneratedColumn(entityProperty, new Table.ColumnGenerator<Entity>() {
            @Override
            public Component generateCell(Entity entity) {
                LinkButton link = factory.createComponent(LinkButton.class);
                final Entity nested = entity.getValue(entityProperty);
                if (nested != null) {
                    link.setAction(new BaseAction(entityProperty + "Link") {
                        @Override
                        public void actionPerform(Component component) {
                            Window.Editor editor = table.getFrame().openEditor(nested, WindowManager.OpenType.THIS_TAB);
                            editor.addCloseListener(actionId -> table.getDatasource().refresh());
                        }

                        @Override
                        public boolean isPermitted() {
                            return super.isPermitted() && security.isPermitted(nested, ConstraintOperationType.READ);
                        }
                    });
                    link.setCaption(captionFunction == null ? nested.getInstanceName() : captionFunction.apply(nested));
                } else {
                    link.setCaption(StringUtils.EMPTY);
                }
                return link;
            }
        });
    }
}
