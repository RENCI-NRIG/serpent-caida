package org.renci.serpent.caida;

import org.apache.jena.ontology.Individual;
import org.renci.serpent.caida.CAIDAImporterBase.EdgeType;
import org.renci.serpent.caida.omn.OMNGen;

public class OMNTest {

	public static void main(String[] argv) {

		try {
		OMNGen og = new OMNGen("/Users/ibaldin/Desktop/TDB");

		Individual topo = og.declareDefaultCAIDATopology("my-topology");
		Individual n1 = og.declareCAIDANode("1");
		Individual n2 = og.declareCAIDANode("2", topo);
		og.declareDirectedCAIDAEdge(n1, n2, EdgeType.PP);
		og.declareCAIDANode("1");
		og.declareCAIDANode("2");

		System.out.println(og.toXMLString());
		og.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
