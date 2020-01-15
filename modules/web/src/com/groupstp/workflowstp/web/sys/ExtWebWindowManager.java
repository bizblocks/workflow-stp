package com.groupstp.workflowstp.web.sys;

import com.haulmont.cuba.gui.WindowContext;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.sys.WindowContextImpl;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import com.haulmont.cuba.gui.xml.layout.loaders.ComponentLoaderContext;
import com.haulmont.cuba.web.AppUI;
import com.haulmont.cuba.web.sys.WebScreens;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Extended Web layout window manager
 *
 * @author adiatullin
 */
public class ExtWebWindowManager extends WebScreens {

    public ExtWebWindowManager(AppUI ui) {
        super(ui);
    }

    /**
     * Create a window without open it.
     *
     * @param windowInfo web window info
     * @return new window without open
     */
    public Window createWindow(WindowInfo windowInfo) {
        try {
            return (Window) windowInfo.asScreen().newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
