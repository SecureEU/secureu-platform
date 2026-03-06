import math
import time
import requests
from bs4 import BeautifulSoup
from tqdm import tqdm
from utils.get_data import link_finder, random_headers, get_proc_pos, get_tqdm_desc
from urllib.parse import quote
from utils.file_helpers import clear, save_results
import re
################################################## Common functions ##################################################


def run_method(new_engine, parser_args, proxies, supported_engines, desktop_agents):
    """
    Calls the appropriate search engine function dynamically and returns the results.

    :param new_engine: The name of the search engine.
    :param parser_args: Arguments for the search.
    :param proxies: Proxy settings.
    :param supported_engines: Dictionary of search engine URLs.
    :param desktop_agents: List of user-agent headers.
    :return: List of results found.
    """
    method_name = new_engine
    ret = []
    try:
        ret = globals()[method_name](parser_args.search, parser_args, proxies, supported_engines, desktop_agents)
    except Exception as e:
        print(f'Exception occurred: {e}')
    return ret  


def ahmia(searchstr, args, proxies, supported_engines, desktop_agents):
    ahmia_url = supported_engines['ahmia'] + "/search/?q={}"

    pos = get_proc_pos()
    with tqdm(total=1, initial=0, desc=get_tqdm_desc("Ahmia", pos), position=pos) as progress_bar:
        response = requests.get(ahmia_url.format(quote(searchstr)), proxies=proxies,
                                headers=random_headers(desktop_agents))
        soup = BeautifulSoup(response.text, 'html5lib')
        results = link_finder("ahmia", soup, args)
        progress_bar.update()
    save_results(results, args)

    return results


