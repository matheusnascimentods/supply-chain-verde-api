-- invites repository query: findActiveByTokenHash.
SELECT *
FROM invite
WHERE token_hash = :tokenHash AND accepted = FALSE AND expires_at > NOW(6);
