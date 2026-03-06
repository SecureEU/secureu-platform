# OnionSearch

CGDarkWebSearch is a Python3 script that scrapes urls on different ".onion" search engines.

![](https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSEsuRXIeHPpkr6EFYmVLBEZJn2EeC09oSoPg&usqp=CAU)


## 💡 Prerequisites
[Python 3](https://www.python.org/download/releases/3.0/)

### Tor service
```bash
apt-get install -y tor
```


## 📚 Currently supported Search engines
- ahmia
- darksearchio
- clone_systems_engine
- notevil
- darksearchenginer
- phobos
- onionsearchserver
- torgle
- torgle1
- onionsearchengine
- tordex
- tor66
- tormax
- haystack
- multivac
- evosearch
- deeplink
- torch
- senator
- demon
- excavator


## 🛠️ Installation

### Standalone from Github

```bash
cd /opt/
git clone https://github.com/clonesec/darkweb-engine.git
```
```bash
# Copy NASL scripts to private NASL directory
cd darkweb-engine/
ls darkweb* | awk ' { print "cp "$1" /usr/local/var/lib/openvas/plugins/private/"$1 }' | sed 'e'
```
```bash
pwd
# Add this path in $PATH variable. For this type in terminal
pip install -r requirements.txt
cd ~
```
```bash
sudo nano .bashrc
```
```bash
# then write at the end (if PWD = /opt/darkweb-engine):
alias cgdarkwebsearch="/opt/darkweb-engine/cgdarkwebsearch.py"
alias OnionSearchParser="/opt/darkweb-engine/OnionSearchParser.py"
```
```bash
# save and close it and then:
source ~/.bashrc

```

### Docker local version

You can either build a Darkweb-engine image through the provided Dockerfile:

```bash
docker build -t darkweb .
```
Or you can download the already compiled base image:

```bash
# login to Clone's docker hub
docker login hub.clone-systems.com

# Download the base image
docker pull hub.clone-systems.com/scanning/darkweb:1.0.0

# Run a standalone darkweb scan from the host without any dependencies
docker run --rm --name darkweb_run -v $(pwd):/opt/darkweb-engine/ darkweb --search mysearchterm --url https://example.com

# The result .txt will be in the directory that you ran the command
```
### Build Darkweb multiarch
```bash
# define the target platform of your pc
# build a scross-platform buildx instance
docker buildx create --name multibuilder --platform "linux/amd64,linux/arm64" --bootstrap --use 

# inspect the buildx instance
docker buildx inspect multibuilder

docker buildx build --push --platform linux/amd64,linux/arm64 --tag hub.clone-systems.com/scanning/darkweb:buildx-latest .
```


## 📈  Usage

#### To test a single NASL script
```bash
cd /usr/local/var/lib/openvas/plugins/
```
```bash
openvas-nasl -X -B -d -i /usr/local/var/lib/openvas/plugins -c /usr/local/sbin/openvassd.config -t www.facebook.com /usr/local/var/lib/openvas/plugins/private/darkwebsearcher.nasl
```


#### To change OpenVAS default search engines:
login to greenbone web interface and go to each NASL script and change the search engines


#### Help:
```
usage: cgdarkwebsearch [-h] [--proxy PROXY] [--output OUTPUT]
                  [--search [SEARCH [SEARCH ...]]]
                  [--continuous_write CONTINUOUS_WRITE] 
                  [--limit LIMIT]
                  [--engines [ENGINES [ENGINES ...]]]
                  [--exclude [EXCLUDE [EXCLUDE ...]]]
                  [--field_delimiter FIELD_DELIMITER] 
                  [--mp_units MP_UNITS]
                  


optional arguments:
  -h, --help            show this help message and exit
  --search              The search string or phrase (split words with __, e.g. facebook__hack)
  --proxy PROXY         Set Tor proxy (default: 127.0.0.1:9050)
  --output OUTPUT       Output File (default: output_$SEARCH_$DATE.txt), where $SEARCH is replaced by the first chars of the search string and $DATE is replaced by the datetime
  --continuous_write CONTINUOUS_WRITE
                        Write progressively to output file (default: False)
  --limit LIMIT         Set a max number of pages per engine to load
  --engines [ENGINES [ENGINES ...]]
                        Engines to request (default: full list)
  --exclude [EXCLUDE [EXCLUDE ...]]
                        Engines to exclude (default: none)
  --field_delimiter FIELD_DELIMITER
                        Delimiter for the CSV fields
  --mp_units MP_UNITS   Number of processing units (default: core number minus 1)

[...]
```

### Dev Usage

To integrate on other pipelines:

```
from utils import darkweb_engine as d_eng


new_search = d_eng.DarkWebSearch(args, args.search, args.engines, args.exclude, args.mp_units, args.output)
new_search.run()
new_search.print_search_info()
```

### Check engine status

To check which engines are up and down:

```
from utils import check_engine_status as status

check_eng = status.CheckEngineStatus()
check_eng.run()
print("Up: ", str(check_eng.up))
print("Down: ", str(check_eng.down))
```

### Multi-processing behaviour

By default, the script will run with the parameter `mp_units = cpu_count() - 1`. It means if you have a machine with 4 cores,
it will run 3 scraping functions in parallel. You can force `mp_units` to any value but it is recommended to leave to default.
You may want to set it to 1 to run all requests sequentially (disabling multi-processing feature).

Please note that continuous writing to csv file has not been *heavily* tested with multiprocessing feature and therefore
may not work as expected.

Please also note that the progress bars may not be properly displayed when `mp_units` is greater than 1.
**It does not affect the results**, so don't worry.

### Examples

To request all the engines for the word "computer":
```
onionsearch "computer"
```

To request all the engines excepted "Ahmia" and "Candle" for the word "computer":
```
onionsearch "computer" --exclude ahmia candle
```

To request only "Tor66", "DeepLink" and "Phobos" for the word "computer":
```
onionsearch "computer" --engines tor66 deeplink phobos
```

The same as previously but limiting to 3 the number of pages to load per engine:
```
onionsearch "computer" --engines tor66 deeplink phobos --limit 3
```

Please kindly note that the list of supported engines (and their keys) is given in the script help (-h).


### Output

#### Default output

By default, the file is written at the end of the process. The file will be csv formatted, containing the following columns:
```
"engine","name of the link","url"
```

#### Customizing the output fields

You can customize what will be flush in the output file by using the parameters `--fields` and `--field_delimiter`.

`--fields` allows you to add, remove, re-order the output fields. The default mode is show just below. Instead, you can for instance
choose to output:
```
"engine","name of the link","url","domain"
```
by setting `--fields engine name link domain`.

Or even, you can choose to output:
```
"engine","domain"
```
by setting `--fields engine domain`.

These are examples but there are many possibilities.

Finally, you can also choose to modify the CSV delimiter (comma by default), for instance: `--field_delimiter ";"`.

#### To add new search engines:
    1. engines.json: add new engine name and url at the end of the file
    2. get_data.py: add new engine scrap instructions in link_finder()
    3. scrap_engine.py: add new engine function for scraping at the end of the file

#### Changing filename

The filename will be set by default to `output_$DATE_$SEARCH.txt`, where $DATE represents the current datetime and $SEARCH the first
characters of the search string.

You can modify this filename by using `--output` when running the script, for instance:
```
onionsearch "computer" --output "\$DATE.csv"
onionsearch "computer" --output output.txt
onionsearch "computer" --output "\$DATE_\$SEARCH.csv"
...
```
(Note that it might be necessary to escape the dollar character.)

In the csv file produced, the name and url strings are sanitized as much as possible, but there might still be some problems...

#### Write progressively

You can choose to progressively write to the output (instead of everything at the end, which would prevent
losing the results if something goes wrong). To do so you have to use `--continuous_write True`, just as is:
```
onionsearch "computer" --continuous_write True
```
You can then use the `tail -f` (tail follow) Unix command to actively watch or monitor the results of the scraping.