def darksearchio(searchstr, args, proxies, supported_engines, desktop_agents):
    darksearchio_url = supported_engines['darksearchio'] + "/api/search?query={}&page={}"
    max_nb_page = 30
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)
        resp = s.get(darksearchio_url.format(quote(searchstr), 1))

        page_number = 1
        if resp.status_code == 200:
            resp = resp.json()
            if 'last_page' in resp:
                page_number = resp['last_page']
            if page_number > max_nb_page:
                page_number = max_nb_page
        else:
            return

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("DarkSearch (.io)", pos), position=pos) \
                as progress_bar:

            results = link_finder("darksearchio", resp['data'], args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(darksearchio_url.format(quote(searchstr), n))
                if resp.status_code == 200:
                    resp = resp.json()
                    results = results + link_finder("darksearchio", resp['data'], args)
                    progress_bar.update()
                else:
                    # Current page results will be lost, but we will try to continue after a short sleep
                    time.sleep(1)
    save_results(results, args)
    return results


def clone_systems_engine(searchstr, args, proxies, supported_engines, desktop_agents):
    clone_systems_engine_url = supported_engines['clone_systems_engine'] + "/search?q={}&page={}"
    max_nb_page = 100 # up to 100 page search
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(clone_systems_engine_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        for i in soup.find_all('div', attrs={"class": "search-status"}):
            approx_re = re.match(r"About ([,0-9]+) result(.*)",
                                 clear(i.find('div', attrs={'class': "col-sm-12"}).get_text()))
            if approx_re is not None:
                nb_res = int((approx_re.group(1)).replace(",", ""))
                results_per_page = 19
                page_number = math.ceil(nb_res / results_per_page)
                if page_number > max_nb_page:
                    page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=max_nb_page, initial=0, desc=get_tqdm_desc("Clone_Systems_Engine", pos), position=pos) as progress_bar:

            results = link_finder("clone_systems_engine", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(clone_systems_engine_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                ret = link_finder("clone_systems_engine", soup, args)
                if len(ret) == 0:
                    break
                results = results + ret
                progress_bar.update()
    save_results(results, args)
    return results


def notevil(searchstr, args, proxies, supported_engines, desktop_agents):
    notevil_url1 = supported_engines['notevil'] + "/index.php?q={}"
    notevil_url2 = supported_engines['notevil'] + "/index.php?q={}&hostLimit=20&start={}&numRows={}&template=0"
    max_nb_page = 20
    if args.limit != 0:
        max_nb_page = args.limit

    # Do not use requests.Session() here (by experience less results would be got)
    req = requests.get(notevil_url1.format(quote(searchstr)), proxies=proxies, headers=random_headers(desktop_agents))
    soup = BeautifulSoup(req.text, 'html5lib')

    page_number = 1
    last_div = soup.find("div", attrs={"style": "text-align:center"}).find("div", attrs={"style": "text-align:center"})
    if last_div is not None:
        for i in last_div.find_all("a"):
            page_number = int(i.get_text())
        if page_number > max_nb_page:
            page_number = max_nb_page

    pos = get_proc_pos()
    with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("not Evil", pos), position=pos) as progress_bar:
        num_rows = 20
        results = link_finder("notevil", soup, args)
        progress_bar.update()

        for n in range(2, page_number + 1):
            start = (int(n - 1) * num_rows)
            req = requests.get(notevil_url2.format(quote(searchstr), start, num_rows),
                               proxies=proxies,
                               headers=random_headers(desktop_agents))
            soup = BeautifulSoup(req.text, 'html5lib')
            results = results + link_finder("notevil", soup, args)
            progress_bar.update()
            time.sleep(1)
    save_results(results, args)
    return results


def darksearchenginer(searchstr, args, proxies, supported_engines, desktop_agents):
    darksearchenginer_url = supported_engines['darksearchenginer']
    max_nb_page = 20
    if args.limit != 0:
        max_nb_page = args.limit
    page_number = 1

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        # Note that this search engine is very likely to timeout
        resp = s.post(darksearchenginer_url, data={"search[keyword]": searchstr, "page": page_number})
        soup = BeautifulSoup(resp.text, 'html5lib')

        pages_input = soup.find_all("input", attrs={"name": "page"})
        for i in pages_input:
            page_number = int(i['value'])
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Dark Search Enginer", pos), position=pos) \
                as progress_bar:

            results = link_finder("darksearchenginer", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.post(darksearchenginer_url, data={"search[keyword]": searchstr, "page": str(n)})
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("darksearchenginer", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def phobos(searchstr, args, proxies, supported_engines, desktop_agents):
    phobos_url = supported_engines['phobos'] + "/search?query={}&p={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(phobos_url.format(quote(searchstr), 1), proxies=proxies, headers=random_headers(desktop_agents))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find("div", attrs={"class": "pages"}).find_all('a')
        if pages is not None:
            for i in pages:
                page_number = int(i.get_text())
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Phobos", pos), position=pos) as progress_bar:
            results = link_finder("phobos", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(phobos_url.format(quote(searchstr), n), proxies=proxies,
                             headers=random_headers(desktop_agents))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("phobos", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def onionsearchserver(searchstr, args, proxies, supported_engines, desktop_agents):
    results = []
    onionsearchserver_url1 = supported_engines['onionsearchserver'] + "/oss/"  # ?page=1&query={}
    onionsearchserver_url2 = None
    results_per_page = 10
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(onionsearchserver_url1)
        soup = BeautifulSoup(resp.text, 'html5lib')
        for i in soup.find_all('iframe', attrs={"style": "display:none;"}):
            onionsearchserver_url2 = i['src'] + "{}&page={}"

        if onionsearchserver_url2 is None:
            return results

        resp = s.get(onionsearchserver_url2.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find_all("div", attrs={"class": "osscmnrdr ossnumfound"})
        if pages is not None and not str(pages[0].get_text()).startswith("No"):
            total_results = float(str.split(clear(pages[0].get_text()))[0])
            page_number = math.ceil(total_results / results_per_page)
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Onion Search Server", pos), position=pos) \
                as progress_bar:

            results = link_finder("onionsearchserver", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(onionsearchserver_url2.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("onionsearchserver", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def torgle(searchstr, args, proxies, supported_engines, desktop_agents):
    torgle_url = supported_engines['torgle'] + "/search.php?term={}"

    pos = get_proc_pos()
    with tqdm(total=1, initial=0, desc=get_tqdm_desc("Torgle", pos), position=pos) as progress_bar:
        response = requests.get(torgle_url.format(quote(searchstr)), proxies=proxies,
                                headers=random_headers(desktop_agents))
        soup = BeautifulSoup(response.text, 'html5lib')
        results = link_finder("torgle", soup, args)
        progress_bar.update()
    save_results(results, args)
    return results


def onionsearchengine(searchstr, args, proxies, supported_engines, desktop_agents):
    onionsearchengine_url = supported_engines['onionsearchengine'] + "/search.php?search={}&submit=Search&page={}"
    # same as onionsearchengine_url = "http://5u56fjmxu63xcmbk.onion/search.php?search={}&submit=Search&page={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(onionsearchengine_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        approx_re = re.search(r"\s([0-9]+)\sresult[s]?\sfound\s!.*", clear(soup.find('body').get_text()))
        if approx_re is not None:
            nb_res = int(approx_re.group(1))
            results_per_page = 9
            page_number = math.ceil(float(nb_res / results_per_page))
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Onion Search Engine", pos), position=pos) \
                as progress_bar:

            results = link_finder("onionsearchengine", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(onionsearchengine_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("onionsearchengine", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def tordex(searchstr, args, proxies, supported_engines, desktop_agents):
    tordex_url = supported_engines['tordex'] + "/search?query={}&page={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(tordex_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find_all("li", attrs={"class": "page-item"})
        if pages is not None:
            for i in pages:
                if i.get_text() != "...":
                    page_number = int(i.get_text())
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Tordex", pos), position=pos) as progress_bar:

            results = link_finder("tordex", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(tordex_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("tordex", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def tor66(searchstr, args, proxies, supported_engines, desktop_agents):
    tor66_url = supported_engines['tor66'] + "/search?q={}&sorttype=rel&page={}"
    max_nb_page = 30
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(tor66_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        approx_re = re.search(r"\.Onion\ssites\sfound\s:\s([0-9]+)", resp.text)
        if approx_re is not None:
            nb_res = int(approx_re.group(1))
            results_per_page = 20
            page_number = math.ceil(float(nb_res / results_per_page))
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Tor66", pos), position=pos) as progress_bar:

            results = link_finder("tor66", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(tor66_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("tor66", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def tormax(searchstr, args, proxies, supported_engines, desktop_agents):
    tormax_url = supported_engines['tormax'] + "/search?q={}"

    pos = get_proc_pos()
    with tqdm(total=1, initial=0, desc=get_tqdm_desc("Tormax", pos), position=pos) as progress_bar:
        response = requests.get(tormax_url.format(quote(searchstr)), proxies=proxies,
                                headers=random_headers(desktop_agents))
        soup = BeautifulSoup(response.text, 'html5lib')
        results = link_finder("tormax", soup, args)
        progress_bar.update()
    save_results(results, args)
    return results


def haystack(searchstr, args, proxies, supported_engines, desktop_agents):
    results = []
    haystack_url = supported_engines['haystack'] + "/?q={}&offset={}"
    # At the 52nd page, it timeouts 100% of the time
    max_nb_page = 50
    if args.limit != 0:
        max_nb_page = args.limit
    offset_coeff = 20

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        req = s.get(haystack_url.format(quote(searchstr), 0))
        soup = BeautifulSoup(req.text, 'html5lib')

        pos = get_proc_pos()
        with tqdm(total=max_nb_page, initial=0, desc=get_tqdm_desc("Haystack", pos), position=pos) as progress_bar:
            continue_processing = True
            ret = link_finder("haystack", soup, args)
            results = results + ret
            progress_bar.update()
            if len(ret) == 0:
                continue_processing = False

            it = 1
            while continue_processing:
                offset = int(it * offset_coeff)
                req = s.get(haystack_url.format(quote(searchstr), offset))
                soup = BeautifulSoup(req.text, 'html5lib')
                ret = link_finder("haystack", soup, args)
                results = results + ret
                progress_bar.update()
                it += 1
                if it >= max_nb_page or len(ret) == 0:
                    continue_processing = False
    save_results(results, args)
    return results


def multivac(searchstr, args, proxies, supported_engines, desktop_agents):
    results = []
    multivac_url = supported_engines['multivac'] + "/?q={}&page={}"
    max_nb_page = 10
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        page_to_request = 1
        req = s.get(multivac_url.format(quote(searchstr), page_to_request))
        soup = BeautifulSoup(req.text, 'html5lib')

        pos = get_proc_pos()
        with tqdm(total=max_nb_page, initial=0, desc=get_tqdm_desc("Multivac", pos), position=pos) as progress_bar:
            continue_processing = True
            ret = link_finder("multivac", soup, args)
            results = results + ret
            progress_bar.update()
            if len(ret) == 0 or page_to_request >= max_nb_page:
                continue_processing = False

            while continue_processing:
                page_to_request += 1
                req = s.get(multivac_url.format(quote(searchstr), page_to_request))
                soup = BeautifulSoup(req.text, 'html5lib')
                ret = link_finder("multivac", soup, args)
                results = results + ret
                progress_bar.update()
                if len(ret) == 0 or page_to_request >= max_nb_page:
                    continue_processing = False
    save_results(results, args)
    return results


def evosearch(searchstr, args, proxies, supported_engines, desktop_agents):
    evosearch_url = supported_engines['evosearch'] + "/evo/search.php?" \
                                                     "query={}&" \
                                                     "start={}&" \
                                                     "search=1&type=and&mark=bold+text&" \
                                                     "results={}"
    results_per_page = 50
    max_nb_page = 30
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        req = s.get(evosearch_url.format(quote(searchstr), 1, results_per_page))
        soup = BeautifulSoup(req.text, 'html5lib')

        page_number = 1
        i = soup.find("p", attrs={"class": "cntr"})
        if i is not None:
            if i.get_text() is not None and "of" in i.get_text():
                nb_res = float(clear(str.split(i.get_text().split("-")[1].split("of")[1])[0]))
                page_number = math.ceil(nb_res / results_per_page)
                if page_number > max_nb_page:
                    page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Evo Search", pos), position=pos) as progress_bar:
            results = link_finder("evosearch", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(evosearch_url.format(quote(searchstr), n, results_per_page))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("evosearch", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def deeplink(searchstr, args, proxies, supported_engines, desktop_agents):
    deeplink_url1 = supported_engines['deeplink'] + "/index.php"
    deeplink_url2 = supported_engines['deeplink'] + "/?search={}&type=verified"

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)
        s.get(deeplink_url1)

        pos = get_proc_pos()
        with tqdm(total=1, initial=0, desc=get_tqdm_desc("DeepLink", pos), position=pos) as progress_bar:
            response = s.get(deeplink_url2.format(quote(searchstr)))
            soup = BeautifulSoup(response.text, 'html5lib')
            results = link_finder("deeplink", soup, args)
            progress_bar.update()
    save_results(results, args)
    return results


def torgle1(searchstr, args, proxies, supported_engines, desktop_agents):
    torgle1_url = supported_engines['torgle1'] + "/torgle/index-frame.php?query={}&search=1&engine-ver=2&isframe=0{}"
    results_per_page = 10
    max_nb_page = 30
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(torgle1_url.format(quote(searchstr), ""))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        i = soup.find('div', attrs={"id": "result_report"})
        if i is not None:
            if i.get_text() is not None and "of" in i.get_text():
                res_re = re.match(r".*of\s([0-9]+)\s.*", clear(i.get_text()))
                total_results = int(res_re.group(1))
                page_number = math.ceil(total_results / results_per_page)
                if page_number > max_nb_page:
                    page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("Torgle 1", pos), position=pos) as progress_bar:
            results = link_finder("torgle1", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                start_page_param = "&start={}".format(n)
                resp = s.get(torgle1_url.format(quote(searchstr), start_page_param))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("torgle1", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def demon(searchstr, args, proxies, supported_engines, desktop_agents):
    demon_url = supported_engines['demon'] + "/search?q={}&page={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(demon_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find_all("a", attrs={"class": "page-link"})
        if pages is not None:
            for i in pages:
                page_number = int(i.get_text())
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("demon", pos), position=pos) as progress_bar:

            results = link_finder("demon", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(demon_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("demon", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def torch(searchstr, args, proxies, supported_engines, desktop_agents):
    torch_url = supported_engines['torch'] + "/search?query={}"

    pos = get_proc_pos()
    with tqdm(total=1, initial=0, desc=get_tqdm_desc("torch", pos), position=pos) as progress_bar:
        response = requests.get(torch_url.format(quote(searchstr)), proxies=proxies,
                                headers=random_headers(desktop_agents))
        soup = BeautifulSoup(response.text, 'html5lib')
        results = link_finder("torch", soup, args)
        progress_bar.update()
    save_results(results, args)
    return results


def senator(searchstr, args, proxies, supported_engines, desktop_agents):
    senator_url = supported_engines['senator'] + "/?q={}&p={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(senator_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find("div", attrs={"class": "pagination"}).find_all("a")
        if pages is not None:
            for i in pages:
                if i.get_text().isdigit():
                    page_number = int(i.get_text())
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("senator", pos), position=pos) as progress_bar:

            results = link_finder("senator", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(senator_url.format(quote(searchstr), n))
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("senator", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results


def excavator(searchstr, args, proxies, supported_engines, desktop_agents):
    excavator_url = supported_engines['excavator'] + "/search/{}/?per_page={}"
    max_nb_page = 100
    if args.limit != 0:
        max_nb_page = args.limit

    with requests.Session() as s:
        s.proxies = proxies
        s.headers = random_headers(desktop_agents)

        resp = s.get(excavator_url.format(quote(searchstr), 1))
        soup = BeautifulSoup(resp.text, 'html5lib')

        page_number = 1
        pages = soup.find_all("li", attrs={"class": "page-item"})
        if pages is not None:
            page_url = []
            for i in pages:
                if i.get_text().isdigit():
                    page_number = int(i.get_text())
                    page_url.append(i.find("a").get("href"))
            if page_number > max_nb_page:
                page_number = max_nb_page

        pos = get_proc_pos()
        with tqdm(total=page_number, initial=0, desc=get_tqdm_desc("excavator", pos), position=pos) as progress_bar:

            results = link_finder("excavator", soup, args)
            progress_bar.update()

            for n in range(2, page_number + 1):
                resp = s.get(page_url[n - 2])
                soup = BeautifulSoup(resp.text, 'html5lib')
                results = results + link_finder("excavator", soup, args)
                progress_bar.update()
    save_results(results, args)
    return results
