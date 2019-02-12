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
import org.onosproject.net.topology.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.slf4j.LoggerFactory.getLogger;
/**
 * Apache Karaf CLI command to determine the edge cut b/w 2 nodes and deploy ACLs
 */

@Command(scope = "onos", name = "kill",
         description = "Deploy Access Control Lists to Network Edge Cut Set")

public class AppCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "s", description = "source", required = true, multiValued = false)
    private int s = -1;
    @Argument(index = 1, name = "t"  , description = "destination", required = true, multiValued = false)
    private int t = -1;
    @Argument(index = 2, name = "r" , description = "# of rules", required = true, multiValued = false)
    private int r = 1;
    private final Logger logger = getLogger(getClass());
    @Override
    public void execute() {
        //instantiate variables for s-t cut
        log.info("Graph Application: Started with min cut between " + s + " and " + t);
        int x, y=0, rulesbefore=0, rulesafter=0;


        //activate ONOS services for getting device and topology information
        DeviceService deviceService = get(DeviceService.class);
        TopologyService topologyService = get(TopologyService.class);

        // grab the current topology and graph
        Topology topo = topologyService.currentTopology();
        // add a listener for topology events


        TopologyGraph graph = topologyService.getGraph(topo);
        // grab the edges and nodes (vertexes) for building an adjacency matrix and building hashmaps
        Set<TopologyEdge> edges = graph.getEdges(); //grab the edges from current topology
        Set<TopologyVertex> vertexes = graph.getVertexes(); // grab the vertexes from the current topologyon
        //Hashmaps and Iterables
        HashMap<DeviceId, Integer> idtonum = new HashMap<>();// deviceid to int
        HashMap<Integer,String> idtonum2 = new HashMap<>();//for rules
        HashMap<DeviceId, List> idtostats = new HashMap<>(); //device id to edges
        HashMap<DeviceId, List> idtoports = new HashMap<>(); // devices and their associated ports

        //Weigher for link topology
        DefaultEdgeWeigher edgeWeigher = new DefaultEdgeWeigher();
        //Flow Rule Instantiations
        FlowRuleService flowRuleService = get(FlowRuleService.class);

        int devicenum = vertexes.size();
        int[][] adjmatrix = new int[devicenum][devicenum];
        DeviceId[] idlist = new DeviceId[devicenum];
        //Vector edgelist = new Vector();

        Graph graph1 = new Graph(devicenum);
        String[] result;

        //populate hashmap idtonum & idtoports
        for (TopologyVertex temp1: vertexes){
            //print(String.valueOf(temp1));
            String name = String.valueOf(temp1);
            DeviceId id = temp1.deviceId();
            //change from name
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
        //get a set of entries
        Set set = idtonum.entrySet();
        //get an iterator
        Iterator iterator = set.iterator();
        while(iterator.hasNext()){
            Map.Entry entry = (Map.Entry)iterator.next();
            print("Device id: " + entry.getKey() + " ; matrix number : " + entry.getValue());
        }

        //print("###########################  idtoports KEYS    ###########################################");
        //idtoports.forEach((k,v) -> print("Key = " + String.valueOf(k) + ", Value = " + String.valueOf(v)));
        //for (DeviceId deviceId : idtoports.keySet()){
        //    print("Key " + String.valueOf(deviceId));
        //    print("------------------------------------------------------------------------------");
        //}
        //print("###########################  ID TO PORT VALUES   ###########################################");
        //for (List list : idtoports.values()) {
        //    print("value " + String.valueOf(list));
        //    print("------------------------------------------------------------------------------");
        //}
        //print("########################### PORT STATS #########################################");
        //for (List stats : idtostats.values() ){
        //    print("stats :" + stats.toString());
        //}
        print("###########################   WEIGHTS  #########################################");

        for (TopologyEdge edgetemp: edges) {

            for (int j = 0; j < devicenum; j++) {
                //string to be scanned to find the pattern
                //DefaultTopologyEdge{src=of:0000000000000001, dst=of:0000000000000002}
                String edge = String.valueOf(edgetemp);
                String src = null;
                String dst = null;
                //print(edge);
                String pattern ="((of:)(\\d)*)";
                //create a pattern oboject
                Pattern p = Pattern.compile(pattern);
                //create matcher object
                Matcher matcher = p.matcher(edge);
                //replace magic numbers with regex finds
                if(matcher.find()) {
                    //print(matcher.group(0));
                    src = matcher.group(0);
                }
                if( matcher.find(44)){
                    //print( matcher.group(0));
                    dst = matcher.group(0);
                }
                //String src  = edge.substring(24,43);
                DeviceId source = DeviceId.deviceId(src);
                //String dst  = edge.substring(49,68);
                DeviceId destination = DeviceId.deviceId(dst);
                //String id = idlist[j];
                DeviceId id = idlist[j];
                if (id.equals(source)){
                    int row = idtonum.get(id);
                    int column = idtonum.get(destination);
                    Weight weight = edgeWeigher.weight(edgetemp);
                    String stringweight = String.valueOf(weight);
                    int lastindex = stringweight.lastIndexOf('}');
                    String sw = stringweight.substring(19,lastindex);
                    //print(" The weight of " + edge + " is: " + sw);
                    //print("-------------------------------------");
                    float floatweight = Float.valueOf(sw);
                    int intweight = Math.round(floatweight);
                    adjmatrix[row][column] = intweight;
                    adjmatrix[column][row] = intweight;
                }
            }
        }
        /*print("##########################    MATRIX    ########################################");

        for (int i=0; i<devicenum;i++){
            print ("Row " + i + "-------------------------------------------------------- Row " + i);
            for(int j = 0; j <devicenum; j++){
                print(String.valueOf(adjmatrix[i][j]));
            }
        }
        */
        print("######################### SHORTEST PATHS #######################################");
        ShortestPath shorty = new ShortestPath(devicenum);
        int origin = s;

        int[] shortydijkstra = shorty.dijkstra(adjmatrix, origin);
        print("Vertex   Distance from Source");
        for (int i = 0; i < devicenum; i++) {
            print(i + "                 " + shortydijkstra[i]);
        }
        print("##########################      CUT     ########################################");

        int[][] test = {{0, 16, 13, 0, 0, 0},
                {0, 0, 10, 12, 0, 0},
                {0, 4, 0, 0, 14, 0},
                {0, 0, 9, 0, 0, 20},
                {0, 0, 0, 7, 0, 4},
                {0, 0, 0, 0, 0, 0}
        };

        //run s-t min cut on nodes 0 and 5 from the test matrix above
        // run on cli arguments if input, default to run them on prepicked nodes
        //DO SOME ERROR CHECKING HERE

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
                    //Integer edge1 = (Integer.parseInt(subtwo[0]));
                    //print(subtwo[0]);
                    //print("subtwo[0]: " + subtwo[0] + " devicelist: " + devicelist);
                    y++;
                    int device1 = Integer.parseInt(subtwo[1]);
                    devicelist.add(y, (device1));
                    int distance1 = shortydijkstra[device1];
                    nodedistance.put(device1,distance1);
                    //Integer edge2 = (Integer.parseInt(subtwo[1]));
                    //Pair temppair = new Pair(edge1, edge2);
                    //edgelist.addElement(temppair);
                    //print(subtwo[1]);
                    //print("subtwo[1]: " + subtwo[1] + " devicelist: " + devicelist);
                    y++;
                    print(String.valueOf(result[i]));

                }
            }
        } else {
            result = graph1.minCut(test, 0, 5);
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
                    //Integer edge1 = (Integer.parseInt(subtwo[0]));
                    //print(subtwo[0]);
                    //print("subtwo[0]: " + subtwo[0] + " devicelist: " + devicelist);
                    y++;
                    int device1 = Integer.parseInt(subtwo[1]);
                    devicelist.add(y, (device1));
                    int distance1 = shortydijkstra[device1];
                    nodedistance.put(device1,distance1);
                    //Integer edge2 = (Integer.parseInt(subtwo[1]));
                    //Pair temppair = new Pair(edge1, edge2);
                    //edgelist.addElement(temppair);
                    //print(subtwo[1]);
                    //print("subtwo[1]: " + subtwo[1] + " devicelist: " + devicelist);
                    y++;
                    print(String.valueOf(result[i]));
                }
            }
        }
        //int cutsize = devicelist.size();
        //int[] intarray = new int[cutsize];
        //print(devicelist.toString());
        int devicelistsize = devicelist.size();
        for (int i = 0; i < devicelistsize; i++) {
            Object tempobject = devicelist.get(i);
            String tempstring = tempobject.toString();
            int tempint = Integer.valueOf(tempstring);
            //intarray[i] = tempint;
            print(" Device ID: " + idtonum2.get(tempint) + "  Matrix id: " + tempint);
        }
        print("######################### MIN MIN CUT    #######################################");
        //List<Integer> finalcut = new ArrayList<Integer>(); // final cut is the list of nodes to place flow rules on
        //print(String.valueOf(result.length));
        try {
            //for (int i = 0; i < result.length; i++) {
            //sorting the max flow min cut set
            int i = 0;
            while (result[i] != null) {
                String sub = result[i];
                print(result[i]);
                String[] edgenodes = sub.split("-");
                int node1 = Integer.parseInt(edgenodes[0]);
                int node2 = Integer.parseInt(edgenodes[1]);
                int dist1 = nodedistance.get(node1); //get node 1 distance
                int dist2 = nodedistance.get(node2); //get node 2 distance
                //int distance1 = shortydijkstra[temp1];
                //int distance2 = shortydijkstra[temp2];
                //print("Distance 1: " + distance1 + "    Distance 2: " + distance2);
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
            }
        } catch (Exception e){
            print(e.toString());
        }
        print("######################### ACL/FLOW RULES #######################################");
        // get rid of duplicates in the list
        try {
            final DefaultFlowRule.Builder flowrulebuilder = DefaultFlowRule.builder();

            print("The total number of flow rules is : ");
            rulesbefore = flowRuleService.getFlowRuleCount();
            print(String.valueOf(rulesbefore));

            //print("Starting Flow Build");

            //for each node in the Min Min Cut
            //for (int i = 0; i < nodedistance.size(); i++) {
            Set finale = nodedistance.keySet();
            Iterator finaleiterator = finale.iterator();
            while(finaleiterator.hasNext()){
                // create # of rules equal to the # in r
                Object finalobj = finaleiterator.next();
                Integer finaleint = Integer.valueOf(finalobj.toString());
                int octetmax = 255;
                int hostmax = 254;
                int fourthoctet = 2;
                int thirdoctect = 0;
                int maxrules = r;
                int addedrules = 0;
                //int loops = 0;
                int thirdoctectloop = ((r+2)/octetmax);
                //iterate over the number of rules
                for( int j = 0; j < thirdoctectloop + 1 ; j++) {
                    if (addedrules == maxrules){
                        print("added rules: " + addedrules);
                        break;
                        //dont do anymore if == total rules
                    }
                    if(j != 0){
                        thirdoctect = thirdoctect + 1;
                    }

                    while(fourthoctet%octetmax < hostmax) {
                        if (addedrules == maxrules){
                            print("added rules: " + addedrules);
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
                        //Integer arraynum = nodedistance.get(i);
                        String tempname = idtonum2.get(finaleint);
                        print(" rule place onto " + tempname);
                        DeviceId deviceId = DeviceId.deviceId(tempname);

                        //print("Building Selector");
                        TrafficSelector selector = selectorbuilder.
                                matchIPDst(ip4Prefixdst1).
                                matchEthType(type).
                                build();
                        //print("Building Treatment");
                        TrafficTreatment treatment = treatmentbuilder.
                                drop().
                                build();
                        //print("Building Rule");
                        FlowRule rule1 = flowrulebuilder.
                                withSelector(selector).
                                withTreatment(treatment).
                                makePermanent().
                                forDevice(deviceId).
                                fromApp(applicationId).
                                withPriority(priority).
                                withHardTimeout(timeout).
                                build();

                        //print("adding rules........................................");
                        flowRuleService.applyFlowRules(rule1);
                        log.info(" Graph Application Rule Built for Device " + tempname);
                        addedrules = addedrules + 1;

                    }
                    //loops = loops +1;
                    fourthoctet = 0;
                }
            }
            rulesafter = flowRuleService.getFlowRuleCount();
            print("Original Rulecount =  " + rulesbefore + " |  Rulecount After = " + rulesafter);
        }
        catch (Exception e)
        {
            print(e.toString());
            log.info(e.toString());
        }
    }
}




