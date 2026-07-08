"""
ingest_dramp.py — ETL COMPLETO de DRAMP

Uso:
    python ingest_dramp.py --all
    python ingest_dramp.py --file stability
"""

import pandas as pd
import logging
import argparse
import re
from pathlib import Path

from config import SOURCE_PRIORITY
from db_utils import (
    get_conn, upsert_one, insert_relation, fetch_or_create,
    ensure_fuente, print_stats
)

log = logging.getLogger(__name__)

# ====================== CONFIG ======================
DATA_DIR = Path("data")

FILES = {
    "general":    DATA_DIR / "general_amps.xlsx",
    "anticancer": DATA_DIR / "Anticancer_amps.xlsx",
    "antiviral":  DATA_DIR / "Antiviral_amps.xlsx",
    "antifungal": DATA_DIR / "Antifungal_amps.xlsx",
    "clinical":   DATA_DIR / "clinical_amps.xlsx",
    "stability":  DATA_DIR / "stability_amps.xlsx",
}

# ====================== HELPERS ======================
def clean_seq(seq):
    if pd.isna(seq):
        return None
    return str(seq).strip().upper().replace(" ", "")

def map_estado_clinico(valor):
    if pd.isna(valor):
        return 'preclínica'
    v = str(valor).lower()
    if 'phase iii' in v or 'fase3' in v:
        return 'fase3' if 'failure' not in v else 'retirado'
    elif 'phase ii' in v or 'fase2' in v:
        return 'fase2'
    elif 'phase i' in v or 'fase1' in v:
        return 'fase1'
    elif any(x in v for x in ['preclinical', 'pre-clin']):
        return 'preclínica'
    elif any(x in v for x in ['approved', 'in market']):
        return 'aprobado'
    elif any(x in v for x in ['failure', 'suspended', 'retirado']):
        return 'retirado'
    return 'preclínica'


