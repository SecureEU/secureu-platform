from nfstream import NFStreamer, NFPlugin
from psutil import net_if_addrs, cpu_count
import dpkt
import re
import sys

class FeaturesKmeans(NFPlugin):
    def on_init(self, packet, flow):
        flow.udps.queryType = 0
        flow.udps.rspType = 0
        flow.udps.flags = ""
        flow.udps.uri = ""
        flow.udps.method = 0
        flow.udps.host = ""
        flow.udps.url = ""
        self.on_update(packet, flow)

    def on_update(self, packet, flow):

        # syn	bool	TCP SYN Flag present.
        # cwr	bool	TCP CWR Flag present.
        # ece	bool	TCP ECE Flag present.
        # urg	bool	TCP URG Flag present.
        # ack	bool	TCP ACK Flag present.
        # psh	bool	TCP PSH Flag present.
        # rst	bool	TCP RST Flag present.
        # fin	bool	TCP FIN Flag present.

        # FIN 1 / 0x01
        # SYN 2 / 0x02
        # RST 4 / 0x04
        # PSH 8 / 0x08
        # AKT 16 / 0x10
        # URG 32 / 0x20
        # ECE 64 / 0x40
        # CWR 128 / 0x80

        sum = 0
        if packet.fin==True:
            sum += 1
        if packet.syn==True:
            sum += 2
        if packet.rst==True:
            sum += 4
        if packet.psh==True:
            sum += 8
        if packet.ack==True:
            sum += 16
        if packet.urg==True:
            sum += 32
        if packet.ece==True:
            sum += 64
        if packet.cwr==True:
            sum += 128

        # flow.udps.flags = "0x"+hex(sum)[2:].zfill(2)
        flow.udps.flags = hex(sum)[2:]

        if flow.application_name.startswith("DNS"):
            try:
                ip = dpkt.ip.IP(packet.ip_packet)
                udp = ip.data
                dns = dpkt.dns.DNS(udp.data)
                if packet.direction == 1:
                    flow.udps.queryType = dns.qd[0].type
                if packet.direction == 0:
                    flow.udps.rspType = dns.qd[0].type
                if packet.protocol == 17:
                    flags = hex((dns.qr << 15) | (dns.aa << 10) | (dns.rd << 8) | (dns.ra << 7))
                    splitFlags = flags.split("x")
                    flow.udps.flags = str(splitFlags[1])
            except (dpkt.NeedData, dpkt.UnpackError):
                return

        if flow.application_name.startswith("HTTP"):

            try:
                eth = dpkt.ethernet.Ethernet(packet.ip_packet)
                ip = dpkt.ip.IP(packet.ip_packet)

                # Check for TCP in the transport layer
                if isinstance(ip.data, dpkt.tcp.TCP):
                    # Set the TCP data
                    tcp = ip.data

                    # Now see if we can parse the contents as a HTTP request
                    try:
                        request = dpkt.http.Request(tcp.data)
                    except (dpkt.dpkt.NeedData, dpkt.dpkt.UnpackError):
                        return

                requestMethod = request.method;

                if requestMethod=="GET":
                    flow.udps.method = 20
                if requestMethod=="HEAD" :
                    flow.udps.method = 21
                if requestMethod=="POST":
                    flow.udps.method = 22
                if requestMethod=="PUT":
                    flow.udps.method = 23
                if requestMethod=="PATCH":
                    flow.udps.method = 24
                if requestMethod=="DELETE":
                    flow.udps.method = 25

                flow.udps.host = request.headers['host']
                flow.udps.url = request.uri

                if flow.udps.url != " ":
                    flow.udps.uri = flow.udps.host + "/" + flow.udps.url
                else:
                    flow.udps.uri = flow.udps.host

                if flow.udps.host == "":
                    flow.udps.uri = ""

                #print(flow.udps.host)
                #print(flow.udps.url)
                #print(flow.udps.uri)
                #print(flow.udps.method)

            except (dpkt.NeedData, dpkt.UnpackError):
                return

# Fallback Windows NPF device when source is "all" and tshark -D discovery fails (replace with your \Device\NPF_{GUID})
PREFERRED_WINDOWS_NPF_DEVICE = r"\Device\NPF_{4C1D2556-A6D8-4E61-BFF0-BE4A381C5456}"


def _get_windows_npf_from_tshark():
    """Run tshark -D and return the first \\Device\\NPF_{GUID} found, or None."""
    import subprocess
    import re
    # Match \Device\NPF_{GUID} in tshark -D output (e.g. "1. \\Device\\NPF_{...} (Ethernet)")
    npf_pattern = re.compile(r"\\Device\\NPF_\{[A-Fa-f0-9\-]+\}")
    for cmd in (["tshark", "-l", "-D"], [r"C:\Program Files\Wireshark\tshark.exe", "-l", "-D"]):
        try:
            out = subprocess.check_output(cmd, stderr=subprocess.DEVNULL, timeout=5, text=True)
            for line in out.splitlines():
                m = npf_pattern.search(line)
                if m:
                    return m.group(0)
        except (FileNotFoundError, subprocess.CalledProcessError, subprocess.TimeoutExpired):
            continue
    return None


