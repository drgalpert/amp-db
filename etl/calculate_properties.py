"""calculate_properties.py — Cálculo de carga neta e hidrofobicidad"""

import logging
from db_utils import get_conn

log = logging.getLogger(__name__)

# Escalas comunes
HYDROPHOBICITY_SCALE = {
    'A': 0.62, 'C': 0.29, 'D': -0.90, 'E': -0.74, 'F': 1.19, 'G': 0.48,
    'H': -0.40, 'I': 1.38, 'K': -1.50, 'L': 1.06, 'M': 0.64, 'N': -0.78,
    'P': 0.12, 'Q': -0.85, 'R': -2.53, 'S': -0.18, 'T': -0.05, 'V': 1.08,
    'W': 0.81, 'Y': 0.26, 'X': 0.0, 'Z': -0.74
}

def calculate_net_charge(seq: str) -> float:
    """Cálculo aproximado de carga neta a pH 7"""
    positive = seq.count('K') + seq.count('R') + seq.count('H')
    negative = seq.count('D') + seq.count('E')
    return positive - negative

def calculate_hydrophobicity(seq: str) -> float:
    """Hidrofobicidad promedio usando escala Kyte-Doolittle simplificada"""
    if not seq:
        return None
    scores = [HYDROPHOBICITY_SCALE.get(aa, 0.0) for aa in seq]
    return round(sum(scores) / len(scores), 3)


def calculate_properties():
    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute('SELECT id, secuencia FROM "pÉptido" WHERE carga_neta IS NULL OR hidrofobicidad IS NULL')
            rows = cur.fetchall()

            log.info(f"Calculando propiedades para {len(rows)} péptidos...")

            updated = 0
            for pid, seq in rows:
                try:
                    carga = calculate_net_charge(seq)
                    hydro = calculate_hydrophobicity(seq)

                    cur.execute("""
                        UPDATE "pÉptido"
                        SET carga_neta = %s,
                            hidrofobicidad = %s
                        WHERE id = %s
                    """, (carga, hydro, pid))
                    updated += 1

                    if updated % 1000 == 0:
                        log.info(f"Procesados {updated} péptidos...")

                except Exception as e:
                    log.warning("Error en péptido %s: %s", pid, e)

            conn.commit()
            log.info(f"✅ Finalizado. Actualizados {updated} péptidos.")


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    calculate_properties()