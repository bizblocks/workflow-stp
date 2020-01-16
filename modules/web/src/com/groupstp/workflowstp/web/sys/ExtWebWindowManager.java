package com.groupstp.workflowstp.web.sys;

import com.haulmont.cuba.gui.WindowContext;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.components.sys.WindowImplementation;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.screen.MapScreenOptions;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.ScreenOptions;
import com.haulmont.cuba.gui.sys.WindowContextImpl;
import com.haulmont.cuba.gui.xml.layout.ComponentLoader;
import com.haulmont.cuba.gui.xml.layout.LayoutLoader;
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
        if ( windowInfo.asScreen() != null) {
            try {
                return (Window) windowInfo.asScreen().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to instantiate window class", e);
            }
        }
        else {
            Map<String, Object> params = new HashMap<>();
            LayoutLoaderConfig layoutConfig = new LayoutLoaderConfig();
            OpenType openType = OpenType.NEW_TAB;
            Element element = screenXmlLoader.load(windowInfo.getTemplate(), windowInfo.getId(), params);

            ComponentLoaderContext componentLoaderContext = new ComponentLoaderContext((ScreenOptions) params);
            componentLoaderContext.setFullFrameId(windowInfo.getId());
            componentLoaderContext.setCurrentFrameId(windowInfo.getId());

            MapScreenOptions options = new MapScreenOptions(params);
            Screen screen = createScreen(windowInfo, openType.getOpenMode(), options);
            Window window = screen.getWindow();
            screenViewsLoader.deployViews(element);

            DsContext dsContext = loadDsContext(element);
            initDatasources(window, dsContext, params);

            componentLoaderContext.setDsContext(dsContext);

            WindowContext windowContext = new WindowContextImpl(window, openType.getOpenMode());
            ((WindowImplementation)window).setContext(windowContext);
            dsContext.setFrameContext(windowContext);

            return window;
        }

    }

}
