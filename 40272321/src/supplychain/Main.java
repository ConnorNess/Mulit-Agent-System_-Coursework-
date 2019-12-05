package supplychain;
import java.util.ArrayList;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

import sc_ontology_concept.ConceptComponent;
import sc_ontology_concept.ConceptBattery;
import sc_ontology_concept.ConceptRAM;
import sc_ontology_concept.ConceptScreen;
import sc_ontology_concept.ConceptStorage;

import sc_ontology_predicate.PredicateSupplierInformation;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*/

public class Main {
	
	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
				rma.start();
			
			AgentController DayCoordAgent = myContainer.createNewAgent("DayCoordinator", DayCoordAgent.class.getCanonicalName(), null);
				DayCoordAgent.start();
			
			Object[] supplierInformation1 = new Object[] {new String[]{"5", "7"}, 
					new int[]{100,150}, 
					new String[]{"64", "256"}, 
					new int[]{25,50}, 
					new String[]{"4", "8"}, 
					new int[]{30,60}, 
					new String[]{"2000", "3000"}, 
					new int[]{70,100}, 1};
			
			Object[] supplierInformation2 = new Object[] {new String[]{}, 
					new int[]{}, 
					new String[]{"64", "256"}, 
					new int[]{15,40}, 
					new String[]{"4", "8"}, 
					new int[]{20,35}, 
					new String[]{}, 
					new int[]{}, 4};

			
			AgentController supplierAgent1 = myContainer.createNewAgent("SupplierAgent1", SupplierAgent.class.getCanonicalName(), supplierInformation1);
				supplierAgent1.start();
			AgentController supplierAgent2 = myContainer.createNewAgent("SupplierAgent2", SupplierAgent.class.getCanonicalName(), supplierInformation2);
				supplierAgent2.start();

			
			AgentController manufacturerAgent = myContainer.createNewAgent("Manufacturer", ManufacturerAgent.class.getCanonicalName(), null);
				manufacturerAgent.start();
			
			
			AgentController warehouseAgent = myContainer.createNewAgent("Warehouse", WarehouseAgent.class.getCanonicalName(), null);
				warehouseAgent.start();
		
			
			AgentController customerAgent1 = myContainer.createNewAgent("CutomerAgent1", CustomerAgent.class.getCanonicalName(), null);
				customerAgent1.start();
			AgentController customerAgent2 = myContainer.createNewAgent("CutomerAgent2", CustomerAgent.class.getCanonicalName(), null);
				customerAgent2.start();
			AgentController customerAgent3 = myContainer.createNewAgent("CutomerAgent3", CustomerAgent.class.getCanonicalName(), null);
				customerAgent3.start();
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}
	}
}
