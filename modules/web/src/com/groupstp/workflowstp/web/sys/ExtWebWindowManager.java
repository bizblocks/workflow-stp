package com.groupstp.workflowstp.web.sys;

import com.haulmont.cuba.gui.WindowContext;
import com.haulmont.cuba.gui.WindowContextImpl;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoaderConfig;
import com.haulmont.cuba.gui.xml.layout.loaders.ComponentLoaderContext;
import com.haulmont.cuba.web.WebWindowManager;
import org.dom4j.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Extended Web layout window manager
 *
 * @author adiatullin
 */
public class ExtWebWindowManager extends WebWindowManager {

    /**
     * Create a window without open it.
     *
     * @param windowInfo web window info
     * @return new window without open
     */
    public Window createWindow(WindowInfo windowInfo) {
        if (windowInfo.getScreenClass() != null) {
            try {
                return (Window) windowInfo.getScreenClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to instantiate window class", e);
            }
        } else {
            Map<String, Object> params = new HashMap<>();
            LayoutLoaderConfig layoutConfig = LayoutLoaderConfig.getWindowLoaders();
            OpenType openType = OpenType.NEW_TAB;

            Element element = screenXmlLoader.load(windowInfo.getTemplate(), windowInfo.getId(), params);

            preloadMainScreenClass(element);//try to load main screen class to resolve dynamic compilation dependencies issues

            ComponentLoaderContext componentLoaderContext = new ComponentLoaderContext(params);
            componentLoaderContext.setFullFrameId(windowInfo.getId());
            componentLoaderContext.setCurrentFrameId(windowInfo.getId());

            ComponentLoader windowLoader = createLayout(windowInfo, element, componentLoaderContext, layoutConfig);
            Window window = (Window) windowLoader.getResultComponent();
            wrapByCustomClass(window, element);

            screenViewsLoader.deployViews(element);

            DsContext dsContext = loadDsContext(element);
            initDatasources(window, dsContext, params);

            componentLoaderContext.setDsContext(dsContext);

            WindowContext windowContext = new WindowContextImpl(window, openType, params);
            window.setContext(windowContext);
            dsContext.setFrameContext(windowContext);

            windowLoader.loadComponent();

            return window;
        }
    }
}
