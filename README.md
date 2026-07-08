# Antimicrobial Peptide Manager

A relational database management system for antimicrobial peptides: a PostgreSQL schema, ETL processes that integrate data from external sources (DRAMP, UniProt), and a JavaFX desktop application with role-based access control.

## Repository structure

```text
├── database/            Database schema and backup
│   ├── create_peptidos_db.sql
│   └── backup_peptidos_db.sql
├── etl/                 Extract, transform and load scripts
│   ├── fetcher_uniprot.py
│   ├── ingest_dramp.py
│   ├── enrich_pdb.py
│   ├── enrich_pubmed.py
│   ├── enrich_peso_molecular.py
│   ├── calculate_properties.py
│   ├── db_utils.py
│   ├── http_utils.py
│   ├── config.py
│   └── requirements.txt
└── app/                  Desktop application (JavaFX + Maven)
    ├── pom.xml
    └── src/
```

## Requirements

- Java 17+
- Maven 3.8+
- PostgreSQL 13+
- Python 3.10+ (only needed to run the scripts in `etl/`)

## 1. Database

Create the database and the required roles (as the `postgres` superuser):

```bash
createdb peptidos_db

psql -d peptidos_db -c "CREATE ROLE app_login WITH LOGIN PASSWORD '12345678';"
psql -d peptidos_db -c "CREATE ROLE administrador WITH LOGIN PASSWORD '12345678' SUPERUSER;"
psql -d peptidos_db -c "CREATE ROLE curador WITH LOGIN PASSWORD '12345678';"
psql -d peptidos_db -c "CREATE ROLE consultor WITH LOGIN PASSWORD '12345678';"
```

Restore the backup (it already contains the full schema and the data loaded by the ETL scripts):

```bash
psql -U postgres -d peptidos_db -f database/backup_peptidos_db.sql
```

> If you'd rather start from scratch instead of the backup, use `database/create_peptidos_db.sql` and then populate the database with the scripts in `etl/`.

## 2. ETL scripts (optional — the backup already includes the data)

> `ingest_dramp.py` expects DRAMP's `.xlsx` files inside `etl/data/` (e.g. `general_amps.xlsx`, `Anticancer_amps.xlsx`, `stability_amps.xlsx`, etc.). That folder is not included in the repository — download them manually from [dramp.cpu-bioinfor.org](http://dramp.cpu-bioinfor.org/) and place them there before running the script.

```bash
cd etl
pip install -r requirements.txt
python fetcher_uniprot.py --preset antimicrobial --stats
python ingest_dramp.py --all
python enrich_peso_molecular.py
python enrich_pdb.py
python enrich_pubmed.py
python calculate_properties.py
```

Update the connection credentials in `etl/config.py` (`DB_CONFIG`) before running the scripts.

## 3. Desktop application

Set the host, port and per-role passwords in `app/src/main/resources/config/db.properties`, then:

```bash
cd app
mvn clean javafx:run
```

### Test accounts

The application uses its own accounts (the `USUARIO_APP` table), independent from the PostgreSQL roles. These accounts already exist in the backup to test each role:

| Username  | Password  | Role          | Can edit                |
|-----------|-----------|---------------|-------------------------|
| admin     | admin123  | administrator | Yes                     |
| curador   | cura123   | curator       | Yes                     |
| consultor | consul123 | viewer        | No (read-only + export) |

With the **consultor** (viewer) account you can verify that the New/Edit/Deactivate buttons are disabled, and that the main list view and the Excel export still show the full enriched information (alternative names, target organisms, activities) thanks to the `vista_peptido_completo` and `vista_actividad_peptido` views.

## Architecture

The application is organized in layers:

```text
model/       POJOs representing the database tables
dao/         Data access (JDBC + PreparedStatement)
service/     Business rules and validation before touching the DB
security/    Active session (user + JDBC connection under their role)
ui/          MainApp + controllers + FXML (Login, List, Detail)
```

Edit permissions are enforced by the PostgreSQL engine itself through `GRANT`/`REVOKE` on each role: the UI disabling certain buttons is only a usability improvement — actual security does not depend on the interface.

The peptide detail form organizes related information (structure, clinical stability, modifications, target organisms, activities, publications and sources) into independent tabs, each with its own FXML controller following the same `cargar(peptidoId)` / `guardar(peptidoId)` pattern.
