package ro.simavi.sphinx.ad.services.impl;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ro.simavi.sphinx.ad.configuration.AdConfigProps;
import ro.simavi.sphinx.ad.kernel.enums.AdTools;
import ro.simavi.sphinx.ad.kernel.enums.AdmlGeneralEnum;
import ro.simavi.sphinx.ad.kernel.enums.AdmlReputationEnum;
import ro.simavi.sphinx.ad.kernel.enums.SflowAlgorithm;
import ro.simavi.sphinx.ad.kernel.hbase.AdmlHBaseRDD;
import ro.simavi.sphinx.ad.services.AdConfigService;
import ro.simavi.sphinx.ad.services.ReputationService;
import ro.simavi.sphinx.model.ConfigModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// updateReputationList.php

@Service
public class ReputationServiceImpl implements ReputationService {

    private static final Logger logger = LoggerFactory.getLogger(ReputationServiceImpl.class);

    private static String BOT_NET_BLACKLIST_DEFAULT = "https://rules.emergingthreats.net/blockrules/emerging-botcc.rules";

    private final AdConfigService adConfigService;

    private final AdConfigProps adConfigProps;

    private String OS_REPO_WINDOWS_DEFAULT = "windowsupdate.microsoft.com," +
            "update.microsoft.com," +
            "windowsupdate.com," +
            "download.windowsupdate.com," +
            "download.microsoft.com," +
            "download.windowsupdate.com," +
            "ntservicepack.microsoft.com," +
            "time.windows.com," +
            "javadl-esd.sun.com," +
            "fpdownload.adobe.com," +
            "cache.pack.google.com," +
            "aus2.mozilla.org," +
            "aus3.mozilla.org," +
            "aus4.mozilla.org," +
            "avast.com," +
            "files.avast.com";

    private String OS_REPO_LINUX_DEFAULT = "security.ubuntu.com," +
            "security.debian.org," +
            "mirrorlist.centos.org," +
            "0.rhel.pool.ntp.org," +
            "1.rhel.pool.ntp.org," +
            "2.rhel.pool.ntp.org," +
            "ntp.ubuntu.com," +
            "linux.dropbox.com";

    private String OS_REPO_ANDROID_DEFAULT = "play.google.com," +
            "android.clients.google.com";

    private String OS_REPO_APPLE_DEFAULT = "phobos.apple.com," +
            "deimos3.apple.com," +
            "albert.apple.com," +
            "gs.apple.com," +
            "itunes.apple.com," +
            "ax.itunes.apple.com";

    private String OS_REPO_FREEBSD_DEFAULT = "ftp.freebsd.org";

    private String BIG_PROVIDER_WHITELIST_DEFAULT = "";
    private String PROXY_SERVER_WHITELIST_DEFAULT = "";
    private String MX_WHITELIST_DEFAULT = "10.1.1.1#SMTP Server";
    private String TTALKER_WHITELIST_DEFAULT = "10.1.1.1#Big Talker 1,10.1.111.#DMZ";

    private HashMap<String, String> defaults;

    public ReputationServiceImpl(AdConfigService adConfigService, AdConfigProps adConfigProps) {
        this.adConfigService = adConfigService;
        this.adConfigProps = adConfigProps;

        this.defaults = new HashMap<>();
        defaults.put("BigProvider.whitelist", BIG_PROVIDER_WHITELIST_DEFAULT);
        defaults.put("ProxyServer.whitelist", PROXY_SERVER_WHITELIST_DEFAULT);
        defaults.put("MX.whitelist", MX_WHITELIST_DEFAULT);
        defaults.put("TTalker.whitelist", TTALKER_WHITELIST_DEFAULT);
        defaults.put("OSRepo.windows", OS_REPO_WINDOWS_DEFAULT);
        defaults.put("OSRepo.linux", OS_REPO_LINUX_DEFAULT);
        defaults.put("OSRepo.android", OS_REPO_ANDROID_DEFAULT);
        defaults.put("OSRepo.apple", OS_REPO_APPLE_DEFAULT);
        defaults.put("OSRepo.freebsd", OS_REPO_FREEBSD_DEFAULT);
    }

