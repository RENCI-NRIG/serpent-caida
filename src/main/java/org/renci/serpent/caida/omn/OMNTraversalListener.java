package org.renci.serpent.caida.omn;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.ontology.Individual;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.renci.serpent.caida.SimpleCAIDA;
import org.renci.serpent.caida.SimpleCAIDA.Edge;

/**
 * Attempted to use this for BFI traversal, but does not work as expected /ib 07/14/2017
 * @author ibaldin
 *
 */
public class OMNTraversalListener extends TraversalListenerAdapter<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> {
	protected OMNFlavor omnType;
	protected OMNGen omg;
	protected Set<SimpleCAIDA.Vertex> visited;
	protected Set<SimpleCAIDA.Vertex> onTree = new HashSet<>();
	
	// determines the types of edges generated in OMN in traversal
	public enum OMNFlavor { DIRECTED, TRADITIONAL, BIDIRECTIONAL};
	
	/**
	 * Take OMN generator and desired flavor of edges we want to produce:
	 * DIRECTED - using forward and inverse property to produce strictly directed edges
	 * TRADITIONAL - produce traditional uni-directional OMN edges with ports pointing to the edge
	 * BIDIRECTIONAL - produce bi-directional OMN edges (including declaring bi-direcitonal ports)
	 * @param g
	 * @param t
	 */
	public OMNTraversalListener(OMNGen g, OMNFlavor t) {
		omg = g;
		omnType = t;
		visited = null;
	}
	
	public OMNTraversalListener(OMNGen g, OMNFlavor t, Set<SimpleCAIDA.Vertex> v) {
		omg = g;
		omnType = t;
		visited = v;
	}
	
	public void edgeTraversed(EdgeTraversalEvent<Edge> e) {
		// check that both vertices of the edge are part of visited,
		// else ignore it
		if ((visited != null) && ((!visited.contains(e.getEdge().getFrom())) || (!visited.contains(e.getEdge().getTo()))))
			return;

		if (onTree.contains(e.getEdge().getFrom())) {
			//System.out.println("--- Edge from " + e.getEdge().getTo().getLabel() + " to " + e.getEdge().getFrom().getLabel());
			return;
		}
		onTree.add(e.getEdge().getFrom());
		//System.out.println("+++ Edge from " + e.getEdge().getTo().getLabel() + " to " + e.getEdge().getFrom().getLabel());
		
		Individual to = omg.declareCAIDANode(e.getEdge().getFrom().getLabel());
		Individual from = omg.declareCAIDANode(e.getEdge().getTo().getLabel());

		switch(omnType) {
		case DIRECTED:
			directedEdgeTraversal(from, to, e);
			break;
		case TRADITIONAL: 
			traditionalEdgeTraversal(from, to, e);
			break;
		case BIDIRECTIONAL:
			bidirectionalEdgeTraversal(from, to, e);
		}
	}
	
	protected void directedEdgeTraversal(Individual from, Individual to, EdgeTraversalEvent<Edge> e) {
		omg.declareDirectedCAIDAEdge(from, to, e.getEdge().getEdgeType());		
	}
	
	protected void traditionalEdgeTraversal(Individual from, Individual to, EdgeTraversalEvent<Edge> e) {
		omg.declareDirectedCAIDAEdgeNoInverse(from, to, e.getEdge().getEdgeType());
	}
	
	protected void bidirectionalEdgeTraversal(Individual from, Individual to, EdgeTraversalEvent<Edge> e) {
		omg.declareBidirectionalCAIDAEdge(from, to, e.getEdge().getEdgeType());
	}
}
