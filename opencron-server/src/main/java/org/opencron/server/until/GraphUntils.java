package org.opencron.server.until;
/**
 * @Package org.opencron.server.until
 * @Title: GraphUntils
 * @author hapic
 * @date 2018/4/11 17:24
 * @version V1.0
 */

import org.opencron.common.graph.Graph;
import org.opencron.common.graph.KahnTopo;
import org.opencron.common.graph.Node;
import org.opencron.server.domain.JobDependence;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @Descriptions: 图的循环依赖验证
 */
public class GraphUntils {

    /**
     * 验证行程的结构是否有循环依赖
     * @param jobDependenceList
     * @return
     */
    public static KahnTopo<Long> createGraph(List<JobDependence> jobDependenceList){
        return createGraph(null, null, jobDependenceList);
    }

    private static Node<Long> getNode(ConcurrentMap<Long, Node<Long>> concurrentMap, Long jobId) {
        Node<Long>  node;
        if(concurrentMap.containsKey(jobId)){
            node=concurrentMap.get(jobId);
        }else{
            node= new Node(jobId);
            concurrentMap.putIfAbsent(jobId,node);
        }
        return node;
    }

    public static KahnTopo<Long> createGraph(Long jobId, String[] dependenceid, List<JobDependence> jobDependenceList) {

        Graph graph = new Graph();
        ConcurrentMap<Long,Node<Long>> concurrentMap= new ConcurrentHashMap();

        for (JobDependence jobDependence:jobDependenceList){
            Node<Long>  start=getNode(concurrentMap,jobDependence.getDependenceJobId());
            Node<Long>  end=getNode(concurrentMap,jobDependence.getJobId());
            graph.addNode(start,end);
        }

     if(jobId!=null && dependenceid!=null){
            Node<Long> target=getNode(concurrentMap,jobId);//new Node(jobId);
            for(String depId:dependenceid){
                Node<Long>   source= getNode(concurrentMap,Long.parseLong(depId));//new Node(Long.parseLong(depId));
                graph.addNode(source,target);
            }
        }

        return new KahnTopo<Long>(graph);
    }
}
