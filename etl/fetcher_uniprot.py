"""
fetcher_uniprot.py — Descarga e ingesta péptidos desde UniProt REST API.

Consultas relevantes para péptidos bioactivos:
  - length:[5 TO 50] AND reviewed:true AND keyword:KW-0929   (antimicrobianos)
  - length:[5 TO 50] AND ft_peptide:*                        (con anotación PEPTIDE)
  - database:dramp OR database:apd3                           (cruzados con otras BDs)

Uso:
    python fetcher_uniprot.py --query "length:[5 TO 50] AND keyword:KW-0929"
    python fetcher_uniprot.py --ids P12345 Q9Y2Z0
    python fetcher_uniprot.py --preset antimicrobial
"""

import argparse
import json
import logging
import re
import time

from config import SOURCE_PRIORITY
from db_utils import (
    ensure_fuente, fetch_or_create, get_conn,
    insert_relation, upsert_one, print_stats,
)
from http_utils import paginate_uniprot, get_json

log = logging.getLogger(__name__)

VALID_AA = re.compile(r'^[ACDEFGHIKLMNPQRSTVWY]+$')

# Campos a solicitar a UniProt
FIELDS = (
    "accession,id,protein_name,sequence,length,organism_name,organism_id,gene_names"
)

# Búsquedas preconfiguradas
PRESETS = {
    "antimicrobial": (
        "length:[5 TO 50] AND keyword:KW-0929 AND reviewed:true"
    ),
    "antiviral": (
        "length:[5 TO 50] AND keyword:KW-0936 AND reviewed:true"
    ),
    "anticancer": (
        "length:[5 TO 50] AND keyword:KW-1028 AND reviewed:true"
    ),
    "all_small": (
        "length:[5 TO 50] AND reviewed:true AND ft_peptide:*"
    ),
}


# ── Transformación de respuesta UniProt → registro interno ────────────────────

def _extraer_secuencia(entry: dict) -> str:
    return (entry.get("sequence") or {}).get("value", "").upper().strip()


def _extraer_xref(entry: dict, db: str) -> str:
    """Extrae el primer ID de una base de datos cruzada."""
    for xref in entry.get("uniProtKBCrossReferences", []):
        if xref.get("database") == db:
            return xref.get("id", "")
    return ""


def _extraer_pubmeds(entry: dict) -> list[str]:
    refs = entry.get("references", [])
    pmids = []
    for ref in refs:
        citation = ref.get("citation", {})
        pubmed = citation.get("citationCrossReferences", [])
        for p in pubmed:
            if p.get("database") == "PubMed":
                pmids.append(p.get("id", ""))
    return [p for p in pmids if p]


def _extraer_pdbs(entry: dict) -> list[str]:
    return [
        xref.get("id", "")
        for xref in entry.get("uniProtKBCrossReferences", [])
        if xref.get("database") == "PDB"
    ]


def _extraer_nombre(entry: dict) -> str:
    prot = entry.get("proteinDescription", {})
    rec  = prot.get("recommendedName", {})
    full = rec.get("fullName", {}).get("value", "")
    if not full:
        sub = prot.get("submissionNames", [{}])
        full = sub[0].get("fullName", {}).get("value", "") if sub else ""
    return full[:200]

def _extraer_nombres_alternativos(entry: dict) -> list[str]:

    nombres = set()

    prot = entry.get("proteinDescription", {})

    # ── alternativeNames ────────────────────────────────────

    rec = prot.get("recommendedName", {})

    for alt in rec.get("alternativeNames", []):

        val = alt.get("fullName", {}).get("value")

        if val:
            nombres.add(val[:200])

    # ── shortNames ──────────────────────────────────────────

    for short in rec.get("shortNames", []):

        val = short.get("value")

        if val:
            nombres.add(val[:200])

    # ── submissionNames ─────────────────────────────────────

    for sub in prot.get("submissionNames", []):

        val = sub.get("fullName", {}).get("value")

        if val:
            nombres.add(val[:200])

    return list(nombres)


def _extraer_organismo(entry: dict) -> tuple[str, str]:
    """Retorna (nombre_científico, reino)."""
    org = entry.get("organism", {})
    nombre = org.get("scientificName", "")
    lineage = org.get("lineage", [])
    reino = lineage[0] if lineage else None
    return nombre, reino

def normalizar_entrada(entry: dict) -> dict:
    """Convierte un resultado JSON de UniProt al formato interno."""
    sec = _extraer_secuencia(entry)
    org_nombre, org_reino = _extraer_organismo(entry)
    return {
        "secuencia":           sec,
        "longitud":            len(sec),
        "nombre_principal":    _extraer_nombre(entry),
        "uniprot_id":          entry.get("primaryAccession", "")[:20],
        "dramp_id":            _extraer_xref(entry, "DRAMP")[:20],
        "peso_molecular":      (entry.get("sequence") or {}).get("molWeight"),
        "carga_neta":          None,   # UniProt no lo provee directamente
        "hidrofobicidad":      None,
        "es_natural":          True,   # Todo en UniProt/SwissProt es natural
        "estado_verificacion": (
            "curado_literatura"
            if entry.get("entryType", "").startswith("Swiss")
            else "predicho"
        ),
        "_organismo_fuente":   org_nombre,
        "_organismo_reino":    org_reino,
        "_pubmeds":            _extraer_pubmeds(entry),
        "_pdbs":               _extraer_pdbs(entry),
        "_nombres_alt":        _extraer_nombres_alternativos(entry),
        "_target":             [],
    }


