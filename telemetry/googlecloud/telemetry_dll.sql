
drop schema if exists telemetry cascade;

create schema telemetry;

create table telemetry.desktop(
    smart_install_key varchar(36),
    last_uploaded_utc timestamp,
    os_name varchar,
    os_version varchar,
    os_arch varchar,
    smart_version varchar,
    primary key (smart_install_key)
);

create table telemetry.desktop_version(
    smart_install_key varchar(36) not null references telemetry.desktop(smart_install_key) on delete cascade,
    plugin_id varchar not null,
    version varchar,
    primary key (smart_install_key, plugin_id)
);

create table telemetry.desktop_stat(
    smart_install_key varchar(36) not null references telemetry.desktop(smart_install_key) on delete cascade,
    key varchar not null,
    month varchar,
    count bigint
);

-- role for the data ingester
create role ingester with login password '<!!!! INGESTER PASSWORD !!!!>';
GRANT USAGE ON SCHEMA telemetry TO ingester;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA telemetry TO ingester;
ALTER DEFAULT PRIVILEGES IN SCHEMA telemetry GRANT ALL PRIVILEGES ON TABLES TO ingester;
REVOKE CREATE ON SCHEMA telemetry FROM ingester;
