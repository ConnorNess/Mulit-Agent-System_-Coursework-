package sc_ontology_predicate;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;
import sc_ontology_concept.ConceptOrder;

public class PredicateDeliveredOrder implements Predicate{

	private ConceptOrder order;
	
	@Slot(mandatory = true)
	public ConceptOrder getOrder() { return order; }
	public void setOrder(ConceptOrder order) { this.order = order; }
}
