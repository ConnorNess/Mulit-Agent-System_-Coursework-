package sc_ontology_concept;
import jade.content.Concept;
import jade.content.onto.annotations.AggregateSlot;
import jade.content.onto.annotations.Slot;
import java.util.ArrayList;

public class ConceptSupplies {
	
	private ArrayList<ConceptComponent> componentsList;
	private int componentsQuantity;
	
	@AggregateSlot(cardMin = 1)
	public ArrayList<ConceptComponent> getComponents(){ return componentsList; }
	public void setComponents(ArrayList<ConceptComponent> components) { this.componentsList = components; }
	
	@Slot(mandatory = true)
	public int getComponentsQuantity(){ return componentsQuantity; }
	public void setComponentsQuantity(int componentsQuantity) { this.componentsQuantity = componentsQuantity; }
}
