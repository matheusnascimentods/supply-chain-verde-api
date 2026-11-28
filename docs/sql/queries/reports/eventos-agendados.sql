-- Estado do agendamento de expiração de certificações.
SELECT event_name, interval_value, interval_field, starts, last_executed, status
FROM information_schema.events
WHERE event_schema = 'supply_chain_verde';
