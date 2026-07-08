"""
http_utils.py — Cliente HTTP con reintentos y rate-limiting.
"""

import logging
import time
from typing import Any

import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from config import MAX_RETRIES, RETRY_DELAY, REQUEST_TIMEOUT

log = logging.getLogger(__name__)

_SESSION: requests.Session | None = None


def get_session() -> requests.Session:
    global _SESSION
    if _SESSION is None:
        _SESSION = requests.Session()
        retry = Retry(
            total=MAX_RETRIES,
            backoff_factor=RETRY_DELAY,
            status_forcelist=[429, 500, 502, 503, 504],
        )
        adapter = HTTPAdapter(max_retries=retry)
        _SESSION.mount("http://", adapter)
        _SESSION.mount("https://", adapter)
        _SESSION.headers.update({
            "User-Agent": "peptidos_etl/1.0 (research; contact@example.com)"
        })
    return _SESSION


def get_json(url: str, params: dict | None = None, **kwargs) -> Any:
    """GET que retorna JSON; lanza excepción si falla."""
    resp = get_session().get(url, params=params, timeout=REQUEST_TIMEOUT, **kwargs)
    resp.raise_for_status()
    return resp.json()


def get_text(url: str, params: dict | None = None, **kwargs) -> str:
    """GET que retorna texto plano."""
    resp = get_session().get(url, params=params, timeout=REQUEST_TIMEOUT, **kwargs)
    resp.raise_for_status()
    return resp.text


def post_json(url: str, payload: dict, **kwargs) -> Any:
    """POST que retorna JSON."""
    resp = get_session().post(url, json=payload, timeout=REQUEST_TIMEOUT, **kwargs)
    resp.raise_for_status()
    return resp.json()


def paginate_uniprot(query: str, fields: str, page_size: int = 500):
    """
    Generador que itera sobre todas las páginas de la API de UniProt REST.
    Yields: listas de resultados.
    """
    from config import UNIPROT_API
    url = UNIPROT_API
    params = {
        "query":    query,
        "format":   "json",
        "fields":   fields,
        "size":     page_size,
    }
    while url:
        resp = get_session().get(url, params=params, timeout=REQUEST_TIMEOUT)
        resp.raise_for_status()
        data = resp.json()
        results = data.get("results", [])
        log.debug("UniProt: %d resultados obtenidos", len(results))
        yield results
        # Link header para siguiente página
        link = resp.headers.get("Link", "")
        if 'rel="next"' in link:
            url = link.split("<")[1].split(">")[0]
            params = {}   # ya viene en la URL completa
        else:
            url = None
        time.sleep(0.3)   # respeto al rate-limit de UniProt
