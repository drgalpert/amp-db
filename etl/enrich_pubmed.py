import requests
from db_utils import get_conn

URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"

with get_conn() as conn:

    with conn.cursor() as cur:
        cur.execute("""
            SELECT pmid
            FROM publicaciÓn
            WHERE titulo IS NULL
        """)

        pmids = [x[0] for x in cur.fetchall()]

    for pmid in pmids:

        params = {
            "db": "pubmed",
            "id": pmid,
            "retmode": "json"
        }

        r = requests.get(URL, params=params)
        data = r.json()

        result = data["result"][str(pmid)]

        titulo = result.get("title")
        pubdate = result.get("pubdate", "")
        doi = None

        for article_id in result.get("articleids", []):
            if article_id["idtype"] == "doi":
                doi = article_id["value"]

        autores = ", ".join(
            a["name"]
            for a in result.get("authors", [])
        )

        anio = None

        if pubdate:
            try:
                anio = int(pubdate[:4])
            except:
                pass

        with conn.cursor() as cur:
            cur.execute("""
                UPDATE publicaciÓn
                SET
                    titulo = %s,
                    doi = %s,
                    autores = %s,
                    anyo = %s
                WHERE pmid = %s
            """, (
                titulo,
                doi,
                autores,
                anio,
                pmid
            ))

        conn.commit()

        print(f"Actualizado PMID {pmid}")