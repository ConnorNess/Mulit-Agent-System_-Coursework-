package sc_ontology_predicate;
import java.util.List;
import jade.content.Predicate;
import jade.content.onto.annotations.AggregateSlot;
import sc_ontology_concept.ConceptOrder;

public class PredicateReadyOrders implements Predicate{
	
	private List<ConceptOrder> orders;
	public List<ConceptOrder> getOrders(){ return orders; }
	public void setOrders(List<ConceptOrder> orders) { this.orders = orders; }
}
