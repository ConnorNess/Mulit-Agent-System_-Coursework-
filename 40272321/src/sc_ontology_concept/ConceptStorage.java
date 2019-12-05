package sc_ontology_concept;
import jade.content.onto.annotations.Slot;

public class ConceptStorage extends ConceptComponent{

	private String storage;
	
	@Slot(mandatory = true, permittedValues = {"64", "256"})
	public String getStorageSize() { return storage; }
	public void setStorageSize(String storage) { this.storage = storage; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((storage == null) ? 0 : storage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (getClass() != obj.getClass()) return false;
		ConceptStorage other = (ConceptStorage) obj;
		if (storage == null) {
			if (other.storage != null) return false;
		} else if (!storage.equals(other.storage))
			return false;
		
		return true;
	}
}
