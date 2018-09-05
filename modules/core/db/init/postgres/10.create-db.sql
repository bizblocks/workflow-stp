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
    EDITOR_SCREEN_GROOVY_SCRIPT text,
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
    ACTIVE boolean,
    ENTITY_NAME varchar(255) not null,
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
    --
    primary key (ID)
)^
-- end WFSTP_WORKFLOW_INSTANCE_TASK
-- begin STAGE_ACTORS_LINK
create table STAGE_ACTORS_LINK (
    STAGE_ID uuid,
    ACTOR_ID uuid,
    primary key (STAGE_ID, ACTOR_ID)
)^
-- end STAGE_ACTORS_LINK
-- begin STAGE_ACTORS_ROLES_LINK
create table STAGE_ACTORS_ROLES_LINK (
    STAGE_ID uuid,
    ACTOR_ROLE_ID uuid,
    primary key (STAGE_ID, ACTOR_ROLE_ID)
)^
-- end STAGE_ACTORS_ROLES_LINK
