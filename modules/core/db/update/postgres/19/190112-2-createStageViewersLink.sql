alter table STAGE_VIEWERS_LINK add constraint FK_STAVIE_ON_STAGE foreign key (STAGE_ID) references WFSTP_STAGE(ID);
alter table STAGE_VIEWERS_LINK add constraint FK_STAVIE_ON_USER foreign key (VIEWER_ID) references SEC_USER(ID);