    @Override
    public void update() {

        updateCnCList();
        updateReposList();

        updateMyNet();
    }

    private void updateMyNet() {
        // put 'adml_mynets', '10.', 'net:description', 'Intranet 1'
        // put 'adml_mynets', '10.', 'net:prefix', '10.'
        deleteMynetsTable();
        String key = "ad.adml.sflow.mynets.net";
        String propertyValue = (String) adConfigProps.getTools().get(AdTools.SFLOW).getGenerals().get(AdmlGeneralEnum.MYNETS).getProperties().get(key);

        String[] ips = getIP("mynets", propertyValue);
        insertToHBase(ips);

    }

    @Override
    public void updateCnCList() {
        deleteAll("CCBotNet", "blacklist");
        String key = "ad.adml.sflow.BotNet.url";
        String propertyValue = (String) adConfigProps.getTools().get(AdTools.SFLOW).getAlgorithms().get(SflowAlgorithm.BOT_NET).getProperties().get(key);
        String url = getConfigValue(key, propertyValue, BOT_NET_BLACKLIST_DEFAULT);
        parse(url);
    }

    private String getConfigValue(String prop, String propertyValue, String defaultValue) {
        String url = null;
        if (adConfigService != null) {
            Map<String, ConfigModel> configModelMap = adConfigService.getConfigs();
            ConfigModel configModel = configModelMap.get(prop);
            if (configModel != null) {
                url = configModel.getValue();
            } else {
                if (propertyValue != null) {
                    url = propertyValue;
                }
            }
        }
        if (url == null) {
            url = defaultValue;
        }
        return url;
    }

    private void deleteMynetsTable() {
        Table table = AdmlHBaseRDD.getInstance().getAdmlMynetsTable();
        Scan scan = new Scan();
        deleteAll(table, scan);
    }

    private void deleteAll(String listName, String listType) {
        Table table = AdmlHBaseRDD.getInstance().getAdmlReputationTable();

        Filter filter1 = new SingleColumnValueFilter(Bytes.toBytes("rep"), Bytes.toBytes("list_type"), CompareOperator.EQUAL, Bytes.toBytes(listType));
        Filter filter2 = new SingleColumnValueFilter(Bytes.toBytes("rep"), Bytes.toBytes("list"), CompareOperator.EQUAL, Bytes.toBytes(listName));

        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
        filterList.addFilter(filter1);
        filterList.addFilter(filter2);

        Scan scan = new Scan();
        scan.setFilter(filterList);
        deleteAll(table, scan);
    }

