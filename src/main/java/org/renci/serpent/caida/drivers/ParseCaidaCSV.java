package org.renci.serpent.caida.drivers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.jena.ontology.Individual;
import org.apache.jena.query.ARQ;
import org.apache.jena.riot.RDFFormat;
import org.javatuples.Triplet;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;
import org.renci.serpent.caida.CAIDACSVImporter;
import org.renci.serpent.caida.CAIDAImporterBase.EdgeType;
import org.renci.serpent.caida.SimpleCAIDA;
import org.renci.serpent.caida.omn.OMNGen;

public class ParseCaidaCSV {
	private static final String PLAIN_NODE = "Node-";
	private static final String SERPENT_NODE_NS = "http://code.renci.org/projects/serpent#Node-";
	static int maxVertices = -1;
	static int maxDests = -1;
	static String caidaSet = null;
	static String rdfFolder = null;
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
	
	public static List<SimpleCAIDA.Vertex> HybridTraverse(Graph<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> g, SimpleCAIDA.Vertex root, OMNGen omg, int size) {
		List<SimpleCAIDA.Vertex> bfsqueue = new ArrayList<>();
		int cnt = 0;
		int bfsIdx = 0;
		
		bfsqueue.add(root);
		
		Random gen = new Random();
		
		while(cnt < size) {
			SimpleCAIDA.Vertex parent = null;
			if (bfsIdx < bfsqueue.size()) {
				 parent = bfsqueue.get(bfsIdx++);
			}
			else {
				break;
			}
			for(Iterator<SimpleCAIDA.Edge> it = g.edgesOf(parent).iterator(); it.hasNext();) {
				SimpleCAIDA.Edge edge = it.next();
				SimpleCAIDA.Vertex child = edge.getFrom();
				// figure out which end of the edge we need
				if (child == parent) {
					if (gen.nextInt(100) > 50)
						child = edge.getTo();
				}

				if (!bfsqueue.contains(child)) {
					//System.out.println("Edge: " + parent + " --> " + child);
					Individual from = omg.declareCAIDANode(parent.getLabel());
					Individual to = omg.declareCAIDANode(child.getLabel());
					omg.declareDirectedCAIDAEdge(from, to, edge.getEdgeType());
					bfsqueue.add(child);
					cnt++;
					if (cnt == size) {
						break;
					}
				}
			}
		}
		return bfsqueue.subList(1, bfsqueue.size());
	}
	
	/**
	 * Does BFS traversal
	 * @param g
	 * @param root
	 * @param omg
	 * @param size
	 * @return
	 */
	public static List<SimpleCAIDA.Vertex> BFSTraverse(Graph<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> g, SimpleCAIDA.Vertex root, OMNGen omg, int size) {
		List<SimpleCAIDA.Vertex> bfsqueue = new ArrayList<>();
		int cnt = 0;
		int bfsIdx = 0;
		
		bfsqueue.add(root);
		
		while(cnt < size) {
			SimpleCAIDA.Vertex parent = null;
			if (bfsIdx < bfsqueue.size()) {
				 parent = bfsqueue.get(bfsIdx++);
			}
			else {
				break;
			}
			for(Iterator<SimpleCAIDA.Edge> it = g.edgesOf(parent).iterator(); it.hasNext();) {
				SimpleCAIDA.Edge edge = it.next();
				SimpleCAIDA.Vertex child = edge.getFrom();
				// figure out which end of the edge we need
				if (child == parent) 
					child = edge.getTo();

				if (!bfsqueue.contains(child)) {
					//System.out.println("Edge: " + parent + " --> " + child);
					Individual from = omg.declareCAIDANode(parent.getLabel());
					Individual to = omg.declareCAIDANode(child.getLabel());
					omg.declareDirectedCAIDAEdge(from, to, edge.getEdgeType());
					bfsqueue.add(child);
					cnt++;
					if (cnt == size) {
						break;
					}
				}
			}
		}
		return bfsqueue.subList(1, bfsqueue.size());
	}
	
	public static List<SimpleCAIDA.Vertex> DFSTraverse(Graph<SimpleCAIDA.Vertex, SimpleCAIDA.Edge> g, SimpleCAIDA.Vertex root, OMNGen omg, int size) {
		// first field is node itself, second field is its parent, third is edge type
		Deque<Triplet<SimpleCAIDA.Vertex, SimpleCAIDA.Vertex, EdgeType>> dfsstack = new LinkedList<>();
		List<SimpleCAIDA.Vertex> visited = new LinkedList<>();
		int cnt = 0;
		
		dfsstack.push(new Triplet<SimpleCAIDA.Vertex, SimpleCAIDA.Vertex, EdgeType>(root, null, null));
	
		while(cnt < size) {
			Triplet<SimpleCAIDA.Vertex, SimpleCAIDA.Vertex, EdgeType> nodeTriplet = dfsstack.pop();
			SimpleCAIDA.Vertex child = nodeTriplet.getValue0();
			
			if (!visited.contains(child)) {
				visited.add(child);
				
				if (nodeTriplet.getValue1() != null) {
					Individual from = omg.declareCAIDANode(nodeTriplet.getValue1().getLabel());
					Individual to = omg.declareCAIDANode(child.getLabel());
					omg.declareDirectedCAIDAEdge(from, to, nodeTriplet.getValue2());
				}
				cnt++;
				
				for(Iterator<SimpleCAIDA.Edge> it = g.edgesOf(child).iterator(); it.hasNext();) {
					SimpleCAIDA.Edge edge = it.next(); 
					SimpleCAIDA.Vertex childOfChild = edge.getFrom();
					// figure out which end of the edge we need
					if (childOfChild == child) 
						childOfChild = edge.getTo();
					
					dfsstack.push(new Triplet<SimpleCAIDA.Vertex, SimpleCAIDA.Vertex, EdgeType>(childOfChild, child, edge.getEdgeType()));
				}
			}
		}
		
		return visited.subList(1, visited.size());
	}
	
	/**
	 * 
	 * @param propertyFile - file to save to
	 * @param rdfFileName - name of the rdf file (facts.file)
	 * @param eval - format for evaluation (true) or Tarjan (false)
	 */
	public static void savePropertyFile(StringBuilder propertyFile, String rdfFileName, boolean eval) {
		
		if (!eval) {
			propertyFile.append("\nlabels.list=\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#hasOutboundPort,\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#isInboundPort,\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#hasCPSink,\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#isCPSource\n\n");

			propertyFile.append("\ntypes.list=\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#Port,\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/base/2013/02#Node,\\\n");
			propertyFile.append("http://schemas.ogf.org/nml/bgp/2017/03#CPLink\n\n");
		}
		
		String factsFileVal = rdfFileName;
		if (eval) {
			Path factFilePath = Paths.get(rdfFileName);
			factsFileVal = factFilePath.getFileName().toString();
		}
		propertyFile.append("facts.file="+ factsFileVal + "\n\n");
		
		propertyFile.append("facts.file.syntax=" + format.getLang().getName() + "\n\n");
		
		if (!eval) {
			propertyFile.append("constraint=only\n\n");
			propertyFile.append("type.constraint=only\n\n");
		}
		
		propertyFile.insert(0, "# Property file for " + maxVertices + " ASs and " + (maxDests == -1 ? "all" : maxDests) + " destinations for datafile " + rdfFileName + "\n\n");
		
		String propertyFileName = rdfFolder + "/caida" + maxVertices + "-" + (maxDests == -1 ? "all" : maxDests) + ".properties";
		
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
		boolean saveProperties = false, formatEvalProps = false;
		int traverseType = 1;
		
		options.addOption("n", "nodes", true, "number of ASs to include in the output");
		options.addOption("f", "format", true, "output format (turtle, n-triples, rdf/xml)");
		options.addOption("o", "directory", true, "output directory");
		options.addOption("s", "source", true, "source CAIDA file");
		options.addOption("p", "props", false, "generate a Tarjan property file");
		options.addOption("e", "eval", false, "format properties for evaluator (use with -p to save)");
		options.addOption("c", "count", true, "number of destinations (defaults to n-1)");
		options.addOption("h", "help", false, "help message, including available output formats");
		options.addOption("t", "tree", true, "tree type - 1: BFS, 2: DFS, 3: Hybrid");
		
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
			if (line.hasOption("c")) {
				maxDests = Integer.parseInt(line.getOptionValue("c"));
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
			if (line.hasOption("e")) {
				formatEvalProps = true;
			}
			if (line.hasOption("t")) {
				traverseType = Integer.parseInt(line.getOptionValue("t"));
				if ((traverseType < 1) || (traverseType > 3))
					throw new Exception("Allowed traverse types are 1, 2 and 3");
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
		
		if ((caidaSet == null) || (rdfFolder == null)) {
			System.err.println("You must specify the location of the CAIDA AS REL dataset and the output folder\n");
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
		
		// find a leaf node - one that only has CP edges. 
		List<SimpleCAIDA.Vertex> leafASs = SimpleCAIDA.findCPLeaves(g);
		
		System.out.println("There are " + leafASs.size() + " leaf ASs from a total of " + g.vertexSet().size());
		//pick the last one
		SimpleCAIDA.Vertex root = leafASs.get(leafASs.size()-1);
		System.out.println("Root is " + root);

		/**
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
		**/
		
		File caidaSetFile = new File(caidaSet);
		
		String caidaSetName = caidaSetFile.getName();
		
		// traverse for real
		OMNGen omg = new OMNGen();
		omg.declareDefaultCAIDATopology(caidaSetName);
		
		// for evaluation we don't need full namespace
		String nodeNs = (formatEvalProps ? PLAIN_NODE : SERPENT_NODE_NS);
		propertyFile.append("src.list=\\\n" + nodeNs + root.getLabel() + "\n\n");
		
		List<SimpleCAIDA.Vertex> dsts = null;
		// do traversal
		switch(traverseType) {
		case 1: System.out.println("Running BFS Traverse"); 
			dsts = BFSTraverse(g, root, omg, maxVertices);
			break;
		case 2: System.out.println("Running DFS Traverse"); 
			dsts = DFSTraverse(g, root, omg, maxVertices);
			break;
		case 3: System.out.println("Running Hybrid Traverse"); 
			dsts = HybridTraverse(g, root, omg, maxVertices);
		}
		
		List<SimpleCAIDA.Vertex> realDsts = dsts;
		
		// find a random set of maxDests destinations from the list
		if ((maxDests != -1) && (maxDests <= dsts.size())) {
			realDsts = new ArrayList<>();
			for(int i = 0; i < maxDests; i++ ) {
				Collections.shuffle(dsts);
				realDsts.add(dsts.remove(0));
			}
		} 
		
		// generate maxDests destinations (or a full list)
		propertyFile.append("dest.list=");

		for(SimpleCAIDA.Vertex dst: realDsts) {
			propertyFile.append("\\\n" + nodeNs + dst.getLabel() + ",");
		}

		// skip the last ','
		propertyFile.delete(propertyFile.length() - 1, propertyFile.length());
		propertyFile.append("\n\n");

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
		
		String rdfFileName = null;
		
		rdfFileName = filePrefix + countSuffix + "." + extension;
		
		System.out.println("Saving file in " + rdfFileName + " using " + format);
		
		try {
			omg.toFile(rdfFileName, format);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		omg.close();
		
		// save a property file if needed
		if (saveProperties) {
			savePropertyFile(propertyFile, rdfFileName, formatEvalProps);
		}

	}
}
