import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from utils.engines import validate_engines, check_status
from utils.file_helpers import read_json
import dark_web_engine as d_eng
from app.dynamic_keyword_search import main
import pathlib
import socket
import time
import platform
import subprocess

# Load supported engines from JSON
current_path = pathlib.Path(__file__).parent.resolve()
supported_engines = read_json(f"{current_path}/utils/engines.json")

# TOR_EXECUTABLE_PATH = str(current_path / "assets" / "tor" / "tor.exe")
TOR_PROXY_ADDRESS = ("localhost", 9050)  # Default Tor SOCKS5 proxy port
WINDOWS_TOR_PATH = str(current_path / "assets" / "tor" / "tor.exe")
LINUX_TOR_COMMAND = "tor"  # Tor command in Linux/Docker
class Args:
    """Class to store parsed arguments in a global scope (avoids multiprocessing issues)."""
    def __init__(self, keyword, engines, exclude, mp_units, proxy, limit, continuous_write):
        self.search = keyword
        self.engines = engines if engines else []
        self.exclude = exclude if exclude else []
        self.mp_units = mp_units
        self.proxy = proxy
        self.limit = limit
        self.continuous_write = continuous_write


def is_tor_running():
    """Check if Tor is running by attempting to connect to the SOCKS5 proxy at 127.0.0.1:9050."""
    try:
        with socket.create_connection(("127.0.0.1", 9050), timeout=3):
            return True
    except (ConnectionRefusedError, socket.timeout):
        return False

def start_tor():
    """Start Tor based on the operating system (Windows or Linux)."""

    if is_tor_running():
        print("✅ Tor is already running.")
        return

    system_os = platform.system()  # Detect OS
    print(f"🚀 Running on {system_os}. Starting Tor...")

    if system_os == "Windows":
        # Start Tor using tor.exe for Windows
        if not os.path.exists(WINDOWS_TOR_PATH):
            print("❌ Tor executable not found! Make sure tor.exe is in the correct folder.")
            return
        process = subprocess.Popen(
            [WINDOWS_TOR_PATH],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )
    else:
        # Start Tor directly if running in Linux (Docker)
        process = subprocess.Popen(
            [LINUX_TOR_COMMAND],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL
        )

    # ✅ Wait up to 10 seconds for Tor to start
    for _ in range(10):
        time.sleep(1)
        if is_tor_running():
            print("✅ Tor has started successfully.")
            return

    print("❌ Failed to start Tor. Please check manually.")

def search_keyword(keyword, engines=None, exclude=None, mp_units=2, proxy="localhost:9050", limit=3, continuous_write=False):
    """
    Function to perform a dark web search with timing metrics.
    """
    timing_results = {}
    
    # Ensure Tor is running before proceeding
    start_time = time.time()
    start_tor()
    timing_results["start_tor"] = time.time() - start_time
    #print(f"Time taken for start_tor: {timing_results['start_tor']:.4f} seconds")
    
    args = Args(keyword, engines, exclude, mp_units, proxy, limit, continuous_write)

    # Validate engines
    start_time = time.time()
    if args.engines and args.exclude:
        raise ValueError("Error: You cannot specify both `engines` and `exclude` at the same time.")
    selected_engines = validate_engines(args, supported_engines)
    timing_results["validate_engines"] = time.time() - start_time
    #print(f"Time taken for validate_engines: {timing_results['validate_engines']:.4f} seconds")

    # Check the status of the selected engines
    start_time = time.time()
    up_engines, down = check_status(selected_engines)
    timing_results["check_status"] = time.time() - start_time
    #print(f"Time taken for check_status: {timing_results['check_status']:.4f} seconds")

    # Check if all engines are down
    if not up_engines:
        raise ValueError(f"All engines are down. Down engines: {down}")

    # Instantiate search engine logic
    start_time = time.time()
    new_search = d_eng.DarkWebSearch(args, args.search, up_engines, args.mp_units)
    timing_results["search_engine_init"] = time.time() - start_time
    #print(f"Time taken for search_engine_init: {timing_results['search_engine_init']:.4f} seconds")

    # Run search function
    start_time = time.time()
    output_json = main(args.search, args.mp_units, new_search.results)
    timing_results["main_search"] = time.time() - start_time
    #print(f"Time taken for main_search: {timing_results['main_search']:.4f} seconds")
    return output_json