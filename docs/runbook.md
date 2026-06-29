# Operations Runbook — DB-per-Service Microservices

## Table of Contents
1. [DB-per-Service Topology](#1-db-per-service-topology)
2. [Deployment Checklist](#2-deployment-checklist)
3. [Backup & Restore](#3-backup--restore)
4. [Migration from Monolith (One-shot)](#4-migration-from-monolith-one-shot)
5. [Rollback Playbook](#5-rollback-playbook)
6. [Event Replay after Incident](#6-event-replay-after-incident)
7. [Health & Observability](#7-health--observability)

---

## 1. DB-per-Service Topology

Each service owns its own schema. No cross-schema queries at runtime.

| Service | Schema | Port | Notes |
|---|---|---|---|
| identity-service | `identity_db` | 8081 | Auth, users, roles |
| crop-catalog-service | `crop_catalog_db` | 8082 | Crops, varieties |
| farm-service | `farm_db` | 8084 | Farms, plots |
| season-service | `season_db` | 8085 | Seasons, tasks, harvests |
| inventory-service | `inventory_db` | 8086 | Lots, warehouses |
| finance-service | `finance_db` | 8087 | Expenses |
| incident-service | `incident_db` | 8088 | Incidents, alerts |
| sustainability-service | `sustainability_db` | 8089 | Soil tests, irrigation |
| marketplace-service | `marketplace_db` | 8090 | Orders, products |
| admin-reporting-service | `admin_reporting_db` | 8091 | Aggregated read models |
| api-gateway | — | 8000 | Routes only |
| ai-service | — | 8083 | Stateless AI calls |

### Cross-schema Dependencies (Runtime)
**None.** All cross-service data flow goes through RabbitMQ events.
The only cross-schema SQL is in:
- `V7__one_shot_backfill.sql` — runs once during migration from monolith
- `AdminReportingBackfillRunner` — disabled by default, opt-in dev only

### Database-per-Service Compose
```yaml
# docker-compose.db-per-service.yml (reference)
services:
  mysql-identity:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: identity_db
  mysql-crop-catalog:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: crop_catalog_db
  mysql-farm:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: farm_db
  # ... repeat per service
```

---

## 2. Deployment Checklist

### Pre-deployment
- [ ] Verify all Flyway migrations ran successfully in each service DB
- [ ] Check no cross-schema connections are required (each service connects to its own DB only)
- [ ] Verify RabbitMQ exchanges and queues are declared in all services
- [ ] Run smoke tests (see Section 6)

### Clean Install
```bash
docker-compose -f docker-compose.yml up -d
```

---

## 3. Backup & Restore

### Backup Strategy
Each service DB must be backed up independently. Schedule daily backups during low-traffic windows.

### Per-Database Backup Commands

```bash
# identity_db
mysqldump -h <host> -u root -p --single-transaction identity_db | gzip > backup/identity_db_$(date +%Y%m%d_%H%M%S).sql.gz

# crop_catalog_db
mysqldump -h <host> -u root -p --single-transaction crop_catalog_db | gzip > backup/crop_catalog_db_$(date +%Y%m%d_%H%M%S).sql.gz

# farm_db
mysqldump -h <host> -u root -p --single-transaction farm_db | gzip > backup/farm_db_$(date +%Y%m%d_%H%M%S).sql.gz

# season_db
mysqldump -h <host> -u root -p --single-transaction season_db | gzip > backup/season_db_$(date +%Y%m%d_%H%M%S).sql.gz

# inventory_db
mysqldump -h <host> -u root -p --single-transaction inventory_db | gzip > backup/inventory_db_$(date +%Y%m%d_%H%M%S).sql.gz

# finance_db
mysqldump -h <host> -u root -p --single-transaction finance_db | gzip > backup/finance_db_$(date +%Y%m%d_%H%M%S).sql.gz

# incident_db
mysqldump -h <host> -u root -p --single-transaction incident_db | gzip > backup/incident_db_$(date +%Y%m%d_%H%M%S).sql.gz

# sustainability_db
mysqldump -h <host> -u root -p --single-transaction sustainability_db | gzip > backup/sustainability_db_$(date +%Y%m%d_%H%M%S).sql.gz

# marketplace_db
mysqldump -h <host> -u root -p --single-transaction marketplace_db | gzip > backup/marketplace_db_$(date +%Y%m%d_%H%M%S).sql.gz

# admin_reporting_db (read model - rebuildable from events)
mysqldump -h <host> -u root -p --single-transaction admin_reporting_db | gzip > backup/admin_reporting_db_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Scheduled Backup Script
```bash
#!/bin/bash
# backup-all.sh — run daily via cron
BACKUP_DIR="/backups/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"
DBS="identity_db crop_catalog_db farm_db season_db inventory_db finance_db incident_db sustainability_db marketplace_db admin_reporting_db"
for db in $DBS; do
  mysqldump -h mysql -u root -p"$MYSQL_ROOT_PASSWORD" \
    --single-transaction --routines --triggers "$db" \
    | gzip > "$BACKUP_DIR/${db}.sql.gz"
  echo "Backed up $db"
done
```

### Restore from Backup

```bash
# 1. Stop the target service to prevent writes
docker-compose stop <service-name>

# 2. Drop and recreate the schema
mysql -h <host> -u root -p -e "DROP DATABASE IF EXISTS <db_name>; CREATE DATABASE <db_name>;"

# 3. Restore from backup
gunzip < backup/<db_name>_20250629_120000.sql.gz | \
  mysql -h <host> -u root -p <db_name>

# 4. Restart the service
docker-compose up -d <service-name>

# 5. Verify
curl http://<host>:<port>/actuator/health
```

### Restore admin_reporting_db (read model — rebuildable)
The `admin_reporting_db` is a **read model** built from events. If lost:
```bash
# Option A: Restore from backup
gunzip < backup/admin_reporting_db_latest.sql.gz | mysql -h <host> -u root -p admin_reporting_db

# Option B: Rebuild from event replay
# 1. Clear processed events (allow re-processing)
mysql -h <host> -u root -p admin_reporting_db -e "TRUNCATE TABLE processed_events;"
# 2. Restart admin-reporting-service — it will replay all outbox events from source services
docker-compose restart admin-reporting-service
```

### Retention
- Keep daily backups for **7 days**
- Keep weekly backups for **4 weeks**
- Keep monthly backups for **12 months**

---

## 4. Migration from Monolith (One-shot)

### When to Run
- First deployment of `admin-reporting-service` into an environment with existing data
- After all source services have been deployed and their data is available

### Prerequisites
- Read access to all source schemas on the same or reachable MySQL instance:
- Target DB: `admin_reporting_db` (Flyway migrations V1–V6 applied)

### Execution
```bash
mysql -h <db-host> -u <user> -p admin_reporting_db \
  < admin-reporting-service/src/main/resources/db/migration/V7__one_shot_backfill.sql
```

### Verification
```sql
-- Check row counts match source
SELECT 'admin_user_summary' tbl, COUNT(*) cnt FROM admin_user_summary
UNION ALL SELECT 'admin_farm_summary', COUNT(*) FROM admin_farm_summary
UNION ALL SELECT 'admin_plot_summary', COUNT(*) FROM admin_plot_summary
UNION ALL SELECT 'admin_season_summary', COUNT(*) FROM admin_season_summary
UNION ALL SELECT 'admin_incident_summary', COUNT(*) FROM admin_incident_summary
UNION ALL SELECT 'admin_inventory_lot_summary', COUNT(*) FROM admin_inventory_lot_summary
UNION ALL SELECT 'admin_marketplace_order_summary', COUNT(*) FROM admin_marketplace_order_summary
UNION ALL SELECT 'admin_documents', COUNT(*) FROM admin_documents;
```

### Post-migration
- Set `admin-reporting.backfill.enabled=false` (already default)
- Disable the `AdminReportingBackfillRunner` bean in production profile
- Start `admin-reporting-service`

### Idempotency
`INSERT IGNORE` is used throughout — running multiple times is safe.

---

## 5. Event Replay after Incident

### Scenario A: Rollback Service Binary (no DB change)

1. Stop the faulty service:
```bash
docker-compose stop <service-name>
```

2. Revert to previous image:
```bash
docker-compose pull <service-name>
docker-compose up -d <service-name>
```

3. Verify:
```bash
curl http://<service-host>:<port>/actuator/health
```

### Scenario B: Rollback Flyway Migration

1. Identify the failed migration version `Vx__*.sql`
2. Roll back to `Vx-1`:
```bash
# If using Flyway with clean schema (rare for production)
flyway -url=jdbc:mysql://<host>:<port>/<schema> \
       -user=<user> -password=<pass> \
       undo -target=Vx-1
```

3. **If `flyway undo` is not available**, restore from backup:
```bash
mysql -h <host> -u <user> -p<pass> <schema> < backup/<schema>_Vx-1.sql
```

---

## 6. Event Replay after Incident

### When to Use
- RabbitMQ queue built up during a service outage
- Consumer processed events with errors and needs to reprocess
- After a rollback, to re-sync read models

### Replay Steps (admin-reporting-service)

1. **Check queue depth first:**
```bash
curl http://localhost:8091/actuator/health | jq '.components.rabbitMQQueue'
```

2. **Option A: Let consumer process normally**
   The consumer will drain the queue automatically. No action needed.

3. **Option B: Reset consumer (if events were processed incorrectly)**
   - Delete processed event records (WARNING: may cause duplicate writes):
   ```sql
   DELETE FROM admin_reporting_db.admin_processed_events
   WHERE processed_at > '2026-01-01';
   ```
   - Reset consumer offset in RabbitMQ (if using classic queue):
   ```bash
   rabbitmqctl purge_queue admin-reporting-service.events
   ```

4. **Option C: Re-publish from source outbox tables** (most reliable)
   For each source service, re-publish from its `outbox_events` table:
   ```sql
   -- Example for inventory-service
   SELECT id, event_type, payload FROM inventory_db.outbox_events
   WHERE created_at > '<last-known-good-timestamp>'
   ORDER BY created_at ASC;
   -- Re-publish each to RabbitMQ
   ```

### Idempotency Guarantee
All event handlers use `ON DUPLICATE KEY UPDATE` / `INSERT IGNORE` / entity ID-based upserts.
Replaying events multiple times is safe and will not create duplicates.

---

## 7. Health & Observability

### Health Endpoints
All services expose:
```
GET /actuator/health          # Overall + component health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/prometheus      # Metrics
GET /actuator/info
```

### Custom Health Indicators (admin-reporting-service)
- `RabbitMQQueueHealthIndicator` — reports `messageCount` and `consumerCount` on `admin-reporting-service.events` queue
  - Returns `DOWN` if `messageCount > 1000` (configurable via `QUEUEDEPTHTHRESHOLD` env)

### Prometheus Scrape Targets
All 12 services are configured in `prometheus/prometheus.yml`.

Scrape: `http://localhost:9090/targets`

### Key Metrics to Monitor

| Metric | Alert If | Service |
|---|---|---|
| `rabbitmq_queue_messages{queue="admin-reporting-service.events"}` | > 1000 | admin-reporting |
| `http_server_requests_seconds_count{status="5xx"}` | > 1% | all |
| `hikaricp_connections_active{pool="HikariPool-1"}` | > 80% of max | all |
| `rabbitmq_connections` | == 0 | all |

### Grafana Dashboards
Import from: `https://grafana.com/grafana/dashboards/` (search Spring Boot)

Key panels:
- Service Error Rate (%5xx)
- RabbitMQ Queue Depth (per queue)
- DB Connection Pool Usage
- JVM Heap / CPU
- Outbox Publish Lag (if publisher services)

### Alert Examples (Prometheus + AlertManager)

```yaml
# alert-rules.yml
groups:
  - name: microservice-alerts
    rules:
      - alert: HighQueueDepth
        expr: rabbitmq_queue_messages{queue=~".*events"} > 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Queue {{ $labels.queue }} has > 1000 messages"

      - alert: ServiceDown
        expr: up{job=~".*-service"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"

      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: warning
```

---

## Quick Reference

```bash
# Check all service health
for port in 8081 8082 8084 8085 8086 8087 8088 8089 8090 8091 8000; do
  curl -s http://localhost:$port/actuator/health | jq -c "{port:$port, status:.status}"
done

# Check RabbitMQ queues
curl -s -u rabbituser:rabbitpass http://localhost:15672/api/queues | \
  jq '.[] | {name, messages, consumers}'

# Restart a specific service
docker-compose restart admin-reporting-service

# Tail logs
docker-compose logs -f admin-reporting-service
```
