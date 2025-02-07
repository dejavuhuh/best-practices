-- export_task
DROP TYPE IF EXISTS export_state CASCADE;
CREATE TYPE export_state AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED');
DROP TABLE IF EXISTS export_task;
CREATE TABLE export_task
(
    id                 bigint GENERATED ALWAYS AS IDENTITY,
    s3_object_key      text         NULL,
    download_file_name text         NOT NULL,
    state              export_state NOT NULL,
    started_at         timestamptz  NULL,
    finished_at        timestamptz  NULL
);

-- person
DROP TABLE IF EXISTS person;
CREATE TABLE person
(
    id         bigint GENERATED ALWAYS AS IDENTITY,
    name       text        NOT NULL,
    age        integer     NOT NULL,
    email      text        NOT NULL,
    phone      text        NOT NULL,
    address    text        NOT NULL,
    city       text        NOT NULL,
    state      text        NOT NULL,
    zip        text        NOT NULL,
    country    text        NOT NULL,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NULL
);
