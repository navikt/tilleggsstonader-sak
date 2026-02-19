alter table oppfolging add column tema varchar(255);

update oppfolging set tema='TSO';

alter table oppfolging alter column tema set not null;
