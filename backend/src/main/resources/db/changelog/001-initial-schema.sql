--liquibase formatted sql

--changeset bttcode:001-onboarding-application
create table onboarding_application (
    id              uuid primary key,
    version         bigint                   not null,
    applicant_sub   varchar(64)              not null,
    first_name      varchar(100)             not null,
    last_name       varchar(100)             not null,
    email           varchar(254)             not null,
    birth_date      date                     not null,
    nationality     varchar(2)               not null,
    street          varchar(200)             not null,
    postal_code     varchar(4)               not null,
    city            varchar(100)             not null,
    product         varchar(32)              not null,
    start_date      date                     not null,
    status          varchar(16)              not null,
    check_status    varchar(16)              not null,
    check_reference varchar(64),
    checked_at      timestamp with time zone,
    document_name   varchar(255),
    submitted_at    timestamp with time zone not null
);
create index ix_application_applicant on onboarding_application (applicant_sub, submitted_at desc);
create index ix_application_status on onboarding_application (status, submitted_at);

--changeset bttcode:002-application-decision
create table application_decision (
    id             uuid primary key,
    application_id uuid                     not null references onboarding_application (id),
    caseworker     varchar(100)             not null,
    decision       varchar(16)              not null,
    comment        varchar(1000),
    decided_at     timestamp with time zone not null,
    -- four-eyes principle, enforced by the database as well as by the domain model
    constraint uq_decision_caseworker unique (application_id, caseworker)
);

--changeset bttcode:003-application-document
create table application_document (
    application_id uuid primary key references onboarding_application (id),
    file_name      varchar(255)             not null,
    content_type   varchar(100)             not null,
    size_bytes     bigint                   not null,
    data           bytea                    not null,
    uploaded_at    timestamp with time zone not null
);

--changeset bttcode:004-outbox-event
create table outbox_event (
    id              uuid primary key,
    aggregate_id    uuid                     not null,
    type            varchar(64)              not null,
    status          varchar(16)              not null,
    attempts        integer                  not null,
    next_attempt_at timestamp with time zone not null,
    last_error      varchar(500),
    created_at      timestamp with time zone not null,
    processed_at    timestamp with time zone
);
-- partial index: the poller only ever looks at pending rows
create index ix_outbox_due on outbox_event (next_attempt_at) where status = 'PENDING';
create index ix_outbox_aggregate on outbox_event (aggregate_id);
