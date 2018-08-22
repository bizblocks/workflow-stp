package com.groupstp.workflowstp.web.stage;

import com.groupstp.workflowstp.entity.Stage;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.gui.WindowParam;
import com.haulmont.cuba.gui.components.AbstractLookup;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.data.CollectionDatasource;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

/**
 * @author adiatullin
 */
public class StageBrowse extends AbstractLookup {
    public static final String ENTITY_NAME = "entityName";
    public static final String LOOKUP = "lookup";

    @Inject
    private CollectionDatasource<Stage, UUID> stagesDs;
    @Inject
    private GroupTable<Stage> stagesTable;

    @WindowParam(name = LOOKUP)
    private Boolean lookup;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        stagesDs.refresh(ParamsMap.of("entityName", params.get(ENTITY_NAME)));
    }

    @Override
    public void ready() {
        super.ready();

        if (Boolean.TRUE.equals(lookup)) {//lookup mode - remove all actions and make table multiselectable
            stagesTable.removeAllActions();
            stagesTable.getButtonsPanel().removeAll();
            stagesTable.setMultiSelect(true);
        }

        stagesTable.expandAll();
    }
}