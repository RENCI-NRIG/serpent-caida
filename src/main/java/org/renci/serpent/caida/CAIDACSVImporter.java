package org.renci.serpent.caida;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.jgrapht.Graph;
import org.jgrapht.ext.EdgeProvider;
import org.jgrapht.ext.GraphImporter;
import org.jgrapht.ext.ImportException;
import org.jgrapht.ext.VertexProvider;

/**
 * This is a JGraphT importer of a CAIDA as-rel files
 * File format is as follows:
 * # comment
 * AS1 | AS2 | [0,-1] | [bgp, mlp]+
 * @author ibaldin
 *
 * @param <V>
 * @param <E>
 */
public class CAIDACSVImporter<V, E> extends CAIDAImporterBase implements GraphImporter<V, E>  {
	VertexProvider<V> vp;
	EdgeProvider<V, E> ep;
	
	public CAIDACSVImporter(VertexProvider<V> vertexProvider, EdgeProvider<V,E> edgeProvider) {
		vp = vertexProvider;
		ep = edgeProvider;
	}
	
	@Override
	public void importGraph(Graph<V, E> g, Reader r) throws ImportException {
		BufferedReader br = null;
		
		if (r instanceof BufferedReader)
			br = (BufferedReader)r;
		else
			br = new BufferedReader(r);
		
		try {
			String l;
			while((l = br.readLine()) != null) {
				if (l.startsWith("#"))
					continue;
				String[] lar = l.split("\\|");
				//System.out.println(lar);
				if (lar.length != 4) {
					throw new ImportException("Unable to parse line: " + l + " " + lar.length);
				}
				
				// vertex provider is assumed to not provide duplicates
				V v1 = vp.buildVertex(lar[0], null);
				V v2 = vp.buildVertex(lar[1], null);
				Map<String, String> eprops = new HashMap<>();
				eprops.put(EdgeProperty.EdgeType.toString(), EdgeType.fromString(lar[2]).toString());
				eprops.put(EdgeProperty.EdgeOrigin.toString(), lar[3]);
				E e = ep.buildEdge(v1, v2, v1+" -> " + v2, eprops);
				g.addVertex(v1);
				g.addVertex(v2);
				g.addEdge(v1, v2, e);
			}
		} catch (IOException ioe) {
			throw new ImportException("Unable to import due to: " + ioe);
		}
	}

}
