package sc_ontology_predicate;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class PredicatePayment implements Predicate{

	private int total;
	
	@Slot(mandatory = true)
	public int getTotal() { return total; }
	public void setTotal(int price) { this.total = price; }
}
