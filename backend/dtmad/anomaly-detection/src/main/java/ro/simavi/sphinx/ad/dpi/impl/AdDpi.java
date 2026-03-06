package ro.simavi.sphinx.ad.dpi.impl;

import ro.simavi.sphinx.ad.dpi.api.DPI;
import ro.simavi.sphinx.ad.dpi.exporter.DPIExporter;
import ro.simavi.sphinx.ad.dpi.exporter.NoneDPIExporter;
import ro.simavi.sphinx.ad.dpi.flow.builder.DnsFlowBuilder;
import ro.simavi.sphinx.ad.dpi.flow.builder.FlowBuilder;
import ro.simavi.sphinx.ad.dpi.flow.builder.HttpFlowBuilder;
import ro.simavi.sphinx.ad.dpi.flow.builder.SFlowBuilder;
import ro.simavi.sphinx.ad.dpi.flow.model.ProtocolFlow;
import ro.simavi.sphinx.ad.dpi.util.AdDpiStats;
import ro.simavi.sphinx.model.event.HogzillaModel;
import ro.simavi.sphinx.model.event.NFstreamModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdDpi implements DPI {

    private Map<String, List<NFstreamModel>> flowMap;

    private List<String> removeFlowIdList;

    private AdDpiStats adDpiStats;

    private DPIExporter dpiExporter;

    private long eventCount = 0;

    private long flowCount = 0;

    public AdDpi(DPIExporter dpiExporter){
        flowMap = new HashMap<>();
        removeFlowIdList = new ArrayList<>();
        this.adDpiStats = new AdDpiStats();
        this.dpiExporter = dpiExporter;
    }

    public AdDpi(){
        flowMap = new HashMap<>();
        removeFlowIdList = new ArrayList<>();
        this.dpiExporter = new NoneDPIExporter();
        this.adDpiStats = new AdDpiStats();
    }

    public void setDpiExporter(DPIExporter dpiExporter) {
        this.dpiExporter = dpiExporter;
    }

    public void process(NFstreamModel nfstreamModel) {
        adDpiStats.incrementEventCount();
        if (adDpiStats.getEventCount() % 10000 == 0){
            System.out.println("event-count (intermediar):"+adDpiStats.getEventCount());
        }
        if (nfstreamModel==null){
            return;
        }

        adDpiStats.addBytes(Long.parseLong(nfstreamModel.getSrcToDstBytes())+Long.parseLong(nfstreamModel.getDstToSrcBytes()));

//        String id = kmeansModel.getFlowId();  //communityID
//        if (id==null || "".equals(id.trim())){
//            id = kmeansModel.getFlowId();
//        }

        //trebuie sters
//        List<KmeansModel> kmeansModelList = flowMap.get(id);
            adDpiStats.incrementSingularFlowCount();

//            kmeansModelList = new ArrayList<>();
//            kmeansModelList.add(kmeansModel);
//            flowMap.put(id, kmeansModelList);
            processFlow(nfstreamModel);
            processSFlow(nfstreamModel);

    }

    public void processSFlow(NFstreamModel nfstreamModel){
        if (nfstreamModel==null){
            return;
        }
        ProtocolFlow protocolFlow = getSFlow(nfstreamModel);

        if (protocolFlow!=null) {
            saveSFlow(protocolFlow);
        }
    }

    public void processHogzilla(HogzillaModel hogzillaModel) {
        if (hogzillaModel==null){
            return;
        }
        ProtocolFlow protocolFlow = getHogzillaFlow(hogzillaModel);

        if (protocolFlow!=null) {
            saveFlow(protocolFlow);
        }
    }


    private void processFlow(NFstreamModel nfstreamModel){
        // process flow
        flowCount++;
        adDpiStats.incrementFlowCount();

        ProtocolFlow protocolFlow = getFlow(nfstreamModel);

        if (protocolFlow!=null) {
            saveFlow(protocolFlow);
        }
    }

    private ProtocolFlow getFlow(NFstreamModel nfstreamModel) {
        /* todo info: se construieste un obiect de tipul ProtocolFlow
            - pentru inceput se vor construi doar pentru DnsFlow si HttpFlow
        */
        ProtocolFlow protocolFlow = null;
        FlowBuilder flowBuilder = null;
        if (nfstreamModel.getProtocol().startsWith("DNS")){
            flowBuilder = new DnsFlowBuilder();
            protocolFlow = flowBuilder.build(nfstreamModel);
        } else if (nfstreamModel.getProtocol().startsWith("HTTP")){
            flowBuilder = new HttpFlowBuilder();
            protocolFlow = flowBuilder.build(nfstreamModel);
        }
        return protocolFlow;
    }

    private ProtocolFlow getSFlow(NFstreamModel nfstreamModel) {

        SFlowBuilder sflowBuilder = new SFlowBuilder();
        ProtocolFlow protocolFlow = sflowBuilder.buildSFlow(nfstreamModel);

        return protocolFlow;
    }

    private ProtocolFlow getHogzillaFlow(HogzillaModel hogzillaModel) {
        ProtocolFlow protocolFlow = null;
        FlowBuilder flowBuilder = null;
        if (hogzillaModel.getDestPort()!=null && hogzillaModel.getDestPort().equals("53")){
            flowBuilder = new DnsFlowBuilder();
            protocolFlow = flowBuilder.buildHogzilla(hogzillaModel);

        } else if (hogzillaModel.getDestPort()!=null && hogzillaModel.getDestPort().equals("80")){
            flowBuilder = new HttpFlowBuilder();
            protocolFlow = flowBuilder.buildHogzilla(hogzillaModel);
        }
        return protocolFlow;
    }

    public void saveFlow(ProtocolFlow protocolFlow){
        dpiExporter.save(protocolFlow);
    }

    public void saveSFlow(ProtocolFlow protocolFlow){
        dpiExporter.saveSflow(protocolFlow);
    }

    private void endProcess() {
        System.out.println("flow-count:"+ flowCount);
        System.out.println("flow-map(intial):"+flowMap.size());

        for(String flowId: removeFlowIdList){
            flowMap.remove(flowId);
        }
        System.out.println("flow-map(final):"+flowMap.size());
        System.out.println("remove-garbage-flow-id:"+removeFlowIdList.size());


    }


    /*todo 1: de construit un scheduler care din 1 in 1 minut sa apeleze urmatoarea metoda*/
    @Override
    public void gc() {
        this.endProcess();
        this.dpiExporter.endProcess();
    }
}
