package sc_ontology_concept;

public class ConceptSmall extends ConceptSmartphone{
	
	//A Small type must have screen of size 5" and battery of capacity 2000mAh
	public ConceptSmall() {
		ConceptScreen screen = new ConceptScreen();
			screen.setSize("5");
			this.setScreen(screen);
			
		ConceptBattery battery = new ConceptBattery();
			battery.setBatterySize("2000");
			this.setBattery(battery);
	}
}
