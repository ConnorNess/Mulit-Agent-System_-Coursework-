package sc_ontology_predicate;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import sc_ontology_concept.ConceptSupplies;

public class PredicateSuppliesDelivered implements Predicate{

	private ConceptSupplies supplies;
	
	@Slot(mandatory = true)
	public ConceptSupplies getSupplies() { return supplies; }
	public void setSupplies(ConceptSupplies supplies) { this.supplies = supplies; }
}