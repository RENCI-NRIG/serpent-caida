package org.renci.serpent.caida.omn;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Common RDF/RDFS/OWL definitions
 * @author ibaldin
 *
 */
public class RDFHelper {

	public static final String W3_NS = "http://www.w3.org/";
	
    /** <p>The ontology model that holds the vocabulary terms</p> */
    private static OntModel m_model = ModelFactory.createOntologyModel( OntModelSpec.OWL_MEM, null );

    
    /** <p>The namespace of the vocabulary as a resource</p> */
    public static final Resource NAMESPACE = m_model.createResource( W3_NS );
	
	public static final Property RDF_TYPE = m_model.createProperty(W3_NS + "1999/02/22-rdf-syntax-ns#type");
	
    public static final Property RDFS_Label = m_model.createProperty(W3_NS + "2000/01/rdf-schema#label");
    
    public static final Property RDFS_SeeAlso = m_model.createProperty(W3_NS + "2000/01/rdf-schema#", "SeeAlso");
    
    public static final Property OWL_sameAs = m_model.createProperty(W3_NS + "2002/07/owl#"+"sameAs");
}
