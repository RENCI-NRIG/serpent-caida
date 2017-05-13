package org.renci.serpent.caida.omn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.StringWriter;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.tdb.TDBFactory;
import org.renci.serpent.caida.CAIDAImporterBase;
import org.renci.serpent.caida.util.CAIDAException;

import info.openmultinet.ontology.vocabulary.Nml_base;
import info.openmultinet.ontology.vocabulary.Nml_bgp;


/**
 * Class that helps generate OMN statements based on topology
 * @author ibaldin
 *
 */
public class OMNGen extends RDFHelper {
	public static final String SERPENT_NS = "http://code.renci.org/projects/serpent#";
	public static final String NML_NS = "http://schemas.ogf.org/nml/base/2013/02#";
	public static final String NML_BGP_NS = "http://schemas.ogf.org/nml/bgp/2017/03#";
	OntModel m;
	Individual defaultTopo = null;
	
	private void setPrefixes() {
		m.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        m.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        m.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        m.setNsPrefix("time", "http://www.w3.org/2006/time#");
        m.setNsPrefix("nml", NML_NS);
        m.setNsPrefix("bgp", NML_BGP_NS);
        m.setNsPrefix("serpent", SERPENT_NS);
	}
	
	/**
	 * Create a new generator with TDB model
	 * @param tdb
	 */
	public OMNGen(String tdbFolderName) throws CAIDAException {
		File dir = new File(tdbFolderName);
		if (!dir.exists()) {
			throw new CAIDAException("Folder " + tdbFolderName + " does not exist, unable to create TDB model!");
		}
		Dataset ds = TDBFactory.createDataset(dir.getAbsolutePath());
		Model fileModel = ds.getDefaultModel();
		m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, fileModel);
		setPrefixes();
	}
	
	/**
	 * Create new generator with in-memory model
	 */
	public OMNGen() {
		m = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		setPrefixes();
	}
	
	/**
	 * Add switching service etc
	 * @param node
	 */
	private void setDefaultNodeAttributes(Individual node) {
		Individual in = m.createIndividual(SERPENT_NS + "NodeSwitchingService-" + node.getLocalName(), Nml_base.SwitchingService);
		node.addProperty(Nml_base.hasService, in);
	}
	
	/**
	 * Link it to a default topology
	 * @param name
	 * @return
	 */
	public Individual declareCAIDANode(String name) {
		assert(name != null);
		
		Individual in = m.getIndividual(SERPENT_NS + "Node-" + name);
		if (in != null)
			return in;
		in = m.createIndividual(SERPENT_NS + "Node-" + name, Nml_base.Node);
		if (defaultTopo != null)
			defaultTopo.addProperty(Nml_base.hasNode, in);
		setDefaultNodeAttributes(in);
		return in;
	}
	
	/**
	 * Link it to a given topology
	 * @param name
	 * @param topo
	 * @return
	 */
	public Individual declareCAIDANode(String name, Individual topo) {
		assert(name != null);
		assert(topo != null);
		
		Individual in = m.getIndividual(SERPENT_NS + "Node-" + name);
		if (in != null)
			return in;
		in = m.createIndividual(SERPENT_NS + "Node-" + name, Nml_base.Node);
		topo.addProperty(Nml_base.hasNode, in);
		setDefaultNodeAttributes(in);
		return in;
	}
	
	/**
	 * Declare a topology
	 * @param name
	 * @return
	 */
	public Individual declareCAIDATopology(String name) {
		assert(name != null);
		
		Individual in = m.createIndividual(SERPENT_NS + "Topology-" + name, Nml_base.Topology);
		return in;
	}
	
	/**
	 * Declare a topology and mark as default
	 * @param name
	 * @return
	 */
	public Individual declareDefaultCAIDATopology(String name) {
		assert(name != null);
		
		Individual in = declareCAIDATopology(name);
		setDefaultTopo(in);
		return in;
	}
	
	/**
	 * Set default topology for this generator
	 * @param t
	 */
	public void setDefaultTopo(Individual t) {
		defaultTopo = t;
	}
	
	/**
	 * Declare two bidirectional ports with two uni-directional edges between them (consistent with traditional
	 * NML)
	 * @param node1
	 * @param node2
	 * @param eType
	 */
	public void declareBidirectionalCAIDAEdge(Individual n1, Individual n2, CAIDAImporterBase.EdgeType eType) {
		// create in: and out: ports
		Individual n1portOut = m.createIndividual(SERPENT_NS + "Port-" + n1.getLocalName() + "-" + n2.getLocalName() + "-out", Nml_base.Port); 
		Individual n1portIn = m.createIndividual(SERPENT_NS+ "Port-" + n1.getLocalName() + "-" + n2.getLocalName() + "-in", Nml_base.Port);
		Individual n2portOut = m.createIndividual(SERPENT_NS + "Port-" + n2.getLocalName() + "-" + n1.getLocalName() + "-out", Nml_base.Port); 
		Individual n2portIn = m.createIndividual(SERPENT_NS+ "Port-" + n2.getLocalName() + "-" + n1.getLocalName() + "-in", Nml_base.Port);
		
		// link to bidirectional ports
		Individual n1BiDirPort = m.createIndividual(SERPENT_NS + "BiDirPort-" + n1.getLocalName() + "-" + n2.getLocalName(), Nml_base.BidirectionalPort);
		Individual n2BiDirPort = m.createIndividual(SERPENT_NS + "BiDirPort-" + n2.getLocalName() + "-" + n1.getLocalName(), Nml_base.BidirectionalPort);

		n1BiDirPort.addProperty(Nml_base.hasPort, n1portOut);
		n1BiDirPort.addProperty(Nml_base.hasPort, n1portIn);
		n2BiDirPort.addProperty(Nml_base.hasPort, n2portOut);
		n2BiDirPort.addProperty(Nml_base.hasPort, n2portIn);
		
		
		n1.addProperty(Nml_base.hasOutboundPort, n1portOut);
		n1.addProperty(Nml_base.hasInboundPort, n1portIn);
		
		n2.addProperty(Nml_base.hasOutboundPort, n2portOut);
		n2.addProperty(Nml_base.hasInboundPort, n2portIn);
		
		Individual n1n2Link = null, n2n1Link = null;
		
		switch(eType) {
		case CP:
			n1n2Link = m.createIndividual(SERPENT_NS + "Link-" + n1.getLocalName() + "-" + n2.getLocalName(), Nml_bgp.CPLink);
			n2n1Link = m.createIndividual(SERPENT_NS + "Link-" + n2.getLocalName() + "-" + n1.getLocalName(), Nml_bgp.CPLink);
			n1portOut.addProperty(Nml_bgp.isCPSource, n1n2Link);
			n2portIn.addProperty(Nml_bgp.isCPSink, n1n2Link);
			n1portIn.addProperty(Nml_bgp.isCPSink, n2n1Link);
			n2portOut.addProperty(Nml_bgp.isCPSource, n2n1Link);
			break;
		case PP:
			n1n2Link = m.createIndividual(SERPENT_NS + "Link-" + n1.getLocalName() + "-" + n2.getLocalName(), Nml_bgp.PPLink);
			n2n1Link = m.createIndividual(SERPENT_NS + "Link-" + n2.getLocalName() + "-" + n1.getLocalName(), Nml_bgp.PPLink);
			n1portOut.addProperty(Nml_bgp.isPPSource, n1n2Link);
			n2portIn.addProperty(Nml_bgp.isPPSink, n1n2Link);
			n1portIn.addProperty(Nml_bgp.isPPSink, n2n1Link);
			n2portOut.addProperty(Nml_bgp.isPPSource, n2n1Link);
			break;
		}
		
		Resource n1Ss = getStitchingService(n1);
		Resource n2Ss = getStitchingService(n2);
		
		assert((n1Ss != null) && (n2Ss != null));
		
		n1Ss.addProperty(Nml_base.hasOutboundPort, n1portOut);
		n1Ss.addProperty(Nml_base.hasInboundPort, n1portIn);
		
		n2Ss.addProperty(Nml_base.hasOutboundPort, n2portOut);
		n2Ss.addProperty(Nml_base.hasInboundPort, n2portIn);
	}
	
	/**
	 * Declare an directed single edge (does not use inverse properties like isOutboundPort and hasSink). This is 
	 * consistent with traditional NML
	 * @param fromNode
	 * @param toNode
	 * @param eType
	 */
	public void declareDirectedCAIDAEdgeNoInverse(Individual fromNode, Individual toNode, CAIDAImporterBase.EdgeType eType) {
		assert(fromNode != null);
		assert(toNode != null);
		
		Individual portOut = m.createIndividual(SERPENT_NS + "Port-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_base.Port); 
		Individual portIn = m.createIndividual(SERPENT_NS+ "Port-" + toNode.getLocalName() + "-" + fromNode.getLocalName(), Nml_base.Port);
		fromNode.addProperty(Nml_base.hasOutboundPort, portOut);
		toNode.addProperty(Nml_base.hasInboundPort, portIn);
		
		Individual link = null;
		switch(eType) {
		case CP:
			link = m.createIndividual(SERPENT_NS + "Link-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_bgp.CPLink);
			portOut.addProperty(Nml_bgp.isCPSource, link);
			portIn.addProperty(Nml_bgp.isCPSink, link);
			break;
		case PP:
			link = m.createIndividual(SERPENT_NS + "Link-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_bgp.PPLink);
			portOut.addProperty(Nml_bgp.isPPSource, link);
			portIn.addProperty(Nml_bgp.isPPSink, link);
			break;
		}
		
		Resource fromSs = getStitchingService(fromNode);
		Resource toSs = getStitchingService(toNode);
		
		assert((fromSs != null) && (toSs != null));
		
		fromSs.addProperty(Nml_base.hasOutboundPort, portOut);
		toSs.addProperty(Nml_base.hasInboundPort, portIn);
	}
	
	/**
	 * Declare a directed link between from and to (edge can only be traversed in fromNode-toNode direction by using direct and inverse properties). Traditional
	 * NML does not use inverse properties. This is to avoid small cycles in the resulting graph.
	 * @param fromNode
	 * @param toNode
	 * @param eType
	 */
	public void declareDirectedCAIDAEdge(Individual fromNode, Individual toNode, CAIDAImporterBase.EdgeType eType) {
		assert(fromNode != null);
		assert(toNode != null);
		
		Individual portOut = m.createIndividual(SERPENT_NS + "Port-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_base.Port); 
		Individual portIn = m.createIndividual(SERPENT_NS+ "Port-" + toNode.getLocalName() + "-" + fromNode.getLocalName(), Nml_base.Port);
		fromNode.addProperty(Nml_base.hasOutboundPort, portOut);
		portIn.addProperty(Nml_bgp.isInboundPort, toNode);
		
		Individual link = null;
		switch(eType) {
		case CP:
			link = m.createIndividual(SERPENT_NS + "Link-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_bgp.CPLink);
			portOut.addProperty(Nml_bgp.isCPSource, link);
			link.addProperty(Nml_bgp.hasCPSink, portIn);
			break;
		case PP:
			link = m.createIndividual(SERPENT_NS + "Link-" + fromNode.getLocalName() + "-" + toNode.getLocalName(), Nml_bgp.PPLink);
			portOut.addProperty(Nml_bgp.isPPSource, link);
			link.addProperty(Nml_bgp.hasPPSink, portIn);
			break;
		}
		
		Resource fromSs = getStitchingService(fromNode);
		Resource toSs = getStitchingService(toNode);
		
		assert((fromSs != null) && (toSs != null));
		
		fromSs.addProperty(Nml_base.hasOutboundPort, portOut);
		portIn.addProperty(Nml_bgp.isInboundPort, toSs);
	}
	
	/**
	 * Locate a stitching service of the node, if available
	 * @param node
	 * @return
	 */
	private Resource getStitchingService(Individual node) {
		// get to the switching service
		Resource stitchService = null;
		for(NodeIterator nIter = node.listPropertyValues(Nml_base.hasService); nIter.hasNext(); ) {
			RDFNode n = nIter.next();
			if (n.isResource()) {
				Resource r = (Resource)n;
				if (r.hasProperty(RDF_TYPE, Nml_base.SwitchingService)) {
					stitchService = r;
					break;
				}
			}
		}
		return stitchService;
	}

	/**
	 * produce N3 output of the model
	 */
	public String toN3String() {
		StringWriter sw = new StringWriter();
		m.write(sw, "N3");
		return sw.toString();
	}

	@Override
	public String toString() {
		return toN3String();
	}

	/**
	 * produce RDF-XML output of the model
	 * @return
	 */
	public String toXMLString() {
		StringWriter sw = new StringWriter();
		m.write(sw);
		return sw.toString();
	}
	
	public void close() {
		m.close();
	}
	
	public void toFile(String name, RDFFormat format) throws CAIDAException {
		FileOutputStream fos = null;
		
		try {
			fos = new FileOutputStream(name);
			RDFDataMgr.write(fos, m, format);
		}
		catch (Exception e) {
			throw new CAIDAException(e.toString());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch(Exception ee) {
					
				}
			}
		}
	}
}
