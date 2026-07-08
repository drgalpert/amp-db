"""
db_utils.py — Utilidades de conexión e inserción en PostgreSQL.
"""

import logging
import time
from contextlib import contextmanager
from typing import Any, Generator

import psycopg2
import psycopg2.extras

from config import DB_CONFIG, BATCH_SIZE

log = logging.getLogger(__name__)


# ── Conexión ──────────────────────────────────────────────────────────────────

@contextmanager
def get_conn() -> Generator[psycopg2.extensions.connection, None, None]:
    """Context manager que abre/cierra una conexión a peptidos_db."""
    conn = psycopg2.connect(**DB_CONFIG)
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


# ── Helpers de inserción ──────────────────────────────────────────────────────

def upsert_one(conn, table: str, data: dict, conflict_col: str) -> Any:
    """
    INSERT ... ON CONFLICT robusto para PostgreSQL.
    Maneja correctamente cuando solo hay una columna.
    """
    if not data:
        return None

    cols = list(data.keys())
    vals = list(data.values())

    table_quoted = f'"{table}"' if '"' not in table else table
    columns_str = ', '.join(f'"{col}"' for col in cols)
    placeholders = ', '.join(['%s'] * len(vals))

    # Si la única columna es la de conflicto → solo INSERT con DO NOTHING
    if len(cols) == 1 and cols[0] == conflict_col:
        sql = f"""
            INSERT INTO {table_quoted} ({columns_str})
            VALUES ({placeholders})
            ON CONFLICT ("{conflict_col}") DO NOTHING
            RETURNING *;
        """
    else:
        # Update para el resto de columnas
        updates = ', '.join(
            f'"{col}" = EXCLUDED."{col}"'
            for col in cols if col != conflict_col
        )
        sql = f"""
            INSERT INTO {table_quoted} ({columns_str})
            VALUES ({placeholders})
            ON CONFLICT ("{conflict_col}") 
            DO UPDATE SET {updates}
            RETURNING *;
        """

    try:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(sql, vals)
            row = cur.fetchone()
            return dict(row) if row else None
    except Exception as e:
        log.error("Error en upsert_one %s: %s", table, e)
        log.error("SQL: %s", sql)
        raise


def upsert_many(conn, table: str, rows: list[dict], conflict_col: str) -> list[dict]:
    """Versión batch de upsert_one. Devuelve las filas insertadas/actualizadas."""
    if not rows:
        return []
    results = []
    for i in range(0, len(rows), BATCH_SIZE):
        batch = rows[i : i + BATCH_SIZE]
        for row in batch:
            r = upsert_one(conn, table, row, conflict_col)
            if r:
                results.append(r)
        log.debug("Batch %d/%d insertado en %s", i + BATCH_SIZE, len(rows), table)
    return results


def insert_relation(conn, table: str, col_a: str, id_a: int,
                    col_b: str, id_b: int, extras: dict | None = None) -> None:
    """Inserta en tabla de relación N:M; ignora duplicados."""
    data = {col_a: id_a, col_b: id_b, **(extras or {})}
    cols = list(data.keys())
    vals = list(data.values())
    sql = f"""
        INSERT INTO {table} ({', '.join(cols)})
        VALUES ({', '.join(['%s'] * len(vals))})
        ON CONFLICT DO NOTHING;
    """
    with conn.cursor() as cur:
        cur.execute(sql, vals)


def fetch_or_create(conn, table: str, data: dict, conflict_col: str) -> int:
    with conn.cursor() as cur:
        sql = f'SELECT id FROM "{table}" WHERE "{conflict_col}" = %s'
        cur.execute(sql, (data[conflict_col],))
        row = cur.fetchone()
        if row:
            return row[0]

    result = upsert_one(conn, table, data, conflict_col)
    return result["id"] if result and "id" in result else None


# ── Registro de fuente ─────────────────────────────────────────────────────────

def ensure_fuente(conn, nombre: str, url: str = "", version: str = "") -> None:
    """Asegura que la fuente exista en fuente_datos."""
    data = {"nombre": nombre, "url": url, "version": version}
    upsert_one(conn, "fuente_datos", data, conflict_col="nombre")


# ── Estadísticas post-carga ───────────────────────────────────────────────────

def print_stats(conn) -> None:
    tables = [
        "pÉptido", "nombre_alternativo", "fuente_organismo",
        "organismo_blanco", "publicaciÓn", "estructura",
        "estabilidad_clinica", "modificacion_postraduccional",
        "peptido_organismo", "peptido_anticancer", "peptido_antiviral",
        "peptido_antifungico", "peptido_anticancer", "peptido_publicacion", "peptido_fuente",
    ]
    log.info("─── Estadísticas de carga ───")
    with conn.cursor() as cur:
        for t in tables:
            cur.execute(f'SELECT COUNT(*) FROM "{t}"')
            n = cur.fetchone()[0]
            log.info("  %-35s %6d filas", t, n)
