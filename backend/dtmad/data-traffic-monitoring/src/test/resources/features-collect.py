from nfstream import NFStreamer, NFPlugin
from psutil import net_if_addrs, cpu_count
import dpkt
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

def collectTest(sourceNf,csvNf):
    print(sourceNf + " - " + csvNf)
    return sourceNf + " - " + csvNf

def collect(sourceNf,csvNf):
    streamer = NFStreamer(source=sourceNf,
                          udps=FeaturesKmeans(),
                          n_dissections=20,
                          splt_analysis=10,
                          statistical_analysis=True)

    # for flow in streamer: # Work also with to_pandas, to_csv
    #     print(".")

    total_flows_count = streamer.to_csv(path=csvNf, columns_to_anonymize=[], flows_per_file=0)
    print("Floww" + str(total_flows_count))
    return "ok"

if len(sys.argv)==4:
    methodName = sys.argv[1]
    sourceNf =  sys.argv[2]
    csvNf =  sys.argv[3]
    if methodName=='collect':
       collect(sourceNf,csvNf)
    #if methodName=='collectTest':
    #    collectTest(sourceNf,csvNf)

    possibles = globals().copy()
    possibles.update(locals())
    method = possibles.get(methodName)
    if not method:
        raise NotImplementedError("Method %s not implemented" % methodName)
    print("source:"+sourceNf)
    print("csvNf:"+csvNf)
    print("methodName:"+methodName)


    available_interfaces = net_if_addrs().keys()
    for ai in available_interfaces:
        print(ai)
        method(ai,ai+" "+csvNf)






# python3 features-collect.py collect capture.pcap /nfstream/final1
# python3 features-collect.py collect lo /nfstream/final1
# python3 features-collect.py collect lo /nfstream/final2.csv
# sudo python3 features-collect.py collect ens160 /nfstream/collectTraffic.csv
# python3 features-collect.py collect fpcap.pcap /nfstream/final-http.csv

# python3 features-collect.py collect pcaps/local.pcap /nfstream/local.csv