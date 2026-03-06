import sys

class Feature():
    def on_init(self):
         return 'Constructor'

    def collectFromPcap(self, pcap):
        return  'Feature: collect from pcap :' + pcap

    def collectFromInterfaces(self, interfaces):
     #   response = 'Feature: collect from interfaces\n'
        print("Feature: interfaces:")
      #  response = response + "Feature: interfaces:\n";
        for i, interface in enumerate(interfaces):
       #     response = response + "Feature: interface " + str(i) + ":" + str(interface) + "\n";
            print("Feature: interface " + str(i) + ":" + str(interface) )
        return "ok";

def start(pcap):
        return  'start collect from 2:' + pcap

def collectTest(sourceNf,csvNf):
    print("collectTest")
    print(sourceNf)
    print(csvNf)
    return sourceNf + " - " + csvNf

if len(sys.argv)==4:
    methodName = sys.argv[1]
    sourceNf =  sys.argv[2]
    csvNf =  sys.argv[3]
    #if methodName=='collect':
    #    collect(sourceNf,csvNf)
    if methodName=='collectTest':
        collectTest(sourceNf,csvNf)