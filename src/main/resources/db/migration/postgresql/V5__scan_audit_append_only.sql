create function scan_audit_reject_change() returns trigger
    language plpgsql as
$$
begin
    raise exception 'scan_audit is append-only, % is not allowed', tg_op
        using errcode = 'insufficient_privilege';
end;
$$;

create trigger scan_audit_no_update_or_delete
    before update or delete on scan_audit
    for each row execute function scan_audit_reject_change();

create trigger scan_audit_no_truncate
    before truncate on scan_audit
    for each statement execute function scan_audit_reject_change();
