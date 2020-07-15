-- begin WFSTP_SCREEN_EXTENSION_TEMPLATE
create unique index IDX_WFSTP_SCREEN_EXTENSION_TEMPLATE_UK_KEY_ on WFSTP_SCREEN_EXTENSION_TEMPLATE (KEY_) where DELETE_TS is null ^
-- end WFSTP_SCREEN_EXTENSION_TEMPLATE
-- begin WFSTP_STEP
alter table WFSTP_STEP add constraint FK_WFSTP_STEP_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID)^
alter table WFSTP_STEP add constraint FK_WFSTP_STEP_WORKFLOW foreign key (WORKFLOW_ID) references WFSTP_WORKFLOW(ID)^
create index IDX_WFSTP_STEP_STAGE on WFSTP_STEP (STAGE_ID)^
create index IDX_WFSTP_STEP_WORKFLOW on WFSTP_STEP (WORKFLOW_ID)^
-- end WFSTP_STEP
-- begin WFSTP_WORKFLOW_INSTANCE_TASK
alter table WFSTP_WORKFLOW_INSTANCE_TASK add constraint FK_WFSTP_WORKFLOW_INSTANCE_TASK_INSTANCE foreign key (INSTANCE_ID) references WFSTP_WORKFLOW_INSTANCE(ID)^
alter table WFSTP_WORKFLOW_INSTANCE_TASK add constraint FK_WFSTP_WORKFLOW_INSTANCE_TASK_STEP foreign key (STEP_ID) references WFSTP_STEP(ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_TASK_INSTANCE on WFSTP_WORKFLOW_INSTANCE_TASK (INSTANCE_ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_TASK_STEP on WFSTP_WORKFLOW_INSTANCE_TASK (STEP_ID)^
-- end WFSTP_WORKFLOW_INSTANCE_TASK
-- begin WFSTP_STEP_DIRECTION
alter table WFSTP_STEP_DIRECTION add constraint FK_WFSTP_STEP_DIRECTION_FROM foreign key (FROM_ID) references WFSTP_STEP(ID)^
alter table WFSTP_STEP_DIRECTION add constraint FK_WFSTP_STEP_DIRECTION_TO foreign key (TO_ID) references WFSTP_STEP(ID)^
create index IDX_WFSTP_STEP_DIRECTION_FROM on WFSTP_STEP_DIRECTION (FROM_ID)^
create index IDX_WFSTP_STEP_DIRECTION_TO on WFSTP_STEP_DIRECTION (TO_ID)^
-- end WFSTP_STEP_DIRECTION
-- begin WFSTP_WORKFLOW_DEFINITION
alter table WFSTP_WORKFLOW_DEFINITION add constraint FK_WFSTP_WORKFLOW_DEFINITION_WORKFLOW foreign key (WORKFLOW_ID) references WFSTP_WORKFLOW(ID)^
create index IDX_WFSTP_WORKFLOW_DEFINITION_WORKFLOW on WFSTP_WORKFLOW_DEFINITION (WORKFLOW_ID)^
-- end WFSTP_WORKFLOW_DEFINITION
-- begin WFSTP_WORKFLOW_INSTANCE_COMMENT
alter table WFSTP_WORKFLOW_INSTANCE_COMMENT add constraint FK_WFSTP_WORKFLOW_INSTANCE_COMMENT_INSTANCE foreign key (INSTANCE_ID) references WFSTP_WORKFLOW_INSTANCE(ID)^
alter table WFSTP_WORKFLOW_INSTANCE_COMMENT add constraint FK_WFSTP_WORKFLOW_INSTANCE_COMMENT_TASK foreign key (TASK_ID) references WFSTP_WORKFLOW_INSTANCE_TASK(ID)^
alter table WFSTP_WORKFLOW_INSTANCE_COMMENT add constraint FK_WFSTP_WORKFLOW_INSTANCE_COMMENT_AUTHOR foreign key (AUTHOR_ID) references SEC_USER(ID)^
alter table WFSTP_WORKFLOW_INSTANCE_COMMENT add constraint FK_WFSTP_WORKFLOW_INSTANCE_COMMENT_ATTACHMENT foreign key (ATTACHMENT_ID) references SYS_FILE(ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_COMMENT_INSTANCE on WFSTP_WORKFLOW_INSTANCE_COMMENT (INSTANCE_ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_COMMENT_TASK on WFSTP_WORKFLOW_INSTANCE_COMMENT (TASK_ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_COMMENT_AUTHOR on WFSTP_WORKFLOW_INSTANCE_COMMENT (AUTHOR_ID)^
-- end WFSTP_WORKFLOW_INSTANCE_COMMENT
-- begin WFSTP_WORKFLOW_INSTANCE
alter table WFSTP_WORKFLOW_INSTANCE add constraint FK_WFSTP_WORKFLOW_INSTANCE_WORKFLOW foreign key (WORKFLOW_ID) references WFSTP_WORKFLOW(ID)^
create index IDX_WFSTP_WORKFLOW_INSTANCE_WORKFLOW on WFSTP_WORKFLOW_INSTANCE (WORKFLOW_ID)^
-- end WFSTP_WORKFLOW_INSTANCE
-- begin WFSTP_STAGE_ACTORS_LINK
alter table WFSTP_STAGE_ACTORS_LINK add constraint FK_STAACT_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID)^
alter table WFSTP_STAGE_ACTORS_LINK add constraint FK_STAACT_USER foreign key (ACTOR_ID) references SEC_USER(ID)^
-- end WFSTP_STAGE_ACTORS_LINK
-- begin WFSTP_TASK_PERFORMERS_LINK
alter table WFSTP_TASK_PERFORMERS_LINK add constraint FK_TASPER_WORKFLOW_INSTANCE_TASK foreign key (TASK_ID) references WFSTP_WORKFLOW_INSTANCE_TASK(ID)^
alter table WFSTP_TASK_PERFORMERS_LINK add constraint FK_TASPER_USER foreign key (PERFORMER_ID) references SEC_USER(ID)^
-- end WFSTP_TASK_PERFORMERS_LINK
-- begin WFSTP_STAGE_VIEWERS_ROLES_LINK
alter table WFSTP_STAGE_VIEWERS_ROLES_LINK add constraint FK_STAVIEROL_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID)^
alter table WFSTP_STAGE_VIEWERS_ROLES_LINK add constraint FK_STAVIEROL_ROLE foreign key (VIEWER_ROLE_ID) references SEC_ROLE(ID)^
-- end WFSTP_STAGE_VIEWERS_ROLES_LINK
-- begin WFSTP_STAGE_ACTORS_ROLES_LINK
alter table WFSTP_STAGE_ACTORS_ROLES_LINK add constraint FK_STAACTROL_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID)^
alter table WFSTP_STAGE_ACTORS_ROLES_LINK add constraint FK_STAACTROL_ROLE foreign key (ACTOR_ROLE_ID) references SEC_ROLE(ID)^
-- end WFSTP_STAGE_ACTORS_ROLES_LINK
-- begin WFSTP_STAGE_VIEWERS_LINK
alter table WFSTP_STAGE_VIEWERS_LINK add constraint FK_STAVIE_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID)^
alter table WFSTP_STAGE_VIEWERS_LINK add constraint FK_STAVIE_USER foreign key (VIEWER_ID) references SEC_USER(ID)^
-- end WFSTP_STAGE_VIEWERS_LINK
