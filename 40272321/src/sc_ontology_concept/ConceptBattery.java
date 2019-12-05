package sc_ontology_concept;
import jade.content.onto.annotations.Slot;

//

public class ConceptBattery extends ConceptComponent{

	private String charge;
	
	@Slot(mandatory = true, permittedValues = {"2000", "3000"})
	public String getBatterySize() { return charge; }
	public void setBatterySize(String charge) { this.charge = charge; }

	@Override
	public int hashCode() {
		final int prime = 31; 
		//31 is used to hash as its a large enough prime number to make it unlikely that a bucket can be divisible by it
		//ty stackoverflow
		int result = super.hashCode();
		result = prime * result + ((charge == null) ? 0 : charge.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		ConceptBattery other = (ConceptBattery) obj;
		if (charge == null) {
			if (other.charge != null) return false;
		} else if (!charge.equals(other.charge)) return false;
		
		return true;
	}
}
