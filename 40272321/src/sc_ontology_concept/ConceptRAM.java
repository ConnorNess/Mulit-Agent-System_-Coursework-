package sc_ontology_concept;
import jade.content.onto.annotations.Slot;

public class ConceptRAM extends ConceptComponent{
	private String amount;
	
	@Slot(mandatory = true, permittedValues = {"4","8"})
	public String getRAMSize() { return amount; }
	public void setRAMSize(String amount) { this.amount = amount; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		ConceptRAM other = (ConceptRAM) obj;
		if (amount == null) {
			if (other.amount != null) return false;
		} else if (!amount.equals(other.amount))
			return false;
		
		return true;
	}
}
