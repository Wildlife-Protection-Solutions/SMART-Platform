    drop table ct_to_smart.element;
    drop table ct_to_smart.sighting;
    drop table ct_to_smart.timertrack;


    create table ct_to_smart.element (
        uuid integer not null,
        i varchar(38) not null,
        n varchar(255),
        primary key (uuid)
    );


    create table ct_to_smart.sighting (
        uuid integer not null,
        i varchar(38) not null,
        n varchar(255),
        v varchar(512),
        s_uuid integer,
        primary key (uuid)
    );

    create table ct_to_smart.timertrack (
        uuid integer not null,
        i varchar(38) not null,
        n varchar(32),
        v varchar(38),
        t_uuid integer,
        primary key (uuid)
    );
