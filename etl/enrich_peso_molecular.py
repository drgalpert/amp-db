"""enrich_peso_molecular.py
Rellena peso_molecular para péptidos que lo tienen NULL.

Estrategia por prioridad:
  1. Si tiene uniprot_id → consulta UniProt REST (dato experimental)
  2. Si no              → calcula desde la secuencia (estimación)

Uso:
    python enrich_peso_molecular.py
    python enrich_peso_molecular.py --solo-calculo   # solo estrategia 2
"""

import argparse
import logging
import time
from db_utils import get_conn
from http_utils import get_json

log = logging.getLogger(__name__)

# Pesos monoisotópicos de residuo (Da) — escala estándar
PESOS_RESIDUO = {
    'A': 71.03711,  'R': 156.10111, 'N': 114.04293, 'D': 115.02694,
    'C': 103.00919, 'E': 129.04259, 'Q': 128.05858, 'G': 57.02146,
    'H': 137.05891, 'I': 113.08406, 'L': 113.08406, 'K': 128.09496,
    'M': 131.04049, 'F': 147.06841, 'P': 97.05276,  'S': 87.03203,
    'T': 101.04768, 'W': 186.07931, 'Y': 163.06333, 'V': 99.06841,
}
AGUA = 18.01056  # se suma una vez por la cadena lineal


def calcular_pm(seq: str) -> float | None:
    """Peso molecular estimado (Da) sumando residuos + agua."""
    total = sum(PESOS_RESIDUO.get(aa, 0.0) for aa in seq.upper())
    if total == 0:
        return None
    return round(total + AGUA, 2)


def enriquecer_desde_uniprot(conn) -> tuple[int, int]:
    """Consulta UniProt para cada péptido con uniprot_id y pm NULL."""
    with conn.cursor() as cur:
        cur.execute("""
            SELECT id, uniprot_id
            FROM "pÉptido"
            WHERE peso_molecular IS NULL
              AND uniprot_id IS NOT NULL
        """)
        filas = cur.fetchall()

    log.info("Péptidos con uniprot_id sin peso_molecular: %d", len(filas))
    ok = fail = 0

    for pid, uid in filas:
        try:
            data = get_json(
                f"https://rest.uniprot.org/uniprotkb/{uid}",
                params={"fields": "sequence", "format": "json"}
            )
            pm = (data.get("sequence") or {}).get("molWeight")
            if pm:
                with conn.cursor() as cur:
                    cur.execute(
                        'UPDATE "pÉptido" SET peso_molecular = %s WHERE id = %s',
                        (pm, pid)
                    )
                ok += 1
            else:
                fail += 1
        except Exception as e:
            log.debug("UniProt error %s: %s", uid, e)
            fail += 1
        time.sleep(0.2)  # respetar rate-limit

    conn.commit()
    log.info("UniProt → actualizados: %d  |  sin dato: %d", ok, fail)
    return ok, fail


def enriquecer_por_calculo(conn) -> int:
    """Calcula pm desde secuencia para los que aún lo tienen NULL."""
    with conn.cursor() as cur:
        cur.execute("""
            SELECT id, secuencia
            FROM "pÉptido"
            WHERE peso_molecular IS NULL
        """)
        filas = cur.fetchall()

    log.info("Péptidos restantes sin peso_molecular: %d → calculando", len(filas))
    ok = 0

    for pid, seq in filas:
        pm = calcular_pm(seq)
        if pm:
            with conn.cursor() as cur:
                cur.execute(
                    'UPDATE "pÉptido" SET peso_molecular = %s WHERE id = %s',
                    (pm, pid)
                )
            ok += 1

    conn.commit()
    log.info("Calculados desde secuencia: %d", ok)
    return ok


def main():
    logging.basicConfig(level=logging.INFO,
                        format="%(asctime)s [%(levelname)s] %(message)s")
    parser = argparse.ArgumentParser()
    parser.add_argument("--solo-calculo", action="store_true",
                        help="Saltar UniProt y calcular todo desde secuencia")
    args = parser.parse_args()

    with get_conn() as conn:
        if not args.solo_calculo:
            enriquecer_desde_uniprot(conn)
        enriquecer_por_calculo(conn)

    log.info("✅ Enriquecimiento de peso molecular completado")


if __name__ == "__main__":
    main()