# pg_cron não vem na imagem oficial; a variante alpine não tem o pacote.
FROM postgres:17
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-17-cron \
    && rm -rf /var/lib/apt/lists/*
CMD ["postgres", "-c", "shared_preload_libraries=pg_cron", "-c", "cron.database_name=supply_chain_verde"]
