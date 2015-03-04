    drop table csv_to_smart.attributes;

    create table csv_to_smart.attributes (
        id integer not null,
        n varchar(255),
        primary key (id)
    );
    