package sc_ontology_predicate;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class PredicateNewDay implements Predicate{

	private int dayNo;
	
	@Slot(mandatory = true)
	public int getDayNumber() { return dayNo; }
	public void setDayNumber(int dayNumber) { this.dayNo = dayNumber; }
}
