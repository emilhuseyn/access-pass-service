create table access_pass (
    id                uuid                     primary key,
    holder_id         bigint                   not null,
    policy_id         bigint                   not null,
    status            varchar(16)              not null,
    valid_from        timestamp with time zone not null,
    valid_until       timestamp with time zone not null,
    max_uses          integer,
    use_count         integer                  not null default 0,
    key_id            varchar(32)              not null,
    nonce             varchar(32)              not null,
    issued_at         timestamp with time zone not null,
    issued_by         varchar(64)              not null,
    revoked_at        timestamp with time zone,
    revoked_by        varchar(64),
    revocation_reason varchar(200),
    version           bigint                   not null,
    constraint fk_access_pass_holder foreign key (holder_id) references holder (id),
    constraint fk_access_pass_policy foreign key (policy_id) references pass_policy (id),
    constraint ck_access_pass_status check (status in ('ACTIVE', 'REVOKED')),
    constraint ck_access_pass_validity check (valid_until > valid_from),
    constraint ck_access_pass_use_count check (use_count >= 0 and (max_uses is null or use_count <= max_uses)),
    constraint ck_access_pass_revocation check (
        (status = 'ACTIVE' and revoked_at is null) or (status = 'REVOKED' and revoked_at is not null))
);

create index ix_access_pass_holder on access_pass (holder_id, issued_at);
create index ix_access_pass_policy on access_pass (policy_id);
