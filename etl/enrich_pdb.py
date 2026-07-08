import requests
import time
from db_utils import get_conn

URL = "https://data.rcsb.org/rest/v1/core/entry/"

with get_conn() as conn:

    with conn.cursor() as cur:

        cur.execute("""
            SELECT DISTINCT pdb_id
            FROM estructura
            WHERE pdb_id IS NOT NULL
            AND (
                metodo IS NULL
                OR metodo = ''
            )
        """)

        pdbs = [x[0] for x in cur.fetchall()]

    print("Total PDB IDs:", len(pdbs))

    for pdb_id in pdbs:

        try:

            time.sleep(0.1)
            print("Consultando:", pdb_id)

            r = requests.get(
                URL + pdb_id,
                timeout=10
            )

            if r.status_code != 200:
                print("HTTP ERROR", pdb_id, r.status_code)
                continue

            data = r.json()

            metodo = None

            exptl = data.get("exptl", [])

            if exptl:
                metodo = exptl[0].get("method")

            print("Método:", metodo)

            with conn.cursor() as cur:

                cur.execute("""
                    UPDATE estructura
                    SET metodo = %s
                    WHERE pdb_id = %s
                """, (metodo, pdb_id))

            conn.commit()

        except Exception as e:

            conn.rollback()

            print("ERROR:", pdb_id, e)