package sc_ontology_action;
import jade.content.AgentAction;
import jade.content.onto.annotations.Slot;
import sc_ontology_concept.ConceptOrder;

public class ActionOrder implements AgentAction{

	private ConceptOrder order;
	
	@Slot(mandatory = true)
	public ConceptOrder getOrder() { return order; }
	public void setOrder(ConceptOrder order) { this.order = order; }
}
