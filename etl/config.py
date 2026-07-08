"""
config.py — Configuración centralizada del pipeline ETL de péptidos.
Ajusta DB_CONFIG con tus credenciales antes de ejecutar.
"""

# ── Conexión a PostgreSQL ─────────────────────────────────────────────────────
DB_CONFIG = {
    "host":     "localhost",
    "port":     5432,
    "dbname":   "peptidos_db",
    "user":     "curador",          # rol con INSERT/UPDATE
    "password": "12345678",
}

# ── URLs de APIs públicas ────────────────────────────────────────────────────
DRAMP_BASE      = "http://dramp.cpu-bioinfor.org/downloads/"
APD_BASE        = "https://aps.unmc.edu/AP/database/query_download.php"
UNIPROT_API     = "https://rest.uniprot.org/uniprotkb/search"
STARPEP_API     = "http://mobiosd-hub.com/starpep/webservices/"   # REST disponible
NCBI_EUTILS     = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils"

# ── Parámetros generales ──────────────────────────────────────────────────────
REQUEST_TIMEOUT   = 30          # segundos
MAX_RETRIES       = 3
RETRY_DELAY       = 2           # segundos entre reintentos
BATCH_SIZE        = 200         # filas por INSERT batch
LOG_LEVEL         = "INFO"      # DEBUG | INFO | WARNING | ERROR

# ── Prioridades de fuentes (menor = más confiable) ───────────────────────────
SOURCE_PRIORITY = {
    "UniProt":   1,
    "DRAMP":     2,
    "APD":       3,
    "StarPep":   4,
    "PeptideAtlas": 2,
}
