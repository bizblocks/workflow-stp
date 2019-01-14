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
);