def ingest_file(filepath: Path, file_type: str = None):
    df = pd.read_excel(filepath)
    log.info(f"Procesando {len(df)} filas → {filepath.name}")

    with get_conn() as conn:
        ensure_fuente(conn, "DRAMP", "http://dramp.cpu-bioinfor.org/", "v4+")

        for _, row in df.iterrows():
            seq = clean_seq(row.get('Sequence') or row.get('Seq'))
            if not seq or len(seq) < 5:
                continue

            if not re.match(r'^[ACDEFGHIKLMNPQRSTVWYBXZ]+$', seq):
                continue

            # === 1. Péptido principal ===
            nombre = row.get('Name') or row.get('DRAMP_ID') or f"DRAMP{row.get('DRAMP_ID','')}"
            peptido_data = {
                "secuencia": seq,
                "nombre_principal": str(nombre)[:200],
                "longitud": int(row.get('Sequence_Length') or len(seq)),
                "dramp_id": str(row.get('DRAMP_ID', ''))[:20],
                "es_natural": "synthetic" not in str(row.get('Source', '')).lower(),
                "estado_verificacion": "curado_literatura",
            }

            result = upsert_one(conn, '"pÉptido"', peptido_data, "secuencia")
            if not result:
                continue
            pid = result["id"]

            # Fuente DRAMP
            insert_relation(conn, "peptido_fuente", "peptido_id", pid,
                            "fuente_nombre", "DRAMP", {"prioridad": 2})

            # === 2. Organismo Fuente ===
            org_name = str(row.get('Source') or row.get('Organism', '')).strip()[:200]
            if org_name:
                org_id = fetch_or_create(conn, "fuente_organismo",
                    {"nombre_cientifico": org_name}, "nombre_cientifico")
                with conn.cursor() as cur:
                    cur.execute('UPDATE "pÉptido" SET organismo_fuente_id = %s WHERE id = %s', (org_id, pid))

            # === 3. Estructura ===
            pdb = str(row.get('PDB_ID', '')).strip()
            if pdb and pdb not in ['None', 'nan']:
                upsert_one(conn, "estructura", {
                    "peptido_id": pid,
                    "pdb_id": pdb[:10],
                    "tipo_estructura": str(row.get('Structure', ''))[:50] or "Alpha helix",
                    "ciclizacion": str(row.get('Linear/Cyclic/Branched', ''))[:30]
                }, "peptido_id")

            # === 4. Publicaciones ===
            pubmed_raw = str(row.get('Pubmed_ID') or row.get('Reference', ''))
            for pmid in re.findall(r'\d+', pubmed_raw):
                if pmid:
                    upsert_one(conn, '"publicaciÓn"', {"pmid": pmid}, "pmid")
                    insert_relation(conn, "peptido_publicacion", "peptido_id", pid, "publicacion_pmid", pmid)

            # === 5. Modificaciones ===
            for campo, col in [("N-terminal", 'N-terminal_Modification'),
                            ("C-terminal", 'C-terminal_Modification'),
                            ("Other",      'Other_Modifications')]:
                val = str(row.get(col, '')).strip()
                if not val or val.lower() in ['free', 'none', 'not included yet', 'nan']:
                    continue
                with conn.cursor() as cur:
                    cur.execute("""
                        INSERT INTO modificacion_postraduccional (tipo, posicion)
                        VALUES (%s, %s)
                        ON CONFLICT (tipo, posicion) DO NOTHING
                        RETURNING id
                    """, (campo, val[:200]))
                    row_mod = cur.fetchone()
                    if not row_mod:
                        cur.execute("""
                            SELECT id FROM modificacion_postraduccional
                            WHERE tipo = %s AND posicion = %s
                        """, (campo, val[:200]))
                        row_mod = cur.fetchone()
                if row_mod:
                    insert_relation(conn, "peptido_modificacion",
                                    "peptido_id", pid, "modificacion_id", row_mod[0])

            # === 6. Actividades ===
            act = str(row.get('Activity', '')).lower()

            if file_type == "anticancer" or any(x in act for x in ["anticancer", "antitumor", "cancer"]):
                aid = fetch_or_create(conn, "actividad_anticancer",
                    {"linea_celular": "General", "tipo_cancer": "General"},
                    "linea_celular")
                insert_relation(conn, "peptido_anticancer", "peptido_id", pid, "anticancer_id", aid)

            if file_type == "antiviral" or "antiviral" in act:
                aid = fetch_or_create(conn, "actividad_antiviral",
                    {"nombre_virus": "General", "familia_viral": "General"},
                    "nombre_virus")
                insert_relation(conn, "peptido_antiviral", "peptido_id", pid, "antiviral_id", aid)

            if file_type == "antifungal" or any(x in act for x in ["antifungal", "fungi", "candida"]):
                aid = fetch_or_create(conn, "actividad_antifungica",
                    {"nombre_hongo": "General"}, "nombre_hongo")
                insert_relation(conn, "peptido_antifungico", "peptido_id", pid, "antifungico_id", aid)

            # === 7. Organismos blanco + MIC ===
            target_raw = str(row.get('Target_Organism', '')).strip()
            VACIOS = {'not found', 'not included yet', 'nan', 'none',
                    'no mics found in dramp database', ''}

            if target_raw.lower() not in VACIOS:
                # Quitar prefijos [Ref.PMID] y separar bloques ;##
                target_raw = re.sub(r'^\[Ref\.\d+\]\s*', '', target_raw)
                bloques = re.split(r'\s*;##\s*', target_raw)

                gram_actual = None
                for bloque in bloques:
                    # Detectar y quitar encabezado Gram
                    m_gram = re.match(
                        r'(Gram-positive bacteria|Gram-negative bacteria|Fungi|'
                        r'Virus|Cancer)[:\s]*(.*)$', bloque, re.I | re.S
                    )
                    if m_gram:
                        encabezado = m_gram.group(1).lower()
                        bloque     = m_gram.group(2).strip()
                        if 'positive' in encabezado:
                            gram_actual = 'positive'
                        elif 'negative' in encabezado:
                            gram_actual = 'negative'
                        else:
                            gram_actual = None

                    # Dividir por coma solo donde empieza nombre en mayúscula
                    entidades = re.split(r',\s*(?=[A-Z])', bloque)

                    for ent in entidades:
                        ent = ent.strip().rstrip('.')
                        if not ent or len(ent) < 4:
                            continue

                        # Extraer MIC — acepta =, >, <, rangos y unidades variadas
                        mic_valor = None
                        mic_unidad = None
                        m_mic = re.search(
                            r'MIC\s*([=<>≤≥]+)\s*([\d.,]+(?:\s*[–\-]\s*[\d.,]+)?)'
                            r'\s*([μµu]?g/m[lL]|mg/L|[μµu]M|nM)',
                            ent, re.I
                        )
                        if m_mic:
                            valor_raw = m_mic.group(2).strip().replace(',', '.')
                            # Si es un rango (ej. "10-20"), se toma el promedio como estimación puntual.
                            # NOTA: el esquema actual no tiene columna para el operador (=, <, >, ≤, ≥);
                            # ese matiz se pierde al pasar de JSONB a columnas numéricas planas.
                            partes = re.split(r'[–\-]', valor_raw)
                            try:
                                numeros = [float(p) for p in partes if p]
                                mic_valor = round(sum(numeros) / len(numeros), 3) if numeros else None
                            except ValueError:
                                mic_valor = None
                            mic_unidad = (m_mic.group(3).strip()
                                          .replace('ug', 'μg').replace('µg', 'μg'))

                        # Nombre = texto antes del primer paréntesis
                        nombre_org = re.sub(r'\s*\(.*', '', ent).strip()
                        # Quitar prefijos tipo "ATCC 25922" solos sin nombre
                        if not nombre_org or len(nombre_org) < 4:
                            continue

                        org_id = fetch_or_create(
                            conn, "organismo_blanco",
                            {"nombre_cientifico": nombre_org[:200],
                            "gram":              gram_actual,
                            "categoria":         "bacteria"},
                            "nombre_cientifico"
                        )
                        if org_id:
                            insert_relation(
                                conn, "peptido_organismo",
                                "peptido_id", pid, "organismo_id", org_id,
                                {"mic_valor": mic_valor, "mic_unidad": mic_unidad}
                            )

            # === 8. Estabilidad (solo en archivo stability) ===
            if file_type == "stability":
                half = str(row.get('half_life', '')).strip()
                suero = None
                m = re.search(r'(\d+\.?\d*)', half)
                if m:
                    try: suero = float(m.group(1))
                    except: pass

                data_stab = {
                    "peptido_id": pid,
                    "estabilidad_suero": suero,
                    "estado_clinico": map_estado_clinico(row.get('Stage_of_development')),
                    "toxicidad_in_vivo": str(row.get('Hemolytic_activity', ''))[:1000],
                }
                upsert_one(conn, "estabilidad_clinica", data_stab, "peptido_id")

    log.info(f"✅ Finalizado {filepath.name}")


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    
    parser = argparse.ArgumentParser(description="ETL Completo DRAMP")
    parser.add_argument("--all", action="store_true", help="Procesar todos los archivos")
    parser.add_argument("--file", choices=FILES.keys(), help="Procesar solo un archivo")
    args = parser.parse_args()

    if args.all or not args.file:
        for name, fname in FILES.items():
            path = Path(fname)
            if path.exists():
                ingest_file(path, name)
    else:
        path = Path(FILES[args.file])
        if path.exists():
            ingest_file(path, args.file)

    with get_conn() as conn:
        print_stats(conn)

    log.info("🎉 ETL DRAMP COMPLETADO")