-- RN-03 (015): lotes ACTIVE do mercado nos últimos 30 dias, por produto. Cobre também o filtro market_id + status
-- da listagem de lotes.
CREATE INDEX idx_batch_market_status_created ON batch (market_id, status, created_at);