    private void deleteAll(Table table, Scan scan){
        try {
            if (table==null){
                logger.error("data cannot be deleted [check hbase]");
                return;
            }
            ResultScanner resultScanner = table.getScanner(scan);
            for (Result rs : resultScanner) {
                table.delete(new Delete(rs.getRow()));
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void updateReposList() {

        int countIP = 0;
        for (AdmlReputationEnum reputation : AdmlReputationEnum.values()) {

            String list = reputation.getName();

            Map<String, Object> propertiesMap = adConfigProps.getTools().get(AdTools.SFLOW).getReputations().get(reputation).getProperties();

            if (propertiesMap != null) {
                for (Map.Entry<String, Object> prop : propertiesMap.entrySet()) {

                    String key = prop.getKey();
                    String[] keyParts = key.split("\\.");
                    String listType = keyParts[keyParts.length - 1];
                    //String key = list+"."+listType;

                    String values = getConfigValue(key, (String) prop.getValue(), defaults.get(key));

                    if (!StringUtils.isEmpty(values)) {
                        deleteAll(list, listType);
                        String[] ips = getIP(list, values);
                        countIP += ips.length;
                        insertToHBase(list, listType, ips);
                    }
                }
            }
        }
        logger.info("Reputation update: {} IPs processed", countIP);
    }

    private String[] getIP(String list, String urls)  {
        String[] urlArray = urls.split(",");
        List<String> urlList = new ArrayList<>();
        for (String url : urlArray) {

            String[] urlSplit = url.split("#");
            String ipOnly = url;
            String description = "";
            if (urlSplit.length > 1) {
                ipOnly = urlSplit[0];
                description = urlSplit[1];
            }

            if ("OSRepo".equals(list)) {
                try {
                    InetAddress[] iaddress = InetAddress.getAllByName(ipOnly);

                    for (InetAddress ipaddresses : iaddress) {
                        String ip = ipaddresses.toString();
                        String[] ipParts = ip.split("/");
                        if (ipParts.length>1){
                            ip = ipParts[1];
                        }

                        if (ip.contains(":")) {
                            IPAddressString addrString = new IPAddressString(ip);
                            IPAddress addr = addrString.getAddress();
                            ip = addr.toFullString();
                        }
                        if (StringUtils.isEmpty(description)) {
                            urlList.add(ip);
                        } else {
                            urlList.add(ip + "#" + description);
                        }
                    }
                } catch (UnknownHostException e) {
                    logger.warn("DNS resolution failed for {}: {}", ipOnly, e.getMessage());
                } catch (Exception e) {
                    logger.error(e.getMessage());
                }
            } else {
                if (StringUtils.isEmpty(description)) {
                    urlList.add(ipOnly);
                } else {
                    urlList.add(ipOnly + "#" + description);
                }
            }

        }

        return urlList.toArray(new String[0]);
    }

    private void parse(String theUrl) {
        try {
            URL url = new URL(theUrl);

            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            int countIP = 0;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String result = StringUtils.substringBetween(line, "[", "]");
                if (result != null) {
                    String ipList[] = result.split(",");
                    countIP += ipList.length;
                    insertToHBase("CCBotNet", "blacklist", ipList);
                }

            }
            bufferedReader.close();
            logger.info("count ip: {}", countIP);
        } catch (UnknownHostException e) {
            logger.warn("DNS resolution failed for {}: {}", theUrl, e.getMessage());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    private void insertToHBase(String list, String listType, String ipList[]) {
        Table admlReputationTable = AdmlHBaseRDD.getInstance().getAdmlReputationTable();
        if (admlReputationTable==null){
            logger.error("[insertToHBase] Adml Reputation Table => does not exist [check hbase]");
            return;
        }
        byte[] columnFamily = Bytes.toBytes("rep");
        for (String ip : ipList) {
            try {
                String[] ipSplit = ip.split("#");
                String ipOnly = ipSplit[0];
                String description = "";
                if (ipSplit.length > 1) {
                    description = ipSplit[1];
                }
                Put putFlow = new Put(Bytes.toBytes(ip));

                putFlow.addColumn(columnFamily, Bytes.toBytes("list_type"), Bytes.toBytes(listType));
                putFlow.addColumn(columnFamily, Bytes.toBytes("list"), Bytes.toBytes(list));
                putFlow.addColumn(columnFamily, Bytes.toBytes("ip"), Bytes.toBytes(ipOnly));
                putFlow.addColumn(columnFamily, Bytes.toBytes("description"), Bytes.toBytes(description));

                admlReputationTable.put(putFlow);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
    }

    private void insertToHBase(String ipList[]) {
        Table admlReputationTable = AdmlHBaseRDD.getInstance().getAdmlMynetsTable();
        if (admlReputationTable==null){
            logger.error("[insertToHBase] Adml Mynets Table => does not exist [check hbase]");
            return;
        }

        byte[] columnFamily = Bytes.toBytes("net");
        for (String ip : ipList) {
            try {
                String[] ipSplit = ip.split("#");
                String ipOnly = ipSplit[0];
                String description = "";
                if (ipSplit.length > 1) {
                    description = ipSplit[1];
                }
                Put putFlow = new Put(Bytes.toBytes(ip));
                putFlow.addColumn(columnFamily, Bytes.toBytes("prefix"), Bytes.toBytes(ipOnly));
                putFlow.addColumn(columnFamily, Bytes.toBytes("description"), Bytes.toBytes(description));
                admlReputationTable.put(putFlow);
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
        }
    }

//    private void put(Table table, String rowKey, String columnFamily, String column, String value) throws IOException {
//        Put putFlow = new Put(Bytes.toBytes(rowKey));
//        putFlow.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column), Bytes.toBytes(value));
//        table.put(putFlow);
//    }
    // https://stackoverflow.com/questions/35168524/get-ipv6-full-format-in-java

}
