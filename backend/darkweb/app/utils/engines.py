
import sys
import requests
import concurrent.futures


def check_status(engines):
    """ 
    Check the status of search engines concurrently and return a list of up and down engines.
    
    :param engines: Dictionary of search engines (name -> URL).
    :return: (List of up engines, List of down engines)
    """
    up_engines = []
    down = []
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"
    }
    proxies = {'http': 'socks5h://localhost:9050', 'https': 'socks5h://localhost:9050'}
    
    def check_engine(name, url):
        """Check a single engine's status."""
        try:
            # Increased timeout for Tor connections (onion sites are slower)
            response = requests.get(url, proxies=proxies, headers=headers, timeout=15)
            # Accept any 2xx status code (200-299) as success
            if 200 <= response.status_code < 300:
                return name
            else:
                print(f"[{name}] Failed with status code {response.status_code}")
                return None
        except requests.exceptions.Timeout:
            print(f"[{name}] Timeout after 15 seconds")
            return None
        except requests.exceptions.ConnectionError as e:
            print(f"[{name}] Connection error: {str(e)[:100]}")
            return None
        except requests.exceptions.RequestException as e:
            print(f"[{name}] Request failed: {str(e)[:100]}")
            return None
    
    # Run checks concurrently
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        future_to_engine = {executor.submit(check_engine, name, url): name for name, url in engines.items()}
        for future in concurrent.futures.as_completed(future_to_engine):
            name = future_to_engine[future]
            result = future.result()
            if result:
                up_engines.append(result)
            else:
                down.append(name)

    print(f"Up Engines: {up_engines}")
    print(f"Down Engines: {down}")

    return up_engines, down

 
def validate_engines(args, supported_engines):
    """ 
    Validate and select the engines for the search dynamically 
    handling --engines and --exclude.
    """

    #  Check if both --engines and --exclude were specified (invalid case)
    if args.engines and args.exclude:
        print("Error: You cannot specify both --engines and --exclude at the same time.")
        sys.exit(1)  # Exit the program with an error code

    #  If --engines is provided, filter only valid engines
    if args.engines:
        selected_engines = {name: supported_engines[name] for name in args.engines if name in supported_engines}
        if not selected_engines:
            print("Error: None of the provided engines are valid. Exiting.")
            sys.exit(1)  #  Exit if no valid engines are found
        #print(f"Using selected engines: {list(selected_engines.keys())}")

    #  If --exclude is provided, remove selected engines from the list
    elif args.exclude:
        excluded_engines = set(engine for sublist in args.exclude for engine in sublist)  # Handle nested lists
        selected_engines = {name: url for name, url in supported_engines.items() if name not in excluded_engines}
        print(f"Using all engines except: {excluded_engines}")

    # If no --engines or --exclude, use all available engines
    else:
        print("No engines provided. Using all available engines.")
        selected_engines = supported_engines  #  Default to all engines

    return selected_engines  #  Return final selection



def print_flag_info():
    """ Display the flag descriptions and usage instructions """
    flag_info = {
        "--proxy": "Tor proxy address. Default is 'localhost:9050'.",
        "--output": "Output file where results will be saved. Supports dynamic $SEARCH and $DATE replacements.",
        "--continuous_write": "Whether to write to the output file progressively or not.",
        "--search": "Search term or phrase to use. Default is 'bannerbuzz.com'.",
        "--limit": "Maximum number of pages per engine to load. Default is 3.",
        "--engines": "List of engines to request. Default is the full list.",
        "--exclude": "List of engines to exclude from the search.",
        "--mp_units": "Number of multiprocessing units to use. Default is system cores minus 1."
    }

    print("\nAvailable flags and instructions:")
    for flag, description in flag_info.items():
        print(f"{flag}: {description}")
    print("\n")

