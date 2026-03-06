import json

def read_json(file_path):
    try:
        with open(file_path, "r") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"Error: The file {file_path} was not found.")
        return {}
    except json.JSONDecodeError as e:
        print(f"Error: Could not decode JSON in {file_path}: {e}")
        return {}

def save_results(results, args):
    """
    Processes and returns results instead of saving them to a file.

    :param results: List of search results.
    :param args: Parser arguments.
    :return: List of processed results.
    """
    processed_results = []

    for result in results:
        processed_result = {
            "engine": result.get("engine"),
            "name": result.get("name"),
            "link": result.get("link")
        }
        processed_results.append(processed_result)

    return processed_results  # ✅ Returns results instead of writing to a file

def clear(to_clear):
    new_str = to_clear.replace("\n", " ")
    new_str = ' '.join(new_str.split())
    return new_str

def print_epilog(available_csv_fields, supported_engines):
    epilog = "Available CSV fields: \n\t"
    for f in available_csv_fields:
        epilog += " {}".format(f)
    epilog += "\n"
    epilog += "Supported engines: \n\t"
    for e in supported_engines.keys():
        epilog += " {}".format(e)
    return epilog


def split_search(search_str):
    if '-' in search_str:
        if '__' in search_str:
            search_str_first_split = search_str.split('-')
            length = len(search_str_first_split)
            search_str_second_split = search_str_first_split[length-1].split('__')
            search_str_temp = ' ' + search_str_second_split[1]
            del search_str_first_split[-1]
            if length > 2:
                search_str_temp = '"' + '-'.join(search_str_first_split) + '-' + search_str_second_split[0] + '"' + search_str_temp
            else:
                search_str_temp = '"' + search_str_first_split[0] + '-' + search_str_second_split[0] + '"' + search_str_temp
            search_str = '"\\"' + search_str_temp + '\\""'
        else:
            search_str = '"' + search_str + '"'
    else:
        if '__' in search_str:
            search_str_temp = search_str.replace('__', ' ')
            search_str = '"\\"' + search_str_temp + '\\""'

    return search_str