package sc_ontology_predicate;
import jade.content.Predicate;
import java.util.List;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import sc_ontology_concept.ConceptComponent;

public class PredicateSupplierInformation implements Predicate{
	
	private List<ConceptComponent> components;
	private int time;
	
	@AggregateSlot(cardMin = 1)
	public List<ConceptComponent> getComponents(){ return components; }
	public void setComponents(List<ConceptComponent> components) { this.components = components; }
	
	@Slot(mandatory = true)
	public int getTime() { return time; }
	public void setTime(int time) { this.time = time; }
}