def _resolve_source(sourceNf):
    """If source is 'all', return preferred device (Windows NPF or Ethernet/Wi-Fi); else return as-is."""
    if sourceNf and sourceNf.strip().lower() != 'all':
        return sourceNf.strip()
    # On Windows: discover first NPF interface via tshark -D so we don't rely on a hardcoded GUID
    if sys.platform == "win32":
        npf = _get_windows_npf_from_tshark()
        if npf:
            return npf
        return PREFERRED_WINDOWS_NPF_DEVICE
    addrs = net_if_addrs()
    skip = ('lo', 'loopback', 'npcap', 'nfloopback', 'vethernet', 'wsl', 'hyper-v', 'virtual', 'vmware', 'vbox')
    candidates = [name for name in addrs if not any(s in name.lower() for s in skip)]
    if not candidates:
        return list(addrs)[0] if addrs else None
    exact = [c for c in candidates if re.match(r'^(Ethernet|Wi-Fi)$', c, re.I)]
    no_number = [c for c in candidates if re.match(r'^(.+?)\s+\d+$', c) is None and c not in exact]
    rest = [c for c in candidates if c not in exact and c not in no_number]
    ordered = exact + no_number + rest
    return ordered[0] if ordered else candidates[0]

def collectTest(sourceNf,csvNf):
    print(sourceNf + " - " + csvNf)
    return sourceNf + " - " + csvNf

def collect(sourceNf,csvNf):
    resolved = _resolve_source(sourceNf)
    if not resolved:
        print("Streamer skipped: no valid source (source=all but no interface found)", file=sys.stderr)
        return 0
    if resolved != (sourceNf or "").strip():
        print("Resolved source 'all' -> '%s'" % resolved)
    plugin = FeaturesKmeans()
    # NFStreamer API varies: plugins= vs udps=; n_dissections/splt_analysis/statistical_analysis not in all versions
    streamer = None
    for try_plugins, try_udps in [(True, False), (False, True)]:
        try:
            if try_plugins:
                streamer = NFStreamer(source=resolved, plugins=[plugin])
            else:
                streamer = NFStreamer(source=resolved, udps=plugin)
            break
        except TypeError:
            continue
    if streamer is None:
        streamer = NFStreamer(source=resolved)

    total_flows_count = streamer.to_csv(path=csvNf, columns_to_anonymize=[], flows_per_file=0)
    print("Floww" + str(total_flows_count))
    return total_flows_count

def _main():
    import sys
    # citește argumentele
    if len(sys.argv) >= 4:
        methodName = sys.argv[1]
        sourceNf   = sys.argv[2]
        csvNf      = sys.argv[3]
    else:
        # fallback for local run: use preferred Windows NPF device or Ethernet
        methodName = "collect"
        sourceNf   = PREFERRED_WINDOWS_NPF_DEVICE if sys.platform == "win32" else "Ethernet"
        csvNf      = "flowNf.csv"

    print(f"source: {sourceNf}")
    print(f"csvNf: {csvNf}")
    print(f"methodName: {methodName}")

    # Selectarea metodei
    if methodName == 'collect':
        method = collect
    elif methodName == 'collectTest':
        method = collectTest
    else:
        possibles = globals().copy()
        possibles.update(locals())
        method = possibles.get(methodName)
        if not method:
            raise NotImplementedError("Method %s not implemented" % methodName)

    # Rularea metodei principale
    total = method(sourceNf, csvNf)
    print(f"Result: {total}")

    # ❗ Comentat: bucla care rulează pentru toate interfețele
    # pentru a evita probleme cu nume de fișiere invalide pe Windows
    # 
    # available_interfaces = list(net_if_addrs().keys())
    # print("Available interfaces:")
    # for ai in available_interfaces:
    #     print(ai)
    #     # Folosește un nume valid pentru fișier
    #     safe_filename = f"{ai.replace(':', '_')}_{csvNf}"
    #     method(ai, safe_filename)

if __name__ == "__main__":
    from multiprocessing import freeze_support
    freeze_support()  # necesar pe Windows când se pornesc procese noi
    _main()


# Exemple de utilizare:
# python3 features-collect.py collect capture.pcap /nfstream/final1
# python3 features-collect.py collect lo /nfstream/final1
# python3 features-collect.py collect lo /nfstream/final2.csv
# sudo python3 features-collect.py collect ens160 /nfstream/collectTraffic.csv
# python3 features-collect.py collect fpcap.pcap /nfstream/final-http.csv
# python3 features-collect.py collect pcaps/local.pcap /nfstream/local.csv