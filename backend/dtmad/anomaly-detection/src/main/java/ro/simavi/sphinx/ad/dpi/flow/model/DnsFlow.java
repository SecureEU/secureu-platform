package ro.simavi.sphinx.ad.dpi.flow.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

////Delete
@Getter
@Setter
@ToString(callSuper=true, includeFieldNames=true)
public class DnsFlow extends ProtocolFlow{

    private long numQueries = 0;

    private long numAnswers = 0;

    private long retCode = 0; // se caluleaza (pa baza lui 'flags') sau se mapeaza ( pe baza lui 'rcode', pentru ca trebuie o valoare numerica) // e in answer

    private Integer queryType; // rrtype , trebuie facuta o mapare folosind enum-ul "RRType" ( e in query)

    private Integer rspType; // rrtype, trebuie facuta o mapare folosind enum-ul "RRType" ( e in answer)
//
  //  private long flags; // flags -> e intr-un answer

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{ Nr query: " + getNumQueries());
        stringBuilder.append(", Nr answer: " + getNumAnswers());
        stringBuilder.append(", AvgPacketSize: " + getAvgPacketSize());
        stringBuilder.append(", PacketsWithoutPayload " + getPacketsWithoutPayload());
        stringBuilder.append(", AvgInterTime: " + getAvgInterTime() );
        stringBuilder.append(", Flow duration: " + getFlowDuration() );
        stringBuilder.append(", Bytes: " + getBytes());
        stringBuilder.append(", Packets: " + getPackets());
        stringBuilder.append(", Flags: " + getFlags());
        stringBuilder.append(", Query: " + getQueryType());
        stringBuilder.append(", Answer: " + getRspType());
        stringBuilder.append(", Max Packet Size:" + getMaxPacketSize());
        stringBuilder.append(", Min Packet Size:" + getMinPacketSize());
//        stringBuilder.append(", InterTime 0:" + getInterTime0());
        stringBuilder.append("}");
        return stringBuilder.toString();
    }


}

//private long badPacket; //!!nu se mai calculeaza

// extra
//private long authorityRrs = 0; // NSCOUNT // count ("authorities"), nu mai are sens daca badPacket e desprecated

// private long additionalRrs = 0; // ARCOUNT // count ("additional"),  nu mai are sens daca badPacket e desprecated


