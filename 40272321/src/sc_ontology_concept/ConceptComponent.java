package sc_ontology_concept;
import jade.content.Concept;

public class ConceptComponent implements Concept{

	private int cost;
	public int getCost(){ return cost; }
	public void setCost(int cost) { this.cost = cost; }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cost;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ConceptComponent other = (ConceptComponent) obj;
		if (cost != other.cost) return false;
		
		return true;
	}
}
