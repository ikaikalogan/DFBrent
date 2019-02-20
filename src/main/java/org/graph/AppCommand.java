/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graph;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.Weight;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.*;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.topology.*;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * Apache Karaf CLI command to determine the edge cut b/w 2 nodes and deploy ACLs
 */

@Command(scope = "onos", name = "kill",
         description = "Deploy Flow Rules to Min Min Edge Cut Set")

public class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "s", description = "source", required = true, multiValued = false)
    private int s = -1;
    @Argument(index = 1, name = "t"  , description = "destination", required = true, multiValued = false)
    private int t = -1;
    @Argument(index = 2, name = "r" , description = "# of rules", required = true, multiValued = false)
    private int r = 1;

    @Override
    public void execute() {
        log.info("Graph Application: Started with min cut between " + s + " and " + t);
        LinkListener linkListener = new LinkListener() {
            @Override
            public void event(LinkEvent event) {
                if (event.type() != null) {
                    try {
                        Thread.sleep(10);
                        int x, y=0, rulesbefore=0, rulesafter=0, rulesadded=0;
                        DeviceService deviceService = get(DeviceService.class);
                        TopologyService topologyService = get(TopologyService.class);
                        Topology topo = topologyService.currentTopology();
                        TopologyGraph graph = topologyService.getGraph(topo);
                        Set<TopologyEdge> edges = graph.getEdges(); //grab the edges from current topology
                        Set<TopologyVertex> vertexes = graph.getVertexes(); // grab the vertexes from the current topologyon
                        HashMap<DeviceId, Integer> idtonum = new HashMap<>();// deviceid to int
                        HashMap<Integer,String> idtonum2 = new HashMap<>();//for rules
                        HashMap<DeviceId, List> idtostats = new HashMap<>(); //device id to edges
                        HashMap<DeviceId, List> idtoports = new HashMap<>(); // devices and their associated ports
                        DefaultEdgeWeigher edgeWeigher = new DefaultEdgeWeigher();
                        FlowRuleService flowRuleService = get(FlowRuleService.class);
                        int devicenum = vertexes.size();
                        int[][] adjmatrix = new int[devicenum][devicenum];
                        DeviceId[] idlist = new DeviceId[devicenum];
                        Graph graph1 = new Graph(devicenum);
                        String[] result;
                        String printout= "";

                        for (TopologyVertex temp1: vertexes){
                            String name = String.valueOf(temp1);
                            DeviceId id = temp1.deviceId();
                            idtonum.put(id,y);
                            idtonum2.put(y,name);
                            idlist[y] = id;
                            List ports = deviceService.getPorts(id);
                            List stats = deviceService.getPortStatistics(id);
                            idtostats.put(id,stats);
                            idtoports.put(id,ports);
                            y++;
                        }
                        String linkchangetype = event.type().toString();
                        String linkchangename = event.type().name();
                        printout = printout.concat(" EVENT " + linkchangetype + "\n" + "NAME" + linkchangename +"\n");
                        printout = printout.concat(
                                "########################## device id to matrix number ##########################"+"\n");
                        Set set = idtonum.entrySet();
                        Iterator iterator = set.iterator();
                        while(iterator.hasNext()){
                            Map.Entry entry = (Map.Entry)iterator.next();
                            printout = printout.concat("Device id: " + entry.getKey() + " ; matrix number : " + entry.getValue() + "\n");
                        }
                        printout = printout.concat(
                                "###########################   WEIGHTS  #########################################"+"\n");
                        for (TopologyEdge edgetemp: edges) {
                            for (int j = 0; j < devicenum; j++) {
                                String edge = String.valueOf(edgetemp);
                                String src = null;
                                String dst = null;
                                String pattern ="((of:)(\\d)*)";
                                Pattern p = Pattern.compile(pattern);
                                Matcher matcher = p.matcher(edge);
                                if(matcher.find()) {
                                    src = matcher.group(0);
                                }
                                if( matcher.find(44)){
                                    dst = matcher.group(0);
                                }
                                DeviceId source = DeviceId.deviceId(src);
                                DeviceId destination = DeviceId.deviceId(dst);
                                DeviceId id = idlist[j];
                                if (id.equals(source)){
                                    int row = idtonum.get(id);
                                    int column = idtonum.get(destination);
                                    Weight weight = edgeWeigher.weight(edgetemp);
                                    String stringweight = String.valueOf(weight);
                                    int lastindex = stringweight.lastIndexOf('}');
                                    String sw = stringweight.substring(19,lastindex);

                                    float floatweight = Float.valueOf(sw);
                                    int intweight = Math.round(floatweight);
                                    adjmatrix[row][column] = intweight;
                                    adjmatrix[column][row] = intweight;
                                }
                            }
                        }
                        printout = printout.concat(
                                "##########################    MATRIX    ########################################"+"\n");
                        for (int i=0; i<devicenum;i++){
                            printout = printout.concat (
                                    "Row " + i + "-------------------------------------------------------- Row " + i+"\n");
                            for(int j = 0; j <devicenum; j++){
                                printout = printout.concat(adjmatrix[i][j] + "\n");
                            }
                        }
                        printout = printout.concat(
                                "######################### SHORTEST PATHS #######################################"+"\n");
                        ShortestPath shorty = new ShortestPath(devicenum);
                        int origin = s;
                        int[] shortydijkstra = shorty.dijkstra(adjmatrix, origin);
                        printout = printout.concat("Vertex   Distance from Source");
                        for (int i = 0; i < devicenum; i++) {
                            printout = printout.concat(i + "                 " + shortydijkstra[i]+"\n");
                        }
                        printout = printout.concat(
                                "##########################      CUT     ########################################"+"\n");
                        Vector devicelist = new Vector();
                        HashMap<Integer,Integer> nodedistance = new HashMap(); //Key:node Value: Distance from source
                        if ((s != -1) && (t != -1)) {
                            result = graph1.minCut(adjmatrix, s, t);
                            x = result.length;
                            y = 0;

                            for (int i = 0; i < x; i++) {
                                if (result[i] != null) {
                                    String sub = result[i];
                                    String[] subtwo = sub.split("-");
                                    int device0 = Integer.parseInt(subtwo[0]);
                                    devicelist.add(y, (device0));
                                    int distance0 = shortydijkstra[device0];
                                    nodedistance.put(device0,distance0);
                                    y++;
                                    int device1 = Integer.parseInt(subtwo[1]);
                                    devicelist.add(y, (device1));
                                    int distance1 = shortydijkstra[device1];
                                    nodedistance.put(device1,distance1);
                                    y++;
                                    printout = printout.concat(result[i] +"\n");
                                }
                            }
                        }
                        int devicelistsize = devicelist.size();
                        for (int i = 0; i < devicelistsize; i++) {
                            Object tempobject = devicelist.get(i);
                            String tempstring = tempobject.toString();
                            int tempint = Integer.valueOf(tempstring);
                            printout = printout.concat(" Device ID: " + idtonum2.get(tempint) + "  Matrix id: " + tempint+"\n");
                        }
                        printout = printout.concat(
                                "######################### MIN MIN CUT    #######################################"+"\n");
                            int i = 0;
                            result = graph1.minCut(adjmatrix, s, t);
                            while (result[i] != null) {
                                String sub = result[i];
                                printout = printout.concat(result[i]+"\n");
                                String[] edgenodes = sub.split("-");
                                int node1 = Integer.parseInt(edgenodes[0]);
                                int node2 = Integer.parseInt(edgenodes[1]);
                                int dist1 = nodedistance.get(node1); //get node 1 distance
                                int dist2 = nodedistance.get(node2); //get node 2 distance
                                if (dist1 < dist2) {
                                    nodedistance.remove(node2);
                                } else if (dist1 > dist2) {
                                    nodedistance.remove(node1);
                                }
                                i++;
                            }
                            Set finalset = nodedistance.keySet();
                            Iterator nodedistanceiterator = finalset.iterator();
                            while (nodedistanceiterator.hasNext()) {
                                Object obj = nodedistanceiterator.next();
                                Integer finalcutint = Integer.valueOf(obj.toString());
                                String finalcutnode = idtonum2.get(finalcutint);
                                printout = printout.concat(" final cut node: " + finalcutnode+"\n");
                            }
                        printout = printout.concat(
                                "######################### ACL/FLOW RULES #######################################"+"\n");
                        try {
                            final DefaultFlowRule.Builder flowrulebuilder = DefaultFlowRule.builder();
                            printout = printout.concat("The total number of flow rules is : "+"\n");
                            rulesbefore = flowRuleService.getFlowRuleCount();
                            printout = printout.concat(rulesbefore +"\n");
                            Set finale = nodedistance.keySet();
                            Iterator finaleiterator = finale.iterator();
                            while(finaleiterator.hasNext()){
                                // create # of rules equal to the # in r
                                Object finalobj = finaleiterator.next();
                                Integer finaleint = Integer.valueOf(finalobj.toString());
                                int octetmax = 255;
                                int hostmax = 254;
                                int fourthoctet = 4;
                                int thirdoctect = 0;
                                int maxrules = r;
                                int addedrules = 0;
                                int thirdoctectloop = ((r+4)/octetmax);
                                //iterate over the number of rules
                                for( int j = 0; j < thirdoctectloop + 1 ; j++) {
                                    if (addedrules == maxrules){
                                        printout = printout.concat("added rules: " + addedrules+"\n");
                                        break;
                                        //dont do anymore if == total rules
                                    }
                                    if(j != 0){
                                        thirdoctect = thirdoctect + 1;
                                    }

                                    while(fourthoctet%octetmax < hostmax) {
                                        if (addedrules == maxrules){
                                            printout = printout.concat("added rules: " + addedrules+"\n");
                                            rulesadded = addedrules;
                                            break;
                                            //dont do anymore if == total rules
                                        }
                                        fourthoctet = (fourthoctet + 1) % (octetmax);
                                        TrafficSelector.Builder selectorbuilder = DefaultTrafficSelector.builder();
                                        TrafficTreatment.Builder treatmentbuilder = DefaultTrafficTreatment.builder();
                                        ApplicationId applicationId = new DefaultApplicationId(158, "org.onosproject.graph");
                                        //DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
                                        short type = 0x800;
                                        int priority = 40000;
                                        int timeout = 10000;
                                        String thirdoctetstring = String.valueOf(thirdoctect);
                                        String fourthoctectstring = String.valueOf(fourthoctet);
                                        Ip4Prefix ip4Prefixdst1 = Ip4Prefix.valueOf("10.0." + thirdoctetstring + "." + fourthoctectstring + "/32");
                                        printout=printout.concat("10.0." + thirdoctetstring + "." + fourthoctectstring + "/32"+"\n");
                                        String tempname = idtonum2.get(finaleint);
                                        printout = printout.concat(" rule place onto " + tempname+"\n");
                                        DeviceId deviceId = DeviceId.deviceId(tempname);
                                        TrafficSelector selector = selectorbuilder.
                                                matchIPDst(ip4Prefixdst1).
                                                matchEthType(type).
                                                build();
                                        TrafficTreatment treatment = treatmentbuilder.
                                                drop().
                                                build();
                                        FlowRule rule1 = flowrulebuilder.
                                                withSelector(selector).
                                                withTreatment(treatment).
                                                makePermanent().
                                                forDevice(deviceId).
                                                fromApp(applicationId).
                                                withPriority(priority).
                                                withHardTimeout(timeout).
                                                build();
                                        flowRuleService.applyFlowRules(rule1);
                                        log.info(" Graph Application Rule Built for Device " + tempname);
                                        addedrules = addedrules + 1;
                                    }
                                    //loops = loops +1;
                                    fourthoctet = 0;
                                }
                            }
                            rulesafter = flowRuleService.getFlowRuleCount();
                            printout =printout.concat(
                                    "Original Rulecount =  " + rulesbefore + "\n"+ " Rulecount After = " + rulesafter+"\n");
                            printout=printout.concat(
                                    "#############################Printing Results###############################"+"\n");
                            try{
                                String rulescreated = String.valueOf(rulesadded);
                                String rulesrequested = String.valueOf(r);
                                String rules = "";
                                ApplicationId applicationId = new DefaultApplicationId(158, "org.onosproject.graph");
                                Iterable iterable = flowRuleService.getFlowEntriesById(applicationId);
                                Integer counted = 1;
                                for(Object s: iterable){
                                    rules = rules.concat(" Rule " + counted + " : " + s.toString() + "\n");
                                    counted = counted + 1;
                                }
                                String fileName = new SimpleDateFormat("yyyyMMddHHmmssSS'.txt'").format(new Date());
                                FileWriter fileWriter = new FileWriter("/home/brent/onostopologychange/LINK_CHANGE_DETECTEDRESULTS"+fileName+".txt");
                                fileWriter.write(fileName + "Rules created : " + rulescreated + "\n"+
                                        "Rules requested : " + rulesrequested + "\n"+ "Rules added: " + rules +"\n" + printout );
                                fileWriter.close();
                            }
                            catch (IOException e) {
                                String notification = e.toString();
                                FileWriter fileWriter = new FileWriter(
                                        "/home/brent/onostopologychange/topologychangedexception1.txt");
                                fileWriter.write(notification);
                                fileWriter.close();
                            }
                        }
                        catch (Exception e)
                        {
                            String notification = e.toString();
                            FileWriter fileWriter = new FileWriter(
                                    "/home/brent/onostopologychange/topologychangedexception2.txt");
                            fileWriter.write(notification);
                            fileWriter.close();
                        }

                    } catch (Exception e) {
                        try {
                            String notification = e.toString();
                            FileWriter fileWriter = new FileWriter(
                                    "/home/brent/onostopologychange/topologychangedexception3.txt");
                            fileWriter.write(notification);
                            fileWriter.close();
                        } catch (IOException i) {
                        }
                    }
                }
            }
        };
        /*
        TopologyListener topologyListener = new TopologyListener() {
            @Override
            public void event(TopologyEvent event) {
                if(event.type() == TopologyEvent.Type.TOPOLOGY_CHANGED){

                    Integer presult=null , counter = 0 , limit = 20;
                    String[] output = new String[20];
                    try{
                        String[] mycommand = new String[]{"sh onos kill "+s + " "+ t + " " + r};
                        Process p = Runtime.getRuntime().exec(mycommand);
                        presult = p.waitFor();

                        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String readline;
                        while ((readline = reader.readLine()) != null & counter < limit ){
                            output[counter] = (readline);
                            counter = counter + 1;
                        }
                        p.destroy();
                        reader.close();

                    } catch (Exception e){
                        log.info("Exception " + e.toString());
                        try{
                            String notification = e.toString();
                            FileWriter fileWriter = new FileWriter(
                                    "/home/brent/onostopologychange/topologychangedexception.txt");
                            fileWriter.write(notification);
                            fileWriter.close();
                        }
                        catch (IOException i){

                        }
                    }

                    try {
                        String date = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
                        String notification = "1" + event.toString()
                                + " event time" + event.time()
                                + " CPU time : " + date
                                + " process exit code: " + presult
                                + " output " + Arrays.toString(output);
                        String fileName = "topologyinfo";
                        FileWriter fileWriter = new FileWriter(
                                "/home/brent/onostopologychange/"+fileName +".txt");
                        fileWriter.write(notification);
                        fileWriter.close();
                    }
                    catch (IOException e){
                      log.info("caught exception " + e.toString());
                    }
                }
            }
        };
        */
        //topologyService.addListener(topologyListener);
        LinkService linkService = get(LinkService.class);
        linkService.addListener(linkListener);
        int x, y=0, rulesbefore=0, rulesafter=0, rulesadded=0;
        DeviceService deviceService = get(DeviceService.class);
        TopologyService topologyService = get(TopologyService.class);
        Topology topo = topologyService.currentTopology();
        //listenerRegistry.addListener(tl);
        TopologyGraph graph = topologyService.getGraph(topo);
        Set<TopologyEdge> edges = graph.getEdges(); //grab the edges from current topology
        Set<TopologyVertex> vertexes = graph.getVertexes(); // grab the vertexes from the current topologyon
        HashMap<DeviceId, Integer> idtonum = new HashMap<>();// deviceid to int
        HashMap<Integer,String> idtonum2 = new HashMap<>();//for rules
        HashMap<DeviceId, List> idtostats = new HashMap<>(); //device id to edges
        HashMap<DeviceId, List> idtoports = new HashMap<>(); // devices and their associated ports
        DefaultEdgeWeigher edgeWeigher = new DefaultEdgeWeigher();
        FlowRuleService flowRuleService = get(FlowRuleService.class);
        int devicenum = vertexes.size();
        int[][] adjmatrix = new int[devicenum][devicenum];
        DeviceId[] idlist = new DeviceId[devicenum];
        Graph graph1 = new Graph(devicenum);
        String[] result;
        String printout= "";

        for (TopologyVertex temp1: vertexes){
            String name = String.valueOf(temp1);
            DeviceId id = temp1.deviceId();
            idtonum.put(id,y);
            idtonum2.put(y,name);
            idlist[y] = id;
            List ports = deviceService.getPorts(id);
            List stats = deviceService.getPortStatistics(id);
            idtostats.put(id,stats);
            idtoports.put(id,ports);
            y++;
        }
        print("########################## device id to matrix number ##########################");
        printout = printout.concat(
                "########################## device id to matrix number ##########################"+"\n");
        //get a set of entries
        Set set = idtonum.entrySet();
        //get an iterator
        Iterator iterator = set.iterator();
        while(iterator.hasNext()){
            Map.Entry entry = (Map.Entry)iterator.next();
            print("Device id: " + entry.getKey() + " ; matrix number : " + entry.getValue());
            printout = printout.concat("Device id: " + entry.getKey() + " ; matrix number : " + entry.getValue()+"\n");
        }
        print("###########################   WEIGHTS  #########################################");
        printout = printout.concat(
                "###########################   WEIGHTS  #########################################"+"\n");
        for (TopologyEdge edgetemp: edges) {
            for (int j = 0; j < devicenum; j++) {
                //DefaultTopologyEdge{src=of:0000000000000001, dst=of:0000000000000002}
                String edge = String.valueOf(edgetemp);
                String src = null;
                String dst = null;
                String pattern ="((of:)(\\d)*)";
                Pattern p = Pattern.compile(pattern);
                Matcher matcher = p.matcher(edge);
                if(matcher.find()) {
                    src = matcher.group(0);
                }
                if( matcher.find(44)){
                    dst = matcher.group(0);
                }
                DeviceId source = DeviceId.deviceId(src);
                DeviceId destination = DeviceId.deviceId(dst);
                DeviceId id = idlist[j];
                if (id.equals(source)){
                    int row = idtonum.get(id);
                    int column = idtonum.get(destination);
                    Weight weight = edgeWeigher.weight(edgetemp);
                    String stringweight = String.valueOf(weight);
                    int lastindex = stringweight.lastIndexOf('}');
                    String sw = stringweight.substring(19,lastindex);
                    float floatweight = Float.valueOf(sw);
                    int intweight = Math.round(floatweight);
                    adjmatrix[row][column] = intweight;
                    adjmatrix[column][row] = intweight;
                }
            }
        }
        print("##########################    MATRIX    ########################################");
        printout = printout.concat(
                "##########################    MATRIX    ########################################"+"\n");
        for (int i=0; i<devicenum;i++){
            print ("Row " + i + "-------------------------------------------------------- Row " + i);
            printout = printout.concat(
                    "Row " + i + "-------------------------------------------------------- Row " + i+"\n");
            for(int j = 0; j <devicenum; j++){
                print(String.valueOf(adjmatrix[i][j]));
                printout = printout.concat(adjmatrix[i][j] +"\n");
            }
        }
        print("######################### SHORTEST PATHS #######################################");
        printout = printout.concat(
                "######################### SHORTEST PATHS #######################################"+"\n");
        ShortestPath shorty = new ShortestPath(devicenum);
        int origin = s;
        int[] shortydijkstra = shorty.dijkstra(adjmatrix, origin);
        print("Vertex   Distance from Source");
        printout = printout.concat("Vertex   Distance from Source"+"\n");
        for (int i = 0; i < devicenum; i++) {
            print(i + "                 " + shortydijkstra[i]);
            printout = printout.concat(i + "                 " + shortydijkstra[i]+"\n");
        }
        print("##########################      CUT     ########################################");
        printout=printout.concat(
                "##########################      CUT     ########################################"+"\n");
        Vector devicelist = new Vector();
        HashMap<Integer,Integer> nodedistance = new HashMap(); //Key:node Value: Distance from source
        if ((s != -1) && (t != -1)) {
            result = graph1.minCut(adjmatrix, s, t);
            x = result.length;
            y = 0;

            for (int i = 0; i < x; i++) {
                if (result[i] != null) {
                    String sub = result[i];
                    String[] subtwo = sub.split("-");
                    int device0 = Integer.parseInt(subtwo[0]);
                    devicelist.add(y, (device0));
                    int distance0 = shortydijkstra[device0];
                    nodedistance.put(device0,distance0);
                    y++;
                    int device1 = Integer.parseInt(subtwo[1]);
                    devicelist.add(y, (device1));
                    int distance1 = shortydijkstra[device1];
                    nodedistance.put(device1,distance1);
                    y++;
                    print(String.valueOf(result[i]));
                    printout = printout.concat(result[i] +"\n");

                }
            }
        }
        int devicelistsize = devicelist.size();
        for (int i = 0; i < devicelistsize; i++) {
            Object tempobject = devicelist.get(i);
            String tempstring = tempobject.toString();
            int tempint = Integer.valueOf(tempstring);
            //intarray[i] = tempint;
            print(" Device ID: " + idtonum2.get(tempint) + "  Matrix id: " + tempint);
            printout=printout.concat(" Device ID: " + idtonum2.get(tempint) + "  Matrix id: " + tempint+"\n");
        }
        print("######################### MIN MIN CUT    #######################################");
        printout=printout.concat("######################### MIN MIN CUT    #######################################"+"\n");
        try {
            result = graph1.minCut(adjmatrix, s, t);
            int i = 0;
            while (result[i] != null) {
                String sub = result[i];
                print(result[i]);
                printout=printout.concat(result[i]+"\n");
                String[] edgenodes = sub.split("-");
                int node1 = Integer.parseInt(edgenodes[0]);
                int node2 = Integer.parseInt(edgenodes[1]);
                int dist1 = nodedistance.get(node1); //get node 1 distance
                int dist2 = nodedistance.get(node2); //get node 2 distance
                if (dist1 < dist2) {
                    nodedistance.remove(node2);
                } else if (dist1 > dist2) {
                    nodedistance.remove(node1);
                }
                i++;
            }
            Set finalset = nodedistance.keySet();
            Iterator nodedistanceiterator = finalset.iterator();
            while (nodedistanceiterator.hasNext()) {
                Object obj = nodedistanceiterator.next();
                Integer finalcutint = Integer.valueOf(obj.toString());
                String finalcutnode = idtonum2.get(finalcutint);
                print(" final cut node: " + finalcutnode);
                printout=printout.concat(" final cut node: " + finalcutnode+"\n");
            }
        } catch (Exception e){
            print(e.toString());
        }
        print("######################### ACL/FLOW RULES #######################################");
        printout=printout.concat(
                "######################### ACL/FLOW RULES #######################################"+"\n");
        // get rid of duplicates in the list
        try {
            final DefaultFlowRule.Builder flowrulebuilder = DefaultFlowRule.builder();

            print("The total number of flow rules is : ");
            printout = printout.concat("The total number of flow rules is : ");
            rulesbefore = flowRuleService.getFlowRuleCount();
            print(String.valueOf(rulesbefore));
            printout = printout.concat(rulesbefore +"\n");

            Set finale = nodedistance.keySet();
            Iterator finaleiterator = finale.iterator();
            while(finaleiterator.hasNext()){
                // create # of rules equal to the # in r
                Object finalobj = finaleiterator.next();
                Integer finaleint = Integer.valueOf(finalobj.toString());
                int octetmax = 255;
                int hostmax = 254;
                int fourthoctet = 4;
                int thirdoctect = 0;
                int maxrules = r;
                int addedrules = 0;
                //int loops = 0;
                int thirdoctectloop = ((r+4)/octetmax);
                //iterate over the number of rules
                for( int j = 0; j < thirdoctectloop + 1 ; j++) {
                    if (addedrules == maxrules){
                        print("added rules: " + addedrules);
                        printout=printout.concat("added rules: " + addedrules+"\n");
                        break;
                        //dont do anymore if == total rules
                    }
                    if(j != 0){
                        thirdoctect = thirdoctect + 1;
                    }

                    while(fourthoctet%octetmax < hostmax) {
                        if (addedrules == maxrules){
                            print("added rules: " + addedrules);
                            printout = printout.concat("added rules: " + addedrules);
                            rulesadded = addedrules;
                            break;
                            //dont do anymore if == total rules
                        }
                        fourthoctet = (fourthoctet + 1) % (octetmax);

                        TrafficSelector.Builder selectorbuilder = DefaultTrafficSelector.builder();
                        TrafficTreatment.Builder treatmentbuilder = DefaultTrafficTreatment.builder();
                        //build rule one
                        ApplicationId applicationId = new DefaultApplicationId(158, "org.onosproject.graph");
                        //DeviceId deviceId = DeviceId.deviceId("of:0000000000000001");
                        short type = 0x800;
                        int priority = 40000;
                        int timeout = 10000;

                        String thirdoctetstring = String.valueOf(thirdoctect);
                        String fourthoctectstring = String.valueOf(fourthoctet);

                        Ip4Prefix ip4Prefixdst1 = Ip4Prefix.valueOf("10.0." + thirdoctetstring + "." + fourthoctectstring + "/32");
                        print("10.0." + thirdoctetstring + "." + fourthoctectstring + "/32");
                        printout=printout.concat("10.0." + thirdoctetstring + "." + fourthoctectstring + "/32"+"\n");
                        //Integer arraynum = nodedistance.get(i);
                        String tempname = idtonum2.get(finaleint);
                        print(" rule place onto " + tempname);
                        printout = printout.concat(" rule place onto " + tempname+"\n");
                        DeviceId deviceId = DeviceId.deviceId(tempname);
                        TrafficSelector selector = selectorbuilder.
                                matchIPDst(ip4Prefixdst1).
                                matchEthType(type).
                                build();
                        TrafficTreatment treatment = treatmentbuilder.
                                drop().
                                build();
                        FlowRule rule1 = flowrulebuilder.
                                withSelector(selector).
                                withTreatment(treatment).
                                makePermanent().
                                forDevice(deviceId).
                                fromApp(applicationId).
                                withPriority(priority).
                                withHardTimeout(timeout).
                                build();

                        flowRuleService.applyFlowRules(rule1);
                        addedrules = addedrules + 1;
                    }
                    //loops = loops +1;
                    fourthoctet = 0;
                }
            }
            rulesafter = flowRuleService.getFlowRuleCount();
            print("Original Rulecount =  " + rulesbefore + "   Rulecount After = " + rulesafter);
            printout=printout.concat(
                    "Original Rulecount =  " + rulesbefore + "   Rulecount After = " + rulesafter+"\n");
            print("#############################Printing Results###############################");
            try{
                String rulescreated = String.valueOf(rulesadded);
                String rulesrequested = String.valueOf(r);
                String rules = "";
                ApplicationId applicationId = new DefaultApplicationId(158, "org.onosproject.graph");
                Iterable iterable = flowRuleService.getFlowEntriesById(applicationId);
                Integer counter = 1;
                for(Object s: iterable){
                    rules = rules.concat("Rule " + counter + " :" + s.toString() + "\n");
                    counter = counter + 1;
                }
                String fileName = new SimpleDateFormat("yyyyMMddHHmmssSS'.txt'").format(new Date());
                FileWriter fileWriter = new FileWriter("/home/brent/captures/OUTCOME"+fileName+".txt");
                fileWriter.write("rules created : " + rulescreated + "\n"+
                        "  Rules requested : " + rulesrequested + "\n" + "  Rules added: " + rules + "\n" + printout  );
                fileWriter.close();
            }
            catch (IOException e) {
                print(e.toString());
            }
        }
        catch (Exception e)
        {
            print(e.toString());
        }
    }
}




