create table STAGE_VIEWERS_LINK (
    STAGE_ID uuid,
    VIEWER_ID uuid,
    primary key (STAGE_ID, VIEWER_ID)
);
