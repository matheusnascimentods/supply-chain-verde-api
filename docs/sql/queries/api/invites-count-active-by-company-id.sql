-- invites repository query: countActiveByCompanyId.
SELECT COUNT(*)
FROM invite i
JOIN users u ON u.id = i.invited_by
WHERE u.company_id = :companyId AND i.accepted = FALSE AND i.expires_at > NOW(6);
