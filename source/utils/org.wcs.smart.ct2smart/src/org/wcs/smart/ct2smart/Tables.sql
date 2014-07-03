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

    create table ct_to_smart.timertrack_old (
        uuid integer not null,
        i varchar(38) not null,
        n varchar(32),
        v varchar(38),
        t_uuid integer,
        primary key (uuid)
    );
    
   create table ct_to_smart.timertrack (
        uuid integer not null,
        device_id varchar(38),
        date varchar(12),
        time varchar(12),
        latitude varchar(24),
        longitude varchar(24),
        primary key (uuid)
    );
    

    
    
    create table ct_to_smart.attributes (
        id integer not null GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
        i varchar(38) not null,
        n varchar(255),
        primary key (id)
    );
    