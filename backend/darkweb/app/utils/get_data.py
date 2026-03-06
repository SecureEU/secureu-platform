import urllib.parse as urlparse
from urllib.parse import parse_qs
from urllib.parse import unquote
from utils.file_helpers import  clear
from multiprocessing import current_process
from random import choice
import re

def get_parameter(url, parameter_name):
    parsed = urlparse.urlparse(url)
    return parse_qs(parsed.query)[parameter_name][0]


def link_finder(engine_str, data_obj, parser_args):
    name = ""
    link = ""
    found_links = []
    csv_file = None



    def add_link():
        found_links.append({"engine": engine_str, "name": name, "link": link})

    if engine_str == "ahmia":
        for r in data_obj.select('li.result h4'):
            name = clear(r.get_text())
            link = r.find('a')['href'].split('redirect_url=')[1]
            add_link()

    if engine_str == "darksearchenginer":
        for r in data_obj.select('.table-responsive a'):
            name = clear(r.get_text())
            link = clear(r['href'])
            add_link()

    if engine_str == "darksearchio":
        for r in data_obj:
            name = clear(r["title"])
            link = clear(r["link"])
            add_link()

    if engine_str == "deeplink":
        for tr in data_obj.find_all('tr'):
            cels = tr.find_all('td')
            if cels is not None and len(cels) == 4:
                name = clear(cels[1].get_text())
                link = clear(cels[0].find('a')['href'])
                add_link()

    if engine_str == "evosearch":
        for r in data_obj.select("#results .title a"):
            name = clear(r.get_text())
            link = get_parameter(r['href'], 'url')
            add_link()

    if engine_str == "haystack":
        for r in data_obj.select(".result b a"):
            name = clear(r.get_text())
            link = get_parameter(r['href'], 'url')
            add_link()

    if engine_str == "multivac":
        for r in data_obj.select("dl dt a"):
            if r['href'] != "":
                name = clear(r.get_text())
                link = clear(r['href'])
                add_link()
            else:
                break

    if engine_str == "notevil":
        for r in data_obj.find_all("p"):
            r = r.find("a")
            name = clear(r.get_text())
            link = unquote(r["href"]).split('./r2d.php?url=')[1].split('&')[0]
            add_link()

    if engine_str == "clone_systems_engine":
        for r in data_obj.select('.result-block'):
            ad_span = r.select_one('span.label-ad')
            if not ad_span:
                name = clear(r.select_one('div.title').get_text())
                link = clear(r.select_one('div.link').get_text())
                add_link()

    if engine_str == "onionsearchengine":
        for r in data_obj.select("table a b"):
            name = clear(r.get_text())
            link = get_parameter(r.parent['href'], 'u')
            add_link()

    if engine_str == "onionsearchserver":
        for r in data_obj.select('.osscmnrdr.ossfieldrdr1 a'):
            name = clear(r.get_text())
            link = clear(r['href'])
            add_link()

    if engine_str == "phobos":
        for r in data_obj.select('.serp .titles'):
            name = clear(r.get_text())
            link = clear(r['href'])
            add_link()

    if engine_str == "tor66":
        for i in data_obj.find('hr').find_all_next('b'):
            if i.find('a'):
                name = clear(i.find('a').get_text())
                link = clear(i.find('a')['href'])
                add_link()

    if engine_str == "tordex":
        for r in data_obj.select('.container h5 a'):
            name = clear(r.get_text())
            link = clear(r['href'])
            add_link()

    if engine_str == "torgle":
        for i in data_obj.find_all('ul', attrs={"id": "page"}):
            for j in i.find_all('a'):
                if str(j.get_text()).startswith("http"):
                    link = clear(j.get_text())
                else:
                    name = clear(j.get_text())
            add_link()

    if engine_str == "torgle1":
        for r in data_obj.select("#results a.title"):
            name = clear(r.get_text())
            link = clear(r['href'])
            add_link()

    if engine_str == "tormax":
        for r in data_obj.find_all("section", attrs={"id":"search-results"})[0].find_all("article"):
            name = clear(r.find('a', attrs={"class":"title"}).get_text())
            link = clear(r.find('div', attrs={"class":"url"}).get_text())
            add_link()

    if engine_str == "demon":
        for r in data_obj.select('div.search'):
            name = clear(r.find('h4').get_text())
            link = clear(r.find('p', attrs={"class":"link"}).get_text())
            add_link()

    if engine_str == "torch":
        for r in data_obj.find_all("div", attrs={"class":"result mb-3"}):
            name = clear(r.find('h5').get_text())
            link = clear(r.find('small').get_text())
            add_link()

    if engine_str == "senator":
        for r in data_obj.find_all("div", attrs={"class":"result"}):
            name = clear(r.find('b').get_text())
            link = clear(r.find_all('a')[1].get_text())
            add_link()

    if engine_str == "excavator":
        for r in data_obj.find_all("div", attrs={"class":"mx-auto"}):
            if r.find('h6'):
                name = clear(r.find('a').get_text())
                link = clear(r.contents[2])
                add_link()

    if parser_args.continuous_write and not csv_file.closed:
        csv_file.close()

    return found_links


def get_domain_from_url(link):
    fqdn_re = r"^[a-z][a-z0-9+\-.]*://([a-z0-9\-._~%!$&'()*+,;=]+@)?([a-z0-9\-._~%]+|\[[a-z0-9\-._~%!$&'()*+,;=:]+\])"
    domain_re = re.match(fqdn_re, link)
    if domain_re is not None:
        if domain_re.lastindex == 2:
            return domain_re.group(2)
    return None


def random_headers(desktop_agents):
    return {'User-Agent': choice(desktop_agents),
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8'}


def get_proc_pos():
    return (current_process()._identity[0]) - 1


def get_tqdm_desc(e_name, pos):
    return "%20s (#%d)" % (e_name, pos)
