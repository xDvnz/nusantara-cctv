"""Runner discovery: eksekusi semua fetcher sumber. Error satu sumber tidak menghentikan lainnya."""
import importlib
import sys
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import log  # noqa: E402

FETCHERS = ["malangkota", "jogjakota", "palembang", "banjarmasin", "bandungkota"]


def main():
    Path(__file__).parent  # noqa: F841
    results = {}
    for name in FETCHERS:
        try:
            mod = importlib.import_module(name)
            results[name] = len(mod.fetch_all() or [])
            log(f"OK {name}: {results[name]} kamera")
        except Exception as e:  # noqa: BLE001
            log(f"GAGAL {name}: {e}")
            traceback.print_exc()
            results[name] = 0
    log(f"Selesai: {results}")


if __name__ == "__main__":
    main()
