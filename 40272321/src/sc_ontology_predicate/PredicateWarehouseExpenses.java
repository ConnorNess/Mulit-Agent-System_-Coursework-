package sc_ontology_predicate;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

public class PredicateWarehouseExpenses implements Predicate{
	
	private int expenseStorage;
	private int expensePenalties;
	private int expenseSuppliesCost;
	
	@Slot(mandatory=true)
	public int getExpenseStorage() { return expenseStorage; }
	public void setExpenseStorage(int expenseStorage) { this.expenseStorage = expenseStorage; }
	
	@Slot(mandatory=true)
	public int getExpensePenalties() { return expensePenalties; }
	public void setExpensePenalties(int expensePenalties) { this.expensePenalties = expensePenalties; }
	
	@Slot(mandatory=true)
	public int getExpenseSupplies() { return expenseSuppliesCost; }
	public void setExpenseSupplies(int expenseSuppliesCost) { this.expenseSuppliesCost = expenseSuppliesCost; }
}
