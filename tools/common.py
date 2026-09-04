"""Shared helpers untuk pipeline discovery/validation CCTV Indonesia."""
import hashlib
import json
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

import requests

ROOT = Path(__file__).resolve().parents[1]
DATA_DIR = ROOT / "data"
RAW_DIR = ROOT / "data" / "raw"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/126.0 Safari/537.36")
TIMEOUT = 15

WIB = timezone(__import__("datetime").timedelta(hours=7))


def now_wib() -> str:
    return datetime.now(WIB).isoformat(timespec="seconds")


def http_session(allow_insecure_fallback: bool = True) -> requests.Session:
    """Sesi HTTP. Beberapa portal pemda tidak mengirim intermediate TLS cert
    (mis. cctv.malangkota.go.id, GeoTrust TLS RSA CA G1) sehingga verify gagal.
    Fallback insecure hanya dipakai utk read-only data publik, tercatat di log."""
    s = requests.Session()
    s.headers.update({"User-Agent": UA, "Accept": "*/*"})
    s.verify = True
    if allow_insecure_fallback:
        orig_request = s.request

        def request_method(*args, **kwargs):
            try:
                return orig_request(*args, **kwargs)
            except requests.exceptions.SSLError:
                log(f"TLS verify gagal ({args[0] if args else kwargs.get('url','')}) -> fallback insecure (read-only publik)")
                kwargs["verify"] = False
                return orig_request(*args, **kwargs)

        s.request = request_method  # type: ignore[method-assign]
    return s


def stable_id(source_id: str, public_identifier: str) -> str:
    digest = hashlib.sha1(f"{source_id}|{public_identifier}".encode()).hexdigest()
    return digest[:16]


def clean(value):
    """Trim string; '' -> None. Angka string -> float bila memungkinkan."""
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return value
    v = str(value).strip()
    return v if v else None


def as_float(value):
    try:
        if value in (None, "", "0"):
            return None
        f = float(str(value).strip())
        return f if f != 0 else None
    except (TypeError, ValueError):
        return None


def save_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding="utf-8")


def load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def slug(text: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "-", (text or "").lower()).strip("-")
    return s[:60] or "unknown"


def log(msg: str) -> None:
    print(f"[{datetime.now(WIB).strftime('%H:%M:%S')}] {msg}", flush=True)


def retry(fn, attempts=2, delay=1.5):
    for i in range(attempts):
        try:
            return fn()
        except Exception as e:  # noqa: BLE001 - pipeline tool, log lalu lanjut
            if i == attempts - 1:
                raise
            log(f"retry {i+1}/{attempts-1} setelah error: {e}")
            time.sleep(delay)
    return None
