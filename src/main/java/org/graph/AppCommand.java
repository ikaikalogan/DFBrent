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

import java.util.*;
import org.apache.karaf.shell.commands.Command;
import org.onlab.graph.DefaultEdgeWeigher;
import org.onlab.graph.Weight;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.topology.*;

/**
 * Sample Apache Karaf CLI command
 */
@Command(scope = "onos", name = "kill",
         description = "Sample Apache Karaf CLI command")
public class AppCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        //instantiate variables for s-t cut

        int x, y=0, z=0;
        String s;
        Graph graph1 = new Graph();
        String[] result;

        //activate ONOS services for getting device and topology information
        DeviceService deviceService = get(DeviceService.class);
        TopologyService topologyService = get(TopologyService.class);
        // grab the current topology and graph
        Topology topo = topologyService.currentTopology();
        TopologyGraph graph = topologyService.getGraph(topo);
        // grab the edges and nodes (vertexes) for building an adjacency matrix and building hashmaps
        Set<TopologyEdge> edges = graph.getEdges(); //grab the edges from current topology
        Set<TopologyVertex> vertexes = graph.getVertexes(); // grab the vertexes from the current topologyon
        //Hashmaps and Iterables
        HashMap<String, Integer> idtonum = new HashMap<>();// deviceid to int
        HashMap<DeviceId, List> idtostats = new HashMap<>(); //device id to edges
        HashMap<DeviceId, List> idtoports = new HashMap<>(); // devices and their associated ports

        DefaultEdgeWeigher edgeWeigher = new DefaultEdgeWeigher();

        int devicenum = vertexes.size();
        int[][] adjmatrix = new int[devicenum][devicenum];
        String[] idlist = new String[devicenum];

        //populate hashmap idtonum & idtoports
        for (TopologyVertex temp1: vertexes){
            //print(String.valueOf(temp1));
            String name = String.valueOf(temp1);
            DeviceId id = temp1.deviceId();
            idtonum.put(name,y);
            idlist[y] = name;
            List ports = deviceService.getPorts(id);
            List stats = deviceService.getPortStatistics(id);
            idtostats.put(id,stats);
            idtoports.put(id,ports);
            y++;
        }
        print("#####################    KEYS    ############################");
        //idtoports.forEach((k,v) -> print("Key = " + String.valueOf(k) + ", Value = " + String.valueOf(v)));
        for (DeviceId deviceId : idtoports.keySet()){
            print("Key " + String.valueOf(deviceId));
        }
        print("#####################   VALUES   ###########################");
        for (List list : idtoports.values()) {
            print("value " + String.valueOf(list));
        }
        print("#####################   WEIGHTS  ############################");

        //populate hashmap idtoedges - for each edge in the set TopologyEdge
        for (TopologyEdge edgetemp: edges) {
            // compare the edge to each device in idlist
            // compare the edge to each device in idlist
            //print(String.valueOf(devicenum));
            for (int j = 0; j < devicenum; j++) {
                String edge = String.valueOf(edgetemp);
                //print("edge " + edge);
                String src  = edge.substring(24,43);
                //print("src " + src);
                String dst  = edge.substring(49,68);
                //print("dst "+ dst);
                String id = idlist[j];
                //print("id " + id);
                if (id.equals(src)){
                    //print("there");
                    int row = idtonum.get(id);
                    int column = idtonum.get(dst);
                    Weight weight = edgeWeigher.weight(edgetemp);
                    String stringweight = String.valueOf(weight);
                    int lastindex = stringweight.lastIndexOf('}');
                    String sw = stringweight.substring(19,lastindex);
                    print(" The weight of " + edge + " is: " + sw);
                    float floatweight = Float.valueOf(sw);
                    int intweight = Math.round(floatweight);
                    adjmatrix[row][column] = intweight;
                    //ScalarWeight{value=1.0}
                }
            }
        }
        print("#####################    MATRIX    ##########################");
        for (int i=0; i<devicenum;i++){
            for(int j = 0; j <devicenum; j++){
                print(String.valueOf(adjmatrix[i][j]));
            }
        }
        print("#####################      CUT     ##########################");
        // sample test adjacancy matrix to test s-t cut code in graph.java

        int test[][] = {{0, 16, 13, 0, 0, 0},
                {0, 0, 10, 12, 0, 0},
                {0, 4, 0, 0, 14, 0},
                {0, 0, 9, 0, 0, 20},
                {0, 0, 0, 7, 0, 4},
                {0, 0, 0, 0, 0, 0}
        };
        //run s-t min cut on nodes 0 and 5 from the test matrix above

        result = graph1.minCut(adjmatrix,0, 2);
        x = result.length;

        //s = Integer.toString(x);
        // print the edge cuts that make the s-t cut

        for (int k = 0; k < x; k++) {
            if (result[k] != null){
            print(result[k]);
            }
        }
    }
}




