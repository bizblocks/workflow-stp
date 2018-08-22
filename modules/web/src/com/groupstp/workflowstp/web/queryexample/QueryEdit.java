package com.groupstp.workflowstp.web.queryexample;

import com.groupstp.workflowstp.entity.QueryExample;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.data.impl.DatasourceImplementation;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.Map;

/**
 * This editor screen using special for dictionary menu
 *
 * @author adiatullin
 */
public class QueryEdit extends AbstractEditor<QueryExample> {
    private static final String PROCESS_CODE = "markQueryAsDone";

    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private DatasourceImplementation<QueryExample> queryDs;


    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initLookupPickersFields();
    }

    private void initLookupPickersFields() {
        //cleanup open actions since in system we have combined screens only
        for (Component component : getComponents()) {
            if (component instanceof LookupPickerField) {
                ((LookupPickerField) component).removeAction(LookupPickerField.OpenAction.NAME);
            }
        }
    }

    @Override
    public void postInit() {
        super.postInit();

        if (!PersistenceHelper.isNew(getItem())) {
            setCaption(String.format(getMessage("queryEdit.captionWithName"), getItem().getInstanceName()));
        } else {
            getItem().setInitiator(getInitiator());//set current user as initiator if possible
            queryDs.setModified(false);
        }

    }

    private User getInitiator() {//get possible initiator by current user
        return userSessionSource.getUserSession().getUser();
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            return true;
        }
        return false;
    }
}