package sc_ontology_concept;
import jade.content.Concept;
import jade.content.onto.annotations.Slot;
import jade.core.AID;

public class ConceptOrder implements Concept{
	
	private int quantity;
	private ConceptSmartphone sphone;
	private int thisCost;
	private int dueDate;
	private int penalties;
	private AID aid;
	
	
	//Quantities
	@Slot(mandatory = true)
	public int getQuantity() { return quantity; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
	
	//Smartphone
	@Slot(mandatory = true)
	public ConceptSmartphone getSmartphone() { return sphone; }
	public void setSmartphone(ConceptSmartphone sphone) { this.sphone = sphone; }
	
	//Prices
	@Slot(mandatory = true)
	public int getThisCost() { return thisCost; }
	public void setThisCost(int thisCost) { this.thisCost = thisCost; }
	
	//DueDates
	@Slot(mandatory = true)
	public int getDueDate() { return dueDate; }
	public void setDueDate(int dueDate) { this.dueDate = dueDate; }
	
	//Penalties
	@Slot(mandatory = true)
	public int getPenalties() { return penalties; }
	public void setPenalties(int penalties) { this.penalties = penalties; }
	
	//AID
	@Slot(mandatory = true)
	public AID getAID() { return aid; }
	public void setAID(AID aid) { this.aid = aid; }
}