/*
1.
{
   "timestamp":"2020-10-14T19:02:00.834100GTB Daylight Time",
   "flow_id":1311281985927732,
   "in_iface":"\\Device\\NPF_{5A8F4FB6-5E07-48A9-9C4B-E19DA3A1FF69}",
   "event_type":"dns",
   "src_ip":"2a02:2f0a:4603:5c00:706f:e433:61f1:51d4",
   "src_port":63686,
   "dest_ip":"2a02:2f0c:8000:0008:0000:0000:0000:0001",
   "dest_port":53,
   "proto":"UDP",
   "dns":{
      "type":"query",
      "id":2056,
      "rrname":"ent-shasta-rrs.symantec.com",
      "rrtype":"AAAA",
      "tx_id":0
   }
}

2.
{
   "timestamp":"2020-10-14T19:02:00.838518GTB Daylight Time",
   "flow_id":1311281985927732,
   "in_iface":"\\Device\\NPF_{5A8F4FB6-5E07-48A9-9C4B-E19DA3A1FF69}",
   "event_type":"dns",
   "src_ip":"2a02:2f0a:4603:5c00:706f:e433:61f1:51d4",
   "src_port":63686,
   "dest_ip":"2a02:2f0c:8000:0008:0000:0000:0000:0001",
   "dest_port":53,
   "proto":"UDP",
   "dns":{
      "version":2,
      "type":"answer",
      "id":2056,
      "flags":"8180",
      "qr":true, // QR - A one bit field that specifies whether this message is a query (0), or a response (1)
      "rd":true, // RD - Recursion Desired - this bit directs the name server to pursue the query recursively. You should use 1, representing that you desire recursion.
      "ra":true, // RA - Recursion Available - this be is set or cleared in a response, and denotes whether recursive query support is available in the name server. Recursive query support is optional. You must exit and return an error if you receive a response that indicates the server does not support recursion.
      "rrname":"ent-shasta-rrs.symantec.com",
      "rrtype":"AAAA", // Resource Record Type (ex: A, AAAA, NS, PTR)
      "rcode":"NOERROR", RCODE - Response code - this 4 bit field is set as part of responses. The values have the following interpretation:
            0 No error condition
            1 Format error - The name server was unable to interpret the query.
            2
            2 Server failure - The name server was unable to process this query due to a problem with
            the name server.
            3 Name Error - Meaningful only for responses from an authoritative name server, this code
            signifies that the domain name referenced in the query does not exist.
            4 Not Implemented - The name server does not support the requested kind of query.
            5 Refused - The name server refuses to perform the specified operation for policy reasons.
      "answers":[
         {
            "rrname":"ent-shasta-rrs.SYMANTEC.com",
            "rrtype":"CNAME",
            "ttl":1253,
            "rdata":"col-rrs-prod-pub.trafficmanager.net"
         },
         {
            "rrname":"col-rrs-prod-pub.trafficmanager.net",
            "rrtype":"CNAME",
            "ttl":6,
            "rdata":"mue1-colossus-rrs-prod02.eastus.cloudapp.azure.com"
         },
         {
            "rrname":"mue1-colossus-rrs-prod02.eastus.cloudapp.azure.com",
            "rrtype":"AAAA",
            "ttl":9,
            "rdata":"2603:1030:020e:0003:0000:0000:0000:0171"
         }
      ],
      "grouped":{
         "AAAA":[
            "2603:1030:020e:0003:0000:0000:0000:0171"
         ],
         "CNAME":[
            "col-rrs-prod-pub.trafficmanager.net",
            "mue1-colossus-rrs-prod02.eastus.cloudapp.azure.com"
         ]
      }
   }
}

3.

{
   "timestamp":"2020-10-14T19:02:12.524819GTB Daylight Time",
   "flow_id":1311281985927732,
   "in_iface":"\\Device\\NPF_{5A8F4FB6-5E07-48A9-9C4B-E19DA3A1FF69}",
   "event_type":"flow",
   "src_ip":"2a02:2f0a:4603:5c00:706f:e433:61f1:51d4",
   "src_port":63686,
   "dest_ip":"2a02:2f0c:8000:0008:0000:0000:0000:0001",
   "dest_port":53,
   "proto":"UDP",
   "app_proto":"dns",
   "flow":{
      "pkts_toserver":1,
      "pkts_toclient":1,
      "bytes_toserver":107,
      "bytes_toclient":269,
      "start":"2020-10-14T19:02:00.834100GTB Daylight Time",
      "end":"2020-10-14T19:02:00.838518GTB Daylight Time",
      "age":0,
      "state":"established",
      "reason":"shutdown",
      "alerted":false
   }
}

Alte exemple:

{
   "timestamp":"2020-10-14T19:27:44.049847GTB Daylight Time",
   "flow_id":613203872625397,
   "in_iface":"\\Device\\NPF_{5A8F4FB6-5E07-48A9-9C4B-E19DA3A1FF69}",
   "event_type":"dns",
   "src_ip":"2a02:2f0a:4603:5c00:706f:e433:61f1:51d4",
   "src_port":50762,
   "dest_ip":"2a02:2f0c:8000:0003:0000:0000:0000:0001",
   "dest_port":53,
   "proto":"UDP",
   "dns":{
      "version":2,
      "type":"answer",
      "id":46513,
      "flags":"8183",
      "qr":true,
      "rd":true,
      "ra":true,
      "rrname":"BRW30F772767079.main.siveco.ro",
      "rrtype":"A",
      "rcode":"NXDOMAIN",
      "authorities":[
         {
            "rrname":"siveco.ro", // Resource Record Name (ex: a domain name)
            "rrtype":"SOA",
            "ttl":7494
         }
      ]
   }
}

/*
Alte observatii:
 rcode
 qtype (dns question type) // response

 https://www2.cs.duke.edu/courses/fall16/compsci356/DNS/DNS-primer.pdf
 http://fga.unb.br/articles/0001/4752/TCC_Macario.pdf

 https://github.com/fireflyc/ndpi-demo/blob/master/deps/nDPI-1.6/src/lib/protocols/dns.c
 https://www.youtube.com/watch?v=qYh6k-S5xC4
 https://delaat.net/rp/2013-2014/p66/report.pdf

// eve.json: "event_type":"dns"
//  => count("type": "query")"=> dns_num_queries
//  => count("type": "answer")"=> dns_num_answers
//  => "rrtype":x => dns_query_type, x = {"AAAA", "NS", "A", "SOA", etc}
       /*
        1 A => A host address
        2 NS => An authoritative name server
        6 SOA => Marks the start of a zone of authority
        12 => PTR A domain name pointer
        16 => TXT Text strings
        28 => AAAA IPv6 Address
        33 => SRV Server Selection
        43 => DS Delegation Signer
        48 => DNSKEY DNSKEY
        */
// "rcode":x => dns_ret_code => resulting state of a request, x = {NOERROR, NXDOMAIN }
    /*
        0 => No Error NOERROR
        2 => Server Failure
        3 => Non-Existent Domain NXDOMAIN
        5 => Query Refused
        https://www.iana.org/assignments/dns-parameters/dns-parameters.xhtml#dns-parameters-6

 */