# ── Ingesta ───────────────────────────────────────────────────────────────────

def ingestar_registros(records: list[dict]) -> None:
    total = insertados = omitidos = 0

    with get_conn() as conn:
        ensure_fuente(conn, "UniProt",
                      url="https://www.uniprot.org",
                      version="2024_01")

        for rec in records:
            total += 1
            sec = rec.get("secuencia", "")
            if not sec or not VALID_AA.match(sec):
                omitidos += 1
                continue

            # ── Organismo fuente ───────────────────────────────────────────
            organismo_fuente_id = None
            org_src = str(rec.get("_organismo_fuente", "")).strip()
            if org_src:
                organismo_fuente_id = fetch_or_create(
                    conn, "fuente_organismo",
                    {"nombre_cientifico": org_src[:200],
                     "reino": rec.get("_organismo_reino")},
                    "nombre_cientifico",
                )

            # ── Péptido ────────────────────────────────────────────────────
            peptido_data = {
                "secuencia":           sec,
                "nombre_principal":    rec["nombre_principal"] or None,
                "longitud":            rec["longitud"],
                "peso_molecular":      rec.get("peso_molecular"),
                "carga_neta":          None,
                "hidrofobicidad":      None,
                "es_natural":          True,
                "estado_verificacion": rec["estado_verificacion"],
                "uniprot_id":          rec["uniprot_id"] or None,
                "dramp_id":            rec["dramp_id"] or None,
                "organismo_fuente_id": organismo_fuente_id,
            }
            result = upsert_one(conn, "pÉptido", peptido_data, "secuencia")
            if not result:
                omitidos += 1
                continue
            pid = result["id"]
            insertados += 1

            # ── Fuente ────────────────────────────────────────────────────
            insert_relation(conn, "peptido_fuente",
                            "peptido_id", pid,
                            "fuente_nombre", "UniProt",
                            extras={"prioridad": SOURCE_PRIORITY["UniProt"]})
            
            # ── Nombres alternativos ──────────────────────────────────────────
            for alt in rec.get("_nombres_alt", []):
                if not alt or len(alt) < 4:
                    continue
                if alt.lower() == (rec["nombre_principal"] or "").lower():
                    continue
                with conn.cursor() as cur:
                    cur.execute("""
                        INSERT INTO nombre_alternativo (nombre, peptido_id)
                        VALUES (%s, %s)
                        ON CONFLICT (nombre, peptido_id) DO NOTHING
                    """, (alt[:200], pid))

            # ── Publicaciones ──────────────────────────────────────────────
            for pmid in rec.get("_pubmeds", []):
                if pmid:
                    upsert_one(conn, "publicaciÓn", {"pmid": pmid}, "pmid")
                    insert_relation(conn, "peptido_publicacion",
                                    "peptido_id", pid,
                                    "publicacion_pmid", pmid)

            # ── Estructuras PDB ────────────────────────────────────────────
            pdbs = rec.get("_pdbs", [])
            if pdbs:
                # Solo guardamos la primera (esquema: 1 estructura por péptido)
                upsert_one(conn, "estructura",
                           {"pdb_id": pdbs[0][:10], "peptido_id": pid},
                           "peptido_id")

        conn.commit()

    log.info("UniProt — total:%d  insertados:%d  omitidos:%d",
             total, insertados, omitidos)


# ── Consultas ──────────────────────────────────────────────────────────────────

def fetch_by_query(query: str) -> list[dict]:
    """Descarga todos los resultados para una query UniProt."""
    records = []
    for page in paginate_uniprot(query, FIELDS):
        for entry in page:
            r = normalizar_entrada(entry)
            if r["secuencia"]:
                records.append(r)
    log.info("UniProt query '%s': %d registros válidos", query, len(records))
    return records


def fetch_by_ids(ids: list[str]) -> list[dict]:
    """Descarga entradas individuales de UniProt por accession."""
    from http_utils import get_json
    records = []
    for acc in ids:
        try:
            data = get_json(
                f"https://rest.uniprot.org/uniprotkb/{acc}",
                params={"fields": FIELDS, "format": "json"}
            )
            r = normalizar_entrada(data)
            if r["secuencia"]:
                records.append(r)
        except Exception as e:
            log.warning("UniProt error para %s: %s", acc, e)
        time.sleep(0.2)
    return records


# ── CLI ───────────────────────────────────────────────────────────────────────

def main():
    logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s [%(levelname)s] %(message)s")
    parser = argparse.ArgumentParser(
        description="Ingesta péptidos desde UniProt a peptidos_db"
    )
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--query",  metavar="QUERY",
                       help="Query UniProt (ej. 'length:[5 TO 50] AND keyword:KW-0929')")
    group.add_argument("--preset", choices=PRESETS.keys(),
                       help="Consulta predefinida")
    group.add_argument("--ids",    nargs="+", metavar="ACCESSION",
                       help="Accessions de UniProt (ej. P12345)")
    parser.add_argument("--stats", action="store_true")
    args = parser.parse_args()

    if args.query:
        records = fetch_by_query(args.query)
    elif args.preset:
        log.info("Usando preset: %s → %s", args.preset, PRESETS[args.preset])
        records = fetch_by_query(PRESETS[args.preset])
    else:
        records = fetch_by_ids(args.ids)

    ingestar_registros(records)

    if args.stats:
        with get_conn() as conn:
            print_stats(conn)


if __name__ == "__main__":
    main()
