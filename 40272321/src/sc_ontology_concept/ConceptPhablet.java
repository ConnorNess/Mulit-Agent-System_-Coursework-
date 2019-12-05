package sc_ontology_concept;

public class ConceptPhablet extends ConceptSmartphone{
	
	//A Small type must have screen of size 7" and battery of capacity 3000mAh
		public ConceptPhablet() {
			ConceptScreen screen = new ConceptScreen();
				screen.setSize("7");
				this.setScreen(screen);
				
			ConceptBattery battery = new ConceptBattery();
				battery.setBatterySize("3000");
				this.setBattery(battery);
		}
}
