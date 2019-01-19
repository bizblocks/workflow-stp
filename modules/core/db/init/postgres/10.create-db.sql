-- begin WFSTP_STAGE
create table WFSTP_STAGE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    ENTITY_NAME varchar(255) not null,
    TYPE integer not null,
    EXECUTION_GROOVY_SCRIPT text,
    BROWSE_SCREEN_GROOVY_SCRIPT text,
    BROWSER_SCREEN_CONSTRUCTOR text,
    EDITOR_SCREEN_GROOVY_SCRIPT text,
    EDITOR_SCREEN_CONSTRUCTOR text,
    DIRECTION_VARIABLES text,
    --
    primary key (ID)
)^
-- end WFSTP_STAGE
-- begin WFSTP_STEP
create table WFSTP_STEP (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ORDER_ integer,
    START boolean,
    STAGE_ID uuid not null,
    WORKFLOW_ID uuid not null,
    --
    primary key (ID)
)^
-- end WFSTP_STEP
-- begin WFSTP_STEP_DIRECTION
create table WFSTP_STEP_DIRECTION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ORDER_ integer,
    FROM_ID uuid not null,
    TO_ID uuid not null,
    CONDITION_SQL_SCRIPT text,
    CONDITION_XML text,
    CONDITION_GROOVY_SCRIPT text,
    --
    primary key (ID)
)^
-- end WFSTP_STEP_DIRECTION
-- begin WFSTP_WORKFLOW
create table WFSTP_WORKFLOW (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    CODE varchar(10) not null,
    ACTIVE boolean,
    ENTITY_NAME varchar(255) not null,
    ORDER_ integer,
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW
-- begin WFSTP_WORKFLOW_INSTANCE
create table WFSTP_WORKFLOW_INSTANCE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    WORKFLOW_ID uuid not null,
    ENTITY_NAME varchar(255) not null,
    ENTITY_ID varchar(255) not null,
    CONTEXT text,
    START_DATE timestamp,
    END_DATE timestamp,
    ERROR_ text,
    ERROR_IN_TASK boolean,
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW_INSTANCE
-- begin WFSTP_WORKFLOW_INSTANCE_COMMENT
create table WFSTP_WORKFLOW_INSTANCE_COMMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    INSTANCE_ID uuid,
    TASK_ID uuid,
    AUTHOR_ID uuid,
    COMMENT text,
    ATTACHMENT_ID uuid,
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW_INSTANCE_COMMENT
-- begin WFSTP_WORKFLOW_INSTANCE_TASK
create table WFSTP_WORKFLOW_INSTANCE_TASK (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    INSTANCE_ID uuid not null,
    STEP_ID uuid not null,
    START_DATE timestamp,
    END_DATE timestamp,
    PERFORMER_ID uuid,
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW_INSTANCE_TASK
-- begin WFSTP_STAGE_ACTORS_LINK
create table WFSTP_STAGE_ACTORS_LINK (
    STAGE_ID uuid,
    ACTOR_ID uuid,
    primary key (STAGE_ID, ACTOR_ID)
)^
-- end WFSTP_STAGE_ACTORS_LINK
-- begin WFSTP_STAGE_ACTORS_ROLES_LINK
create table WFSTP_STAGE_ACTORS_ROLES_LINK (
    STAGE_ID uuid,
    ACTOR_ROLE_ID uuid,
    primary key (STAGE_ID, ACTOR_ROLE_ID)
)^
-- end WFSTP_STAGE_ACTORS_ROLES_LINK
-- begin WFSTP_SCREEN_ACTION_TEMPLATE
create table WFSTP_SCREEN_ACTION_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    ENTITY_NAME varchar(255),
    ALWAYS_ENABLED boolean,
    CAPTION varchar(255) not null,
    ICON varchar(255) not null,
    STYLE varchar(255),
    SHORTCUT varchar(255),
    BUTTON_ACTION boolean,
    SCRIPT text not null,
    PERMIT_REQUIRED boolean,
    PERMIT_ITEMS_COUNT integer,
    PERMIT_ITEMS_TYPE integer,
    PERMIT_SCRIPT text,
    --
    primary key (ID)
)^
-- end WFSTP_SCREEN_ACTION_TEMPLATE
-- begin WFSTP_SCREEN_EXTENSION_TEMPLATE
create table WFSTP_SCREEN_EXTENSION_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    KEY_ varchar(255) not null,
    ENTITY_NAME varchar(255) not null,
    SCREEN_ID varchar(255) not null,
    IS_BROWSER boolean,
    SCREEN_CONSTRUCTOR text not null,
    --
    primary key (ID)
)^
-- end WFSTP_SCREEN_EXTENSION_TEMPLATE
-- begin WFSTP_SCREEN_TABLE_COLUMN_TEMPLATE
create table WFSTP_SCREEN_TABLE_COLUMN_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    ENTITY_NAME varchar(255),
    CAPTION varchar(255) not null,
    COLUMN_ID varchar(255) not null,
    GENERATOR_SCRIPT text not null,
    EDITABLE boolean,
    --
    primary key (ID)
)^
-- end WFSTP_SCREEN_TABLE_COLUMN_TEMPLATE
-- begin WFSTP_WORKFLOW_DEFINITION
create table WFSTP_WORKFLOW_DEFINITION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ENTITY_NAME varchar(255) not null,
    WORKFLOW_ID uuid not null,
    PRIORITY_ integer not null,
    CONDITION_SQL_SCRIPT text,
    CONDITION_XML text,
    CONDITION_GROOVY_SCRIPT text,
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW_DEFINITION
-- begin WFSTP_STAGE_VIEWERS_LINK
create table WFSTP_STAGE_VIEWERS_LINK (
    STAGE_ID uuid,
    VIEWER_ID uuid,
    primary key (STAGE_ID, VIEWER_ID)
)^
-- end WFSTP_STAGE_VIEWERS_LINK
-- begin WFSTP_STAGE_VIEWERS_ROLES_LINK
create table WFSTP_STAGE_VIEWERS_ROLES_LINK (
    STAGE_ID uuid,
    VIEWER_ROLE_ID uuid,
    primary key (STAGE_ID, VIEWER_ROLE_ID)
)^
-- end WFSTP_STAGE_VIEWERS_ROLES_LINK
