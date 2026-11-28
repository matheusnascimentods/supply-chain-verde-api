-- invites repository query: existsActiveByEmail.
SELECT EXISTS (
    SELECT 1 FROM invite
    WHERE invited_for = :email AND accepted = FALSE AND expires_at > NOW(6)
);
