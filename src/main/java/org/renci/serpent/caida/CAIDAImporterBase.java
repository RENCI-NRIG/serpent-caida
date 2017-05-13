package org.renci.serpent.caida;

import org.jgrapht.ext.ImportException;

/**
 * Base attributes of an importer
 * @author ibaldin
 *
 */
public class CAIDAImporterBase {

	public static enum EdgeProperty {
		EdgeType("ET"), EdgeOrigin("EO");
		
		String label;
		
		EdgeProperty(String l) {
			label = l;
		}
		
		public String getLabel() {
			return label;
		}
	}

	public static enum EdgeType { 
		CP(-1), PP(0);
	
		int val;
		
		EdgeType(int v) {
			val = v;
		}
		
		public int getVal() {
			return val;
		}
		
		public String toString() {
			return "" + val;
		}
		
		public static EdgeType fromString(String s) throws ImportException {
			int v = Integer.parseInt(s);
			switch (v) {
			case -1: return CP;
			case 0: return PP;
			default: throw new ImportException("Unknown edge type " + s);
			}
		}
	}
}
