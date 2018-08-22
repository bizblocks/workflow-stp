package com.groupstp.workflowstp.web.workflow;

import com.groupstp.workflowstp.entity.*;
import com.groupstp.workflowstp.web.stage.StageBrowse;
import com.groupstp.workflowstp.web.stepdirection.StepDirectionEdit;
import com.haulmont.bali.util.ParamsMap;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.ExtendedEntities;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.*;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class WorkflowEdit extends AbstractEditor<Workflow> {
    @Inject
    private Metadata metadata;
    @Inject
    private MessageTools messageTools;
    @Inject
    private ExtendedEntities extendedEntities;

    @Inject
    private FieldGroup generalFieldGroup;
    @Inject
    private LookupField entityNameField;
    @Inject
    private SplitPanel stepsSplit;
    @Inject
    private Table<Step> stepsTable;
    @Inject
    private Table<StepDirection> stepDirectionsTable;
    @Inject
    private CollectionDatasource.Sortable<Step, UUID> stepsDs;
    @Inject
    private CollectionDatasource.Sortable<StepDirection, UUID> directionsDs;

    private boolean editable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initEntityNameBehaviour();
        initStepsTable();
        initStepDirectionTable();
    }

    private void initEntityNameBehaviour() {
        //prepare workflow entities in system
        Map<String, Object> options = new TreeMap<>();
        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            if (WorkflowEntity.class.isAssignableFrom(metaClass.getJavaClass())) {
                MetaClass mainMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = mainMetaClass.getName();
                options.put(messageTools.getEntityCaption(metaClass) + " (" + originalName + ")", originalName);
            }
        }
        entityNameField.setOptionsMap(options);
        //after user selection disable choosing
        entityNameField.addValueChangeListener(e -> {
            entityNameField.setEditable(e.getValue() == null);//turn edit off
            stepsSplit.setVisible(e.getValue() != null);
        });
        stepsSplit.setVisible(false);
    }

    private void initStepsTable() {
        stepsTable.addAction(new AddAction(stepsTable) {
            @Override
            public void actionPerform(Component component) {
                Lookup.Handler handler = selected -> {
                    if (!CollectionUtils.isEmpty(selected)) {
                        //noinspection unchecked
                        List<Stage> items = new ArrayList<>(selected);

                        //remove already exist stages
                        Collection<Step> currentSteps = stepsDs.getItems();
                        if (!CollectionUtils.isEmpty(currentSteps)) {
                            for (Step step : currentSteps) {
                                if (step.getStage() != null) {
                                    items.remove(step.getStage());
                                }
                            }
                        }

                        if (!CollectionUtils.isEmpty(items)) {
                            int count = stepsDs.size();
                            for (Stage item : items) {
                                Step step = metadata.create(Step.class);
                                step.setOrder(count);
                                step.setStage(item);
                                step.setWorkflow(getItem());

                                stepsDs.addItem(step);
                                count += 1;
                            }
                        }
                    }
                };
                target.getFrame().openLookup(Stage.class, handler,
                        WindowManager.OpenType.DIALOG,
                        ParamsMap.of(StageBrowse.ENTITY_NAME, entityNameField.getValue(), StageBrowse.LOOKUP, Boolean.TRUE));
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && security.isEntityOpPermitted(Stage.class, EntityOp.READ) && editable;
            }
        });
        stepsTable.addAction(new RemoveAction(stepsTable) {
            @Override
            protected void afterRemove(Set selected) {
                super.afterRemove(selected);

                //check directions which end point is deleted steps
                Collection<Step> steps = stepsDs.getItems();
                if (!CollectionUtils.isEmpty(steps)) {
                    for (Step step : steps) {
                        if (!CollectionUtils.isEmpty(step.getDirections())) {
                            List<StepDirection> currentDirections = new ArrayList<>(step.getDirections());
                            for (StepDirection direction : currentDirections) {
                                if (selected.contains(direction.getTo())) {
                                    step.getDirections().remove(direction);
                                }
                            }
                        }
                    }
                }

                stepDirectionsTable.getActions().forEach(Action::refreshState);
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && editable;
            }
        });
        stepsTable.addAction(new ItemMoveAction(stepsTable, true));
        stepsTable.addAction(new ItemMoveAction(stepsTable, false));

        stepsDs.addCollectionChangeListener(e -> {
            correctOrderIfNeed(stepsTable, "order");
        });

        sortTable(stepsTable, "order");
    }

    private void initStepDirectionTable() {
        stepDirectionsTable.addAction(new CreateAction(stepDirectionsTable) {
            @Override
            public Map<String, Object> getWindowParams() {
                return prepareDirectionParams();
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && editable && stepsTable.getSingleSelected() != null;
            }
        });
        stepDirectionsTable.addAction(new EditAction(stepDirectionsTable) {
            @Override
            public Map<String, Object> getWindowParams() {
                return prepareDirectionParams();
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() && editable && stepDirectionsTable.getSingleSelected() != null;
            }
        });
        stepDirectionsTable.addAction(new RemoveAction(stepDirectionsTable) {
            @Override
            public boolean isPermitted() {
                return super.isPermitted() && editable;
            }
        });
        stepDirectionsTable.addAction(new ItemMoveAction(stepDirectionsTable, true));
        stepDirectionsTable.addAction(new ItemMoveAction(stepDirectionsTable, false));

        directionsDs.addCollectionChangeListener(e -> {
            correctOrderIfNeed(stepDirectionsTable, "order");
        });

        sortTable(stepDirectionsTable, "order");

        stepsDs.addItemChangeListener(e -> stepDirectionsTable.getActions().forEach(Action::refreshState));
    }

    //get params for creating direction between two steps
    private Map<String, Object> prepareDirectionParams() {
        Step selected = stepsTable.getSingleSelected();
        Collection<Step> total = stepsDs.getItems();
        if (!CollectionUtils.isEmpty(total)) {
            total = new ArrayList<>(total);
            total.remove(selected);
        } else {
            total = Collections.emptyList();
        }
        return ParamsMap.of(StepDirectionEdit.ORDER, directionsDs.size() + 1,
                StepDirectionEdit.FROM_STEP, selected,
                StepDirectionEdit.POSSIBLE_TO_STEPS, total);
    }

    private void sortTable(Table table, String orderProperty) {
        CollectionDatasource.Sortable.SortInfo<Object> sortInfo = new CollectionDatasource.Sortable.SortInfo<>();
        sortInfo.setOrder(CollectionDatasource.Sortable.Order.ASC);
        sortInfo.setPropertyPath(table.getDatasource().getMetaClass().getPropertyPath(orderProperty));
        ((CollectionDatasource.Sortable) table.getDatasource()).sort(new CollectionDatasource.Sortable.SortInfo[]{sortInfo});
    }

    //check and correct items order in table
    private void correctOrderIfNeed(Table table, String orderProperty) {
        //noinspection unchecked
        Collection<Entity> items = table.getDatasource().getItems();
        if (!CollectionUtils.isEmpty(items)) {
            int i = 1;
            for (Entity item : items) {
                Integer order = item.getValue(orderProperty);
                if (!Objects.equals(i, order)) {
                    item.setValue(orderProperty, i);
                }
                i += 1;
            }
        }
    }

    @Override
    public void postInit() {
        super.postInit();

        if (!PersistenceHelper.isNew(getItem())) {
            entityNameField.setEditable(false);
            generalFieldGroup.getFieldNN("name").setEditable(false);
        }

        editable = !Boolean.TRUE.equals(getItem().getActive());
        stepsTable.getActions().forEach(Action::refreshState);
        stepDirectionsTable.getActions().forEach(Action::refreshState);
    }

    @Override
    public boolean preCommit() {
        if (super.preCommit()) {
            if (isCycleDetected()) {
                return false;
            }
            return true;
        }
        return false;
    }

    //check what created steps are not cycle
    private boolean isCycleDetected() {
        return false;//TODO
    }


    //order moving action
    private class ItemMoveAction extends ItemTrackingAction {
        private final boolean up;
        private final String orderProperty;

        ItemMoveAction(Table table, boolean up) {
            super(table, up ? "up" : "down");

            this.up = up;
            this.orderProperty = "order";

            setCaption(getMessage(up ? "workflowEdit.up" : "workflowEdit.down"));
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

            sortTable((Table) target, orderProperty);
        }

        @Override
        public boolean isPermitted() {
            if (super.isPermitted() && editable) {
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
}