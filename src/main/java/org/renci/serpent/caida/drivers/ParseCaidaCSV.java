package org.renci.serpent.caida.drivers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.ontology.Individual;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.RDFFormat;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.renci.serpent.caida.CAIDACSVImporter;
import org.renci.serpent.caida.SimpleCAIDA;
import org.renci.serpent.caida.omn.OMNGen;

public class ParseCaidaCSV {
	static int maxVertices = 3;
	static String caidaSet = "/Users/ibaldin/Documents/Projects/SERPENT/CAIDA/20170201.as-rel2.txt";
	static String rdfFolder = "/Users/ibaldin/Desktop/SERPENT-WORK/CAIDA/";
	static final String execName = "parseCaidaCSV";
	
	static final RDFFormat[] allowedFormats = { RDFFormat.TTL, RDFFormat.NTRIPLES, RDFFormat.RDFXML };
	
	static RDFFormat format = RDFFormat.TTL;
	
	static String printFormats() {
		StringBuilder sb = new StringBuilder();
		for (RDFFormat f: allowedFormats) {
			sb.append(" ");
			sb.append(f.getLang().getName());
		}
		return sb.toString();
	}
	
	public static void savePropertyFile(StringBuilder propertyFile, String rdfFileName) {
		
		propertyFile.append("\nlabels.list=\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#hasOutboundPort,\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#isInboundPort,\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#hasCPSink,\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#isCPSource\n\n");
		
		propertyFile.append("\ntypes.list=\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#Port,\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#Node,\\\n");
		propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#CPLink\n\n");
		
		propertyFile.append("facts.file="+ rdfFileName + "\n\n");
		
		propertyFile.append("facts.file.syntax=" + format.getLang().getName() + "\n\n");
		
		propertyFile.append("constraint=only\n\n");
		propertyFile.append("type.constraint=only\n\n");
		
		propertyFile.insert(0, "# Property file for " + maxVertices + " ASs for datafile " + rdfFileName + "\n\n");
		
		String propertyFileName = rdfFolder + "/caida" + maxVertices + ".properties";
		
		System.out.println("Saving property file " + propertyFileName);
		
		BufferedWriter bw = null;
		try {
			File propertyF = new File(propertyFileName);
			bw = new BufferedWriter(new FileWriter(propertyF));
			bw.append(propertyFile);
		} catch (Exception e) {
			System.err.println("Unable to save file " + propertyFileName);
		} finally {
			if (bw != null) {
				try {
					bw.close();
				} catch (Exception e) {
					;
				}
			}
		}
	}
	
	public static void main(String[] argv) {
		
		ARQ.init();
		
		CommandLineParser  parser = new DefaultParser();
		HelpFormatter hf = new HelpFormatter();
		Options options = new Options();
		boolean saveProperties = false;
		
		options.addOption("n", "nodes", true, "number of ASs to include in the output");
		options.addOption("f", "format", true, "output format (turtle, n-triples, rdf/xml)");
		options.addOption("o", "directory", true, "output directory");
		options.addOption("s", "source", true, "source CAIDA file");
		options.addOption("p", "props", false, "generate a property file");
		options.addOption("h", "help", false, "help message, including available output formats");
		
		StringBuilder propertyFile = new StringBuilder();
		
		try {
			CommandLine line = parser.parse(options, argv, false);
			
			if (line.hasOption("h")) {
				hf.printHelp(execName, options);
				System.out.println("Available output formats: " + printFormats());
				System.exit(0);
			} 
			
			if (line.hasOption("n")) {
				maxVertices = Integer.parseInt(line.getOptionValue("n"));
			} 
			if (line.hasOption("f")) {
				boolean set = false;
				for(RDFFormat f: allowedFormats) {
					if (f.getLang().getName().equalsIgnoreCase(line.getOptionValue("f"))) {
						set = true;
						format = f;
					}
				}
				if (!set) throw new Exception("Unknown output format " + line.getOptionValue("f"));
			} 
			if (line.hasOption("o")) {
				rdfFolder = line.getOptionValue("o");
			} 
			if (line.hasOption("s")) {
				caidaSet = line.getOptionValue("s");
			}
			if (line.hasOption("p")) {
				saveProperties = true;
			}
		} catch (ParseException pe) {
			System.err.println("Unable to parse option: " + pe);
			hf.printHelp(execName, options);
			System.exit(255);
		} catch (NumberFormatException nfe) {
			System.err.println("Unable to parse integer: " + nfe);
			hf.printHelp(execName, options);
			System.exit(255);
		} catch (Exception e) {
			System.err.println("Unable to parse command line: " + e);
			hf.printHelp(execName, options);
			System.exit(255);
		}
		
		CAIDACSVImporter<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> imp = new CAIDACSVImporter<>(new SimpleCAIDA.SCVertexProvider(), new SimpleCAIDA.SCEdgeProvider());
		
		Graph<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> g = new SimpleGraph<SimpleCAIDA.Vertex, SimpleCAIDA.Edge>(SimpleCAIDA.Edge.class);
		try {
			imp.importGraph(g, new File(caidaSet));
		} catch (Exception e) {
			System.err.println("Exception encountered while importing file: " + e);
			e.printStackTrace();
		}
		System.out.println("Graph imported with " + g.vertexSet().size() + " vertices and " + g.edgeSet().size() + " edges");
		//for (SimpleCAIDAVertex v: g.vertexSet()) {
		//	System.out.print(v.name + " " );
		//}
		
//		int i = 10;
//		for (SimpleCAIDAVertex v: g.vertexSet()) {
//			if (i-- == 0) 
//				break;
//			Set<SimpleCAIDAEdge> edges = g.edgesOf(v);
//			System.out.println(v + " edges " + edges.size());
//			int j = 10;
//			for (SimpleCAIDAEdge e: edges) {
//				if (j-- == 0)
//					break;
//				System.out.println("\t" + e);
//			}
//		}
		
		// find a leaf node - one that only has CP edges. 
		List<SimpleCAIDA.Vertex> leafASs = SimpleCAIDA.findCPLeaves(g);
		
		System.out.println("There are " + leafASs.size() + " leaf ASs from a total of " + g.vertexSet().size());
		//pick the last one
		SimpleCAIDA.Vertex root = leafASs.get(leafASs.size()-1);
		
		BreadthFirstIterator<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> bfi = new BreadthFirstIterator<>(g, root);
		
		//Set<SimpleCAIDA.Vertex> visited = new HashSet<>();
		List<SimpleCAIDA.Vertex> visList = new ArrayList<>();
		int count = 0;
		// fake traversal to find out which vertices we'll touch
		while(bfi.hasNext()) {
			if ((maxVertices > 0) && (count++ == maxVertices)) 
				break;
			//visited.add(bfi.next());
			visList.add(bfi.next());
		}
		
		File caidaSetFile = new File(caidaSet);
		
		String caidaSetName = caidaSetFile.getName();
		
		// traverse for real
		OMNGen omg = new OMNGen();
		omg.declareDefaultCAIDATopology(caidaSetName);
		
		//
		// new bfs
		//
		boolean first = true;
		for(SimpleCAIDA.Vertex ed: visList) {
			if (first) {
				//System.out.println("Root " + ed);
				propertyFile.append("src.list=\\\nhttp://code.renci.org/projects/serpent#Node-" + ed.getLabel() + "\n\n");
				propertyFile.append("dest.list=");
				first = false;
				continue;
			}
			//System.out.println("Destination " + ed);
			for(int i = 0; i < visList.indexOf(ed); i++) {
				SimpleCAIDA.Edge e = g.getEdge(visList.get(i), ed);
				if (e != null) {
					propertyFile.append("\\\nhttp://code.renci.org/projects/serpent#Node-" + ed.getLabel() + ",");
					// To and From are reversed for some reason
					//System.out.println("Add edge from " + e.getTo() + " to " + e.getFrom());
					Individual to = omg.declareCAIDANode(e.getFrom().getLabel());
					Individual from = omg.declareCAIDANode(e.getTo().getLabel());
					omg.declareDirectedCAIDAEdge(from, to, e.getEdgeType());
					break;
				}
			}
		}
		// skip the last ','
		propertyFile.delete(propertyFile.length() - 1, propertyFile.length());
		propertyFile.append("\n\n");
		
		/**
		// didn't work /ib
		bfi = new BreadthFirstIterator<>(g, root);
		bfi.addTraversalListener(new OMNTraversalListener(omg, OMNTraversalListener.OMNFlavor.DIRECTED, visited));
		
		System.out.println("Traversing the tree from " + root + " and constructing RDF");
		count = 0;
		while(bfi.hasNext()) {
			if ((maxVertices > 0) && (count++ == maxVertices))
				break;
			SimpleCAIDA.Vertex nv= bfi.next();
			
			System.out.println(nv);
		}
		**/
		
		String extension = null;
		String filePrefix = rdfFolder + "/" + caidaSetName;
		String countSuffix = "";
		
		if (maxVertices > 0) {
			countSuffix = "." + maxVertices + "node";
		}
		
		if ((RDFFormat.TURTLE_PRETTY.equals(format))||(RDFFormat.TTL.equals(format))) {
			extension = "ttl";
		} else if (RDFFormat.NTRIPLES.equals(format)) {
				extension = "n3";
		} else if (RDFFormat.RDFXML_PRETTY.equals(format)) {
			extension = "xml";
		} else {
			System.err.println("Unknown format " + format + "using TURTLE");
			format = RDFFormat.TURTLE_PRETTY;
			extension = "ttl";
		}
		
		String rdfFileName = filePrefix + countSuffix + "." + extension;
		
		System.out.println("Saving file in " + rdfFileName + " using " + format);
		
		try {
			omg.toFile(rdfFileName, format);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		omg.close();
		
		// save a property file if needed
		if (saveProperties) {
			savePropertyFile(propertyFile, rdfFileName);
		}
	}
}
