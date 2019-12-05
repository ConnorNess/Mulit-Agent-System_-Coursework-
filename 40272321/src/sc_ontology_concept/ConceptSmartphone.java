package sc_ontology_concept;
import jade.content.Concept;
import jade.content.onto.annotations.Slot;

public class ConceptSmartphone implements Concept{
	
	private ConceptScreen screen;
	private ConceptStorage storage;
	private ConceptRAM ram;
	private ConceptBattery battery;
	
	@Slot(mandatory = true)
	public ConceptScreen getScreen() { return screen; }
	protected void setScreen(ConceptScreen screen) { this.screen = screen; }
	
	@Slot(mandatory = true)
	public ConceptStorage getStorage() { return storage; }
	public void setStorage(ConceptStorage storage) { this.storage = storage; }
	
	@Slot(mandatory = true)
	public ConceptRAM getRam() { return ram; }
	public void setRam(ConceptRAM ram) { this.ram = ram; }
	
	@Slot(mandatory = true)
	public ConceptBattery getBattery() { return battery; }
	protected void setBattery(ConceptBattery battery) { this.battery = battery; }
}
