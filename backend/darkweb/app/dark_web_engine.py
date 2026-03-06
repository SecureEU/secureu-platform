from utils.file_helpers import read_json, split_search
from multiprocessing import Pool, cpu_count, freeze_support
from utils.scrap_engines import run_method
import datetime
import pathlib
from collections import Counter


class DarkWebSearch:
    current_path = pathlib.Path(__file__).parent.resolve()

    def __init__(self, parser_args, search, engines, mp_units, proxy='localhost:9050', limit=3,
                 field_delimiter=",",
                 continuous_write=False, 
                # available_csv_fields=None, 
                 timeout=1200,
                 engines_path=f"{current_path}/utils/engines.json",
                 desktop_agents_path=f"{current_path}/utils/desktop_agents.json"):
        """
        Search constructor
        """
        self.proxies = {'http': 'socks5h://{}'.format(proxy), 'https': 'socks5h://{}'.format(proxy)}
        self.search = split_search(search)
        self.limit = limit
        self.field_delimiter = field_delimiter
        self.mp_units = mp_units
        self.continuous_write = continuous_write
        self.timeout = timeout 
        self.start_time = datetime.datetime.now()
        self.stop_time = None
        self.parser_args = parser_args
        self.parser_args.search = self.search

        # Load all available engines
        self.supported_engines = read_json(engines_path) 
        self.desktop_agents = read_json(desktop_agents_path)["agents"]

        # Store requested engines
        self.engines = engines  # ["ahmia", "tor66", etc.]

        # Filter only "up" engines
        self.up_engines = self.get_up_engines(self.engines)  # Only active engines

        # Run search
        self.run()


    def get_up_engines(self, up_engine_names):
        """
        Returns a dictionary {engine_name: engine_url} containing only the engines 
        that are in the provided up_engine_names list.

        :param up_engine_names: List of engine names that are up.
        :return: Dictionary {engine_name: engine_url}.
        """
        if not isinstance(self.supported_engines, dict):
            print(f"Error: supported_engines is not a dictionary! It is {type(self.supported_engines)}")
            return {}

        # Filter self.supported_engines to only include names in up_engine_names
        up_engines_dict = {
            engine: self.supported_engines[engine]  #  Extract URL
            for engine in up_engine_names
            if engine in self.supported_engines  # Ensures the engine exists
        }

        #print(f"Filtered up engines: {up_engines_dict}")  # Debugging
        return up_engines_dict  # Returns {engine_name: engine_url}
    
    def run(self):
        """Run the search using multiprocessing with only up engines."""
        if self.mp_units and self.mp_units > 0:
            units = self.mp_units
        else:
            units = max((cpu_count() - 1), 1)
        print(f"search.py started with {units} processing units...")

        # Prepare the arguments dynamically **for only up engines**
        func_args = [
            (engine, self.parser_args, self.proxies, self.supported_engines, self.desktop_agents)
            for engine in self.up_engines
        ]

        freeze_support()
        with Pool(units) as pool:
            try:
                results = pool.starmap_async(run_method, func_args).get(timeout=self.timeout)
                # Flatten results (since run_method might return lists)
                self.results = [item for sublist in results for item in sublist]
            except Exception as e:
                print(f"Error during multiprocessing: {e}")
            finally:
                self.stop_time = datetime.datetime.now()
        pool.terminate()


    def count_results(self):
        """
        Count occurrences of search results per engine.
        
        :return: List of lists: [[engine, count], ...]
        """
        if not hasattr(self, "results") or not self.results:
            print("No results available.")
            return []

        # Extract only the search engine names from the results
        engine_counts = Counter(engine for engine, _, _ in self.results)

        # Convert Counter object to a list of lists
        line_table = [[engine, count] for engine, count in engine_counts.items()]

        print("Search Results Count per Engine:", line_table)  # Debugging
        return line_table


    def get_results(self):
        """
        Returns the search results as a tuple.
        """
        return tuple(self.results)
    

    def print_search_info(self):
        """
        Print search summary, including the count of results per engine.
        """
        if not hasattr(self, "results") or not self.results:
            print("\n\nReport:")
            print("  No search results available.")
            return

        total = 0
        print("\n\nReport:")
        print(f"  Execution time: {self.stop_time - self.start_time} seconds")
        print("  Results per engine:")

        # Count occurrences of each engine in self.results
        engine_counts = Counter(result["engine"] for result in self.results)

        # Print counts per engine
        for engine, count in engine_counts.items():
            total += count
            print(f"    {engine}: {count}")

        print(f"  Total results found: {total}")