package com.groupstp.workflowstp.web.screenconstructor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupstp.workflowstp.entity.ScreenConstructor;
import com.groupstp.workflowstp.web.screenconstructor.frame.AbstractScreenConstructorFrame;
import com.groupstp.workflowstp.web.sys.ExtWebWindowManager;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Frame;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author adiatullin
 */
public class ScreenConstructorEditor extends AbstractWindow {
    public static final String SCREEN_ID = "screen-constructor";

    private static final String ENTITY_NAME = "entity-name";
    private static final String EXTENDING_SCREEN_ID = "extending-screen-id";
    private static final String BROWSER_SCREEN = "browser";
    private static final String CONSTRUCTOR_JSON = "constructor-json";

    private static final String BROWSER_TAB = "browserTab";
    private static final String EDITOR_TAB = "editorTab";

    /**
     * Show up the screen extending constructor
     *
     * @param frame           calling UI frame
     * @param entityName      extending screen entity name
     * @param screenId        extending screen id
     * @param browse          is extending screen are browser or not
     * @param constructorJson previous screen constructor json, may be null
     * @return opened constructor window
     */
    public static ScreenConstructorEditor show(Frame frame, String entityName, String screenId, boolean browse, String constructorJson) {
        Preconditions.checkNotNullArgument(frame, "Frame is empty");
        Preconditions.checkNotEmptyString(entityName, "Entity name not specified");
        Preconditions.checkNotEmptyString(screenId, "Extending screen id is empty");

        return (ScreenConstructorEditor) frame.openWindow(SCREEN_ID, WindowManager.OpenType.THIS_TAB,
                ParamsMap.of(ENTITY_NAME, entityName, EXTENDING_SCREEN_ID, screenId, BROWSER_SCREEN, browse, CONSTRUCTOR_JSON, constructorJson));
    }

    @Inject
    private WindowConfig windowConfig;
    @Inject
    private ExtWebWindowManager windowManager;
    @Inject
    private Metadata metadata;

    @Inject
    private DatasourceImplementation<ScreenConstructor> screenConstructorDs;
    @Inject
    private TabSheet mainTabSheet;
    private List<AbstractScreenConstructorFrame> frames;

    private MetaClass entityClass;
    private Window extendingWindow;
    private String screenConstructor;

    /**
     * @return constructed screen json result
     */
    public String getScreenConstructor() {
        return screenConstructor;
    }

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntity(params);
        initExtendingScreen(params);
        initScreenConstructor(params);

        getItem().setIsBrowserScreen(Boolean.TRUE.equals(params.get(BROWSER_SCREEN)));

        initTabSheet();
    }

    private void initEntity(Map<String, Object> params) {
        entityClass = metadata.getClassNN((String) params.get(ENTITY_NAME));
    }

    private void initExtendingScreen(Map<String, Object> params) {
        WindowInfo info = windowConfig.getWindowInfo((String) params.get(EXTENDING_SCREEN_ID));
        try {
            extendingWindow = windowManager.createWindow(info);
        } catch (Exception e) {
            throw new RuntimeException(getMessage("screenConstructorEditor.unableToExtendScreen"), e);
        }
    }

    private void initScreenConstructor(Map<String, Object> params) {
        String json = (String) params.get(CONSTRUCTOR_JSON);
        if (!StringUtils.isEmpty(json)) {
            ScreenConstructor screenConstructor = fromJson(json, ScreenConstructor.class);
            screenConstructorDs.setItem(screenConstructor);
        } else {
            screenConstructorDs.setItem(metadata.create(ScreenConstructor.class));
        }
        screenConstructorDs.setModified(false);
    }

    private void initTabSheet() {
        mainTabSheet.removeTab(Boolean.TRUE.equals(getItem().getIsBrowserScreen()) ? EDITOR_TAB : BROWSER_TAB);

        frames = new ArrayList<>();
        for (String name : Arrays.asList("actionsFrame", "browserFrame", "editorFrame", "customFrame")) {
            AbstractScreenConstructorFrame frame = (AbstractScreenConstructorFrame) getComponent(name);
            if (frame != null) {
                frame.setEntityMetaClass(entityClass);
                frame.setScreenConstructor(getItem());
                frame.setExtendingWindow(extendingWindow);

                frames.add(frame);
            }
        }
    }

    public void onOk() {
        for (AbstractScreenConstructorFrame frame : frames) {
            frame.prepare();
        }
        screenConstructor = toJson(screenConstructorDs.getItem());
        close(COMMIT_ACTION_ID, true);
    }

    public void onCancel() {
        close(CLOSE_ACTION_ID);
    }

    private ScreenConstructor getItem() {
        return screenConstructorDs.getItem();
    }

    private <T> String toJson(T object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return new ObjectMapper().readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }
}
