package sc_ontology;
import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;
import jade.content.onto.Ontology;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Designed to simulate a supply chain with use of multiple agents and ontology design
*/

public class OntologySupplyChain extends BeanOntology{
	
	private static Ontology currInst = new OntologySupplyChain("sc_ontology");
	public static Ontology getInstance(){return currInst;}
	
	//Singleton pattern from the lecture slides
	private OntologySupplyChain(String name) {
		super(name); //Super is used to access the parent class
		try {
			add("sc_ontology_concept");
			add("sc_ontology_predicate");
			add("sc_ontology_action");
		}
		catch (BeanOntologyException e) { e.printStackTrace(); }
	}
}
