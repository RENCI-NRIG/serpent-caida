package org.renci.serpent.caida;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListenerAdapter;
import org.jgrapht.ext.EdgeProvider;
import org.jgrapht.ext.ImportException;
import org.jgrapht.ext.VertexProvider;

/***
 * Simple interpretation of nodes and edges for CAIDA dataset
 * @author ibaldin
 *
 */
public class SimpleCAIDA {

	public static class Vertex {
		protected String label;
		
		public Vertex(String nm) {
			label = nm;
		}
		
		public String toString() {
			return "[ " + label + " ]";
		}
		
		public String getLabel() {
			return label;
		}
	}

	public static class Edge {
		protected String label;
		protected Vertex from, to;
		protected CAIDACSVImporter.EdgeType eType;
		
		public Edge(String l, Vertex f, Vertex t, Map<String, String> props) {
			label = l;
			from = f;
			to = t;
			try {
				eType = CAIDAImporterBase.EdgeType.fromString(props.get(CAIDAImporterBase.EdgeProperty.EdgeType.toString()));
			} catch(ImportException ie) {
				
			}
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(from + " ---" + label + "[" + eType +"] ---> " + to);
			return sb.toString();
		}
		
		public Vertex getFrom() {
			return from;
		}
		
		public Vertex getTo() {
			return to;
		}
		
		public String getLabel() {
			return label;
		}
		
		public CAIDAImporterBase.EdgeType getEdgeType() {
			return eType;
		}
	}

	// do not supply new vertex objects for the same name
	public static class SCVertexProvider implements VertexProvider<Vertex> {
		Map<String, Vertex> knownVertices = new HashMap<>();
		
		public Vertex buildVertex(String nm, Map<String, String> arg1) {
			Vertex ret = knownVertices.get(nm);
			if (ret == null) {
				ret = new Vertex(nm);
				knownVertices.put(nm, ret);
			}
			
			return ret;
		}
	}


	public static class SCEdgeProvider implements EdgeProvider<Vertex, Edge> {

		public SimpleCAIDA.Edge buildEdge(Vertex f, Vertex t, String l, Map<String, String> props) {
			return new Edge(l, f, t, props);
		}
	}
	
	public static class TrivialTraversalListener extends TraversalListenerAdapter<Vertex, Edge> {

		public void edgeTraversed(EdgeTraversalEvent<Edge> e) {
			System.out.println("Traversed edge " + e.getEdge());
		}
		
	}
	
	/**
	 * Find ASs with only CP-links
	 * @param g
	 * @return
	 */
	public static List<Vertex> findCPLeaves(Graph<Vertex, Edge> g) {
		List<Vertex> leafASs = new ArrayList<>();
		for(Vertex v: g.vertexSet()) {
			Set<Edge> edges = g.edgesOf(v);

			boolean allCP = true;
			for (Edge e: edges) {
				if (e.eType.equals(CAIDAImporterBase.EdgeType.PP)) {
					allCP = false;
					break;
				}
			}
			if (allCP) 
				leafASs.add(v);
		}
		return leafASs;
	}
	
}
