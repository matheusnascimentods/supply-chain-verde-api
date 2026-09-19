INSERT INTO audit_log (
    user_id,
    action,
    affected_table,
    performed_at
) VALUES
((SELECT user_id FROM users WHERE email = 'm.neuer@bayern.com'), 'INSERT', 'supplier', '2026-01-05 09:15:00'),
((SELECT user_id FROM users WHERE email = 'm.neuer@bayern.com'), 'INSERT', 'product', '2026-01-06 10:20:00'),
((SELECT user_id FROM users WHERE email = 'a.davies@bayern.com'), 'STATUS_CHANGE', 'certification', '2026-01-08 14:00:00'),
((SELECT user_id FROM users WHERE email = 'j.kimmich@bayern.com'), 'INSERT', 'batch', '2026-01-12 08:00:00'),
((SELECT user_id FROM users WHERE email = 'j.tah@bayern.com'), 'INSERT', 'chain', '2026-01-14 08:30:00'),
((SELECT user_id FROM users WHERE email = 'j.tah@bayern.com'), 'INSERT', 'transport', '2026-01-17 07:15:00'),
((SELECT user_id FROM users WHERE email = 'm.olise@bayern.com'), 'INSERT', 'carbon_emission', '2026-01-20 16:45:00'),
((SELECT user_id FROM users WHERE email = 'a.davies@bayern.com'), 'UPDATE', 'supplier', '2026-02-02 11:10:00'),
((SELECT user_id FROM users WHERE email = 'm.neuer@bayern.com'), 'INSERT', 'report', '2026-04-01 09:00:00'),
((SELECT user_id FROM users WHERE email = 'h.kane@bayern.com'), 'STATUS_CHANGE', 'certification', '2026-04-03 15:30:00');