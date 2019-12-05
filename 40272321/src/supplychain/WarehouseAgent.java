package supplychain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.HashMap;
import java.util.LinkedList;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import sc_ontology.OntologySupplyChain;
import sc_ontology_action.*;
import sc_ontology_concept.*;
import sc_ontology_predicate.*;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Acts as a warehouse storage of materials
*/

public class WarehouseAgent extends Agent{

	private Codec codec = new SLCodec();
	private Ontology ontology = OntologySupplyChain.getInstance();
	
	private AID AIDmanu;
	private AID AIDday;
	private AID[] AIDsupp;
	
	private ArrayList<Supplier> suppliers;
	private ArrayList<ConceptOrder> ordersReady;
	private ArrayList<PendingOrder> ordersPend;
	private HashMap<ConceptComponent,Integer> stock;
	private ArrayList<SuppliesNeeded> suppliesToOrder;
	
	private static final int maxPhonesThisDay = 50; //Change this to alter the max amount of items to be made in a day
	private static final int componentStorageCostThisDay = 5; //Change this to alter how much the cost of storage per component is
	private int assembledSmartphonesThisDayNo;
	
	private int thisDayCostSupplies;
	private int thisDayCostPenalties;
	private int thisDayCostStorage;

	protected void setup() {
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
			sd.setType("Warehouse");
			sd.setName(getLocalName() + "-warehouse-agent");
			dfd.addServices(sd);
		
		try { DFService.register(this, dfd); } 
		catch(FIPAException e) { e.printStackTrace(); }

		suppliers = new ArrayList<Supplier>();
		ordersReady = new ArrayList<ConceptOrder>();
		ordersPend = new ArrayList<PendingOrder>();
		stock = new HashMap<ConceptComponent, Integer>();
		suppliesToOrder = new ArrayList<SuppliesNeeded>();

		doWait(3000);
		this.addBehaviour(new LocateManufacturer());
		this.addBehaviour(new LocateSupplier());
		this.addBehaviour(new LocateDay());
		this.addBehaviour(new DayWaiter());
	}

	protected void takeDown() {
		System.out.println("Agent " + this.getLocalName() + " is terminating.");
		
		try { DFService.deregister(this); } 
		catch(FIPAException e) { e.printStackTrace(); }
	}
	
	private class LocateManufacturer extends OneShotBehaviour{
		
		public void action() {
			
			DFAgentDescription manufacturerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
				sd.setType("Manufacturer");
				manufacturerTemplate.addServices(sd);
			
			try {
				AIDmanu = new AID();
				DFAgentDescription[] manufacturerAgents = DFService.search(myAgent, manufacturerTemplate);
				AIDmanu = manufacturerAgents[0].getName();
			} 
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}

	private class LocateSupplier extends OneShotBehaviour{
		public void action() {
			DFAgentDescription supplierTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Supplier");
			supplierTemplate.addServices(sd);
			try {
				DFAgentDescription[] supplierAgents = DFService.search(myAgent, supplierTemplate);
				int size = supplierAgents.length;
				AIDsupp = new AID[size];
				for(int i = 0; i < size; i++) {
					AIDsupp[i] = supplierAgents[i].getName();
				}
			} catch(FIPAException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class LocateDay extends OneShotBehaviour{
		
		public void action() {
			
			DFAgentDescription dayCoordinatorTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
				sd.setType("DayCoordinator");
				dayCoordinatorTemplate.addServices(sd);
			
			try {
				AIDday = new AID();
				DFAgentDescription[] dayCoordinatorAgents = DFService.search(myAgent, dayCoordinatorTemplate);
				AIDday = dayCoordinatorAgents[0].getName();
			} 
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}
	
	//checks if its a new day, if not the run is ended
	public class DayWaiter extends CyclicBehaviour{
		
		public void action() {
			
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(AIDday));
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				try {
					ContentElement ce = getContentManager().extractContent(msg);;
					
					//methods carried out on new day
					if(ce instanceof PredicateNewDay) {
						assembledSmartphonesThisDayNo = 0;
						thisDayCostSupplies = 0;
						thisDayCostPenalties = 0;
						thisDayCostStorage = 0;

						SequentialBehaviour dailyActivity = new SequentialBehaviour();
						if(suppliers.isEmpty()) { dailyActivity.addSubBehaviour(new GetSupplierDetailsBehaviour()); }
						
						dailyActivity.addSubBehaviour(new DeliveredSuppliesProcessor());
						dailyActivity.addSubBehaviour(new OrderPendingProcessor());
						dailyActivity.addSubBehaviour(new OrderPendingUpdate_PenaltiesCalculator());
						dailyActivity.addSubBehaviour(new OrderRequestManufacturerProcessor());
						dailyActivity.addSubBehaviour(new SuppliesRequest());
						dailyActivity.addSubBehaviour(new OrderReadySender());
						dailyActivity.addSubBehaviour(new StorageCosts());
						dailyActivity.addSubBehaviour(new WarehouseExpenses());
						myAgent.addBehaviour(dailyActivity);
					} 
					else { myAgent.doDelete(); }
					
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} 
			else { block(); }
		}
	}
	
	private class GetSupplierDetailsBehaviour extends Behaviour{
		
		private int step = 0;
		int numResponsesReceived = 0;
		
		public void action() {
			
			switch(step) {
			//Send msg to suppliers
			case 0:
				PredicateSupplierDetails supplierDetails = new PredicateSupplierDetails();
				ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
				
				for(int i = 0; i < AIDsupp.length; i++) { msg.addReceiver(AIDsupp[i]); }
				
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
				
				try {
					getContentManager().fillContent(msg, supplierDetails);
					myAgent.send(msg);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
				step++;
				break;
				
			//Recieve response from supplirs
			case 1:
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg1 = myAgent.receive(mt);
				
				if(msg1 != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg1);
						if(ce instanceof PredicateSupplierInformation) {
							PredicateSupplierInformation supplierInformation = (PredicateSupplierInformation) ce;
							
							//Store supplier info
							suppliers.add(new Supplier(supplierInformation.getComponents(), supplierInformation.getTime(), msg1.getSender()));
							numResponsesReceived++;
							
							if(numResponsesReceived == AIDsupp.length) { step++; }
						} 
						else { myAgent.postMessage(msg1); }
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				} 
				else { block(); }
				break;
			}
			
		}
		public boolean done() { return step == 2; }
	}

	private class DeliveredSuppliesProcessor extends Behaviour{
		private int numbSuppliersDone = 0;
		
		public void action() {
			
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					if(ce instanceof PredicateSuppliesDelivered) {
						
						//Store supplies
						PredicateSuppliesDelivered suppliesDelivered = (PredicateSuppliesDelivered) ce;
						int quantityPerComponent = suppliesDelivered.getSupplies().getComponentsQuantity();
						
						for(ConceptComponent component : suppliesDelivered.getSupplies().getComponents()) {

							//check thiscomponent against current stored price
							if(stock.containsKey(component)) {
								//update quantity
								int quantity = quantityPerComponent + stock.get(component);
								stock.replace(component, quantity);
							} 
							else { stock.put(component, quantityPerComponent); } 
						}
					} 
					else if (ce instanceof PredicateNoMoreSuppliesDay) { numbSuppliersDone++; }  //Once a supplier  has sent all supplies, the supplier sends a notification of no more supplies on thisday
					else { myAgent.postMessage(msg); }
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} 
			else { block(); }
			
		}
		public boolean done() { return numbSuppliersDone == AIDsupp.length; }
	}
	
	private class OrderPendingProcessor extends OneShotBehaviour{
		
		public void action() {
		
			//First in, first out
			ArrayList<PendingOrder> ordersToRemove = new ArrayList<PendingOrder>();
			
			for(PendingOrder pendingOrder : ordersPend) {
				ConceptOrder order = pendingOrder.getPendingOrder();
				
				//more orders not processed if required items amount cannot be made
				if(assembledSmartphonesThisDayNo + order.getQuantity() > maxPhonesThisDay) { break; }
				
				//Stock quantity check for needed components
				ConceptScreen screen = order.getSmartphone().getScreen();
				ConceptBattery battery = order.getSmartphone().getBattery();
				ConceptStorage storage = order.getSmartphone().getStorage();
				ConceptRAM ram = order.getSmartphone().getRam();
				
				if(stock.containsKey(screen) && stock.containsKey(storage) && stock.containsKey(ram) && stock.containsKey(battery)) {
					//Stock quantity check for thisorder
					int screenAvailables = stock.get(order.getSmartphone().getScreen());
					int storageAvailables = stock.get(order.getSmartphone().getStorage());
					int ramAvailables = stock.get(order.getSmartphone().getRam());
					int batteryAvailables = stock.get(order.getSmartphone().getBattery());
					int requiredQuantity = order.getQuantity();
					
					if(screenAvailables >= requiredQuantity && storageAvailables >= requiredQuantity && ramAvailables >= requiredQuantity && batteryAvailables >= requiredQuantity) {
						//Updates counter of items to be assembled on thisday
						assembledSmartphonesThisDayNo += requiredQuantity;

						ordersReady.add(order);
						
						// Update stock
						stock.replace(screen, screenAvailables - requiredQuantity);
						stock.replace(battery, batteryAvailables - requiredQuantity);
						stock.replace(storage, storageAvailables - requiredQuantity);
						stock.replace(ram, ramAvailables - requiredQuantity);

						ordersToRemove.add(pendingOrder); 
					} 
					else { break; } //If components aren't in stock for current assembly, more orders are not processed
				} 
				else { break; } //If components aren't in stock for current assembly, more orders are not processed
			}
			if(!ordersToRemove.isEmpty()) { ordersPend.removeAll(ordersToRemove); } //Once an order is ready, it can be deleted from pending list
		}
	}
	
	private class OrderPendingUpdate_PenaltiesCalculator extends OneShotBehaviour{
		
		public void action() {
			
			for(PendingOrder pendingOrder : ordersPend) {
				pendingOrder.setDaysLeftToAssemble(pendingOrder.getDaysLeftToAssemble() - 1);
				pendingOrder.getPendingOrder().setDueDate(pendingOrder.getPendingOrder().getDueDate() - 1);
				
				if(pendingOrder.getPendingOrder().getDueDate() < 0) {
					thisDayCostPenalties += pendingOrder.getPendingOrder().getPenalties();
				}
			}
		}
	}
	
	private class OrderRequestManufacturerProcessor extends Behaviour{
		
		private boolean allOrdersProcessed = false;
		private PredictedOrderInformation predictedOrderInformation = new PredictedOrderInformation();
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive();
			
			if(msg != null) {
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					
					if(ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						
						if(action instanceof ActionOrderTotal) {
							ActionOrderTotal orderCost = (ActionOrderTotal) action;

							PredicatePredictCost predictedOrderCost = new PredicatePredictCost();
							//store predicted order so if the order is accepted we don't need to redo the calculation
							predictedOrderInformation = calculateOrderInformation(orderCost.getOrder());
							predictedOrderCost.setCost(predictedOrderInformation.getMinimumPredictedCost());

							//sends predicted order cost to manufacture
							ACLMessage msgOrderCost = new ACLMessage(ACLMessage.INFORM);
								msgOrderCost.addReceiver(AIDmanu);
								msgOrderCost.setLanguage(codec.getName());
								msgOrderCost.setOntology(ontology.getName());
							
							try {
								getContentManager().fillContent(msgOrderCost, predictedOrderCost);
								myAgent.send(msgOrderCost);
							} 
							catch (CodecException codece) { codece.printStackTrace(); } 
							catch (OntologyException oe) { oe.printStackTrace(); }
							
						} 
						else if(action instanceof ActionAssemblePreperation) {
							
							//Adds order to pending list
							PendingOrder pendingOrder = new PendingOrder();
							pendingOrder.setOrder(predictedOrderInformation.getOrder());
							pendingOrder.setDaysLeftToAssemble(predictedOrderInformation.getPredictedAssemblytime());
							ordersPend.add(predictedOrderInformation.getPredictedPositionInPendingOrdersList(), pendingOrder);
							
							//storage cost minimised by making different supply orders on alternate days in the case that more than one supplier is being ordered from
							int quantity = predictedOrderInformation.getOrder().getQuantity();
							int time = predictedOrderInformation.getPredictedAssemblytime();
							
							for(Map.Entry<Supplier, ArrayList<ConceptComponent>> entry : predictedOrderInformation.getComponentsToOrderFromSuppliers().entrySet()) {
								SuppliesNeeded suppliesNeeded = new SuppliesNeeded(entry.getKey(), entry.getValue(), quantity, time);
								suppliesToOrder.add(suppliesNeeded);
							}
						}
					} 
					else if(ce instanceof PredicateNoMoreOrdersDay) { allOrdersProcessed = true; }
				}
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} 
			else { block(); }
		}
		public boolean done() { return allOrdersProcessed; }
	}
	
	private class SuppliesRequest extends OneShotBehaviour{
		
		public void action() {
			
			//Trying to lower warehouse expense by ensuring all supplies for an hour arrive on the same day to lower storage costs
			ArrayList<SuppliesNeeded> suppliesToRemove = new ArrayList<SuppliesNeeded>();
			
			for(SuppliesNeeded supplies : suppliesToOrder) {
				//Should supplies be delivered on thisday
				if(supplies.getTimeLeftToRequestDelivery() == supplies.getSupplier().getDeliveryTime()) {
					
					//Action Creation
					ActionSellSupplies sellSupplies = new ActionSellSupplies();
					ConceptSupplies s = new ConceptSupplies();
						s.setComponents(supplies.getComponents());
						s.setComponentsQuantity(supplies.getQuantityPerComponent());
						sellSupplies.setSupplies(s);

					//Wrapper Creation
					Action request = new Action();
						request.setAction(sellSupplies);
						request.setActor(supplies.getSupplier().getSupplierAID());

					//Request supplies from supplier
					ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						msg.addReceiver(supplies.getSupplier().getSupplierAID());
						msg.setLanguage(codec.getName());
						msg.setOntology(ontology.getName());
					
					try {

						getContentManager().fillContent(msg, request);
						myAgent.send(msg);
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }

					//adds cost of supplies to daily purchase counter
					for(ConceptComponent component : supplies.getComponents()) { thisDayCostSupplies = component.getCost() * supplies.getQuantityPerComponent(); }

					suppliesToRemove.add(supplies); //adds this supply to be removed list
				} 
				else { supplies.setTimeLeftToRequestDelivery(supplies.getTimeLeftToRequestDelivery() - 1); } //Lower request delivery time
			}
			// remove supplies
			suppliesToOrder.removeAll(suppliesToRemove);
		}
	}

	private class OrderReadySender extends OneShotBehaviour{
		public void action() {

			PredicateReadyOrders ordersToSend = new PredicateReadyOrders();
				ordersToSend.setOrders(ordersReady);

			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(AIDmanu);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
			
			try {
				getContentManager().fillContent(msg, ordersToSend);
				myAgent.send(msg);
			} 
			catch (CodecException ce) { ce.printStackTrace(); } 
			catch (OntologyException oe) { oe.printStackTrace(); }
			
			ordersReady.clear();
		}
	}
	
	private class StorageCosts extends OneShotBehaviour{
		public void action() { for(Map.Entry<ConceptComponent,Integer> entry : stock.entrySet()) { thisDayCostStorage = entry.getValue() * componentStorageCostThisDay; }}
	}
	
	private class WarehouseExpenses extends Behaviour{
		
		int step = 0;
		
		public void action() {
			
			switch(step) {
			//wait to recieve msg from manufacture
			case 0:
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null) {
					
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						if(ce instanceof PredicateWarehouseExpensesDay) { step++; }
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				} 
				else { block(); }
				break;

			//expenses from warehouse by day sent to manufacture
			case 1:
				PredicateWarehouseExpenses expenses = new PredicateWarehouseExpenses();
					expenses.setExpensePenalties(thisDayCostPenalties);
					expenses.setExpenseStorage(thisDayCostStorage);
					expenses.setExpenseSupplies(thisDayCostSupplies);

				//Sends expenses by msg
				ACLMessage response = new ACLMessage(ACLMessage.INFORM);
					response.addReceiver(AIDmanu);
					response.setLanguage(codec.getName());
					response.setOntology(ontology.getName());
				
				try {
					getContentManager().fillContent(response, expenses);
					myAgent.send(response);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
				step++;
				break;
			}
		}
		
		public boolean done() { return step == 2; }
	}
	
	private class Supplier{
		
		private List<ConceptComponent> componentsAvailable;
		private int deliveryTime;
		private AID supplierAID;
		
		public Supplier(List<ConceptComponent> componentsAvailable, int deliveryTime, AID supplierAID) {
			this.componentsAvailable = componentsAvailable;
			this.deliveryTime = deliveryTime;
			this.supplierAID = supplierAID;
		}
		
		public int getDeliveryTime() { return deliveryTime; }
		
		public AID getSupplierAID() { return supplierAID; }
		
		public ConceptComponent getComponentUsingProperty(ConceptComponent componentToFind) {
			
			for(ConceptComponent component : componentsAvailable) {
				
				if(componentToFind instanceof ConceptScreen && component instanceof ConceptScreen) {
					ConceptScreen screenToFind = (ConceptScreen) componentToFind;
					ConceptScreen screen = (ConceptScreen) component;
					
					if(screen.getSize().equals(screenToFind.getSize())) { return screen; }
				} 
				else if(componentToFind instanceof ConceptStorage && component instanceof ConceptStorage) {
					ConceptStorage storageToFind = (ConceptStorage) componentToFind;
					ConceptStorage storage = (ConceptStorage) component;
					
					if(storage.getStorageSize().equals(storageToFind.getStorageSize())) { return storage; }
				} 
				else if(componentToFind instanceof ConceptRAM && component instanceof ConceptRAM) {
					ConceptRAM ramToFind = (ConceptRAM) componentToFind;
					ConceptRAM ram = (ConceptRAM) component;
					
					if(ram.getRAMSize().equals(ramToFind.getRAMSize()))
					{ return ram; }
				} 
				else if(componentToFind instanceof ConceptBattery && component instanceof ConceptBattery) {
					ConceptBattery batteryToFind = (ConceptBattery) componentToFind;
					ConceptBattery battery = (ConceptBattery) component;
					
					if(battery.getBatterySize().equals(batteryToFind.getBatterySize())) { return battery; }
				}
			}
			return null;
		}
	}
	
	private class SuppliesNeeded{
		
		private Supplier supplier;
		private ArrayList<ConceptComponent> components;
		private int quantityPerComponent;
		private int timeLeftToRequestDelivery;
		
		public SuppliesNeeded(Supplier supplier, 
				ArrayList<ConceptComponent> components, 
				int quantityPerComponent, 
				int timeLeftToRequestDelivery) {
			this.supplier = supplier;
			this.components = components;
			this.quantityPerComponent = quantityPerComponent;
			this.timeLeftToRequestDelivery = timeLeftToRequestDelivery;
		}
		
		public Supplier getSupplier() { return supplier; }
		
		public ArrayList<ConceptComponent> getComponents(){ return components; }
		public int getQuantityPerComponent() { return quantityPerComponent; }
		
		public int getTimeLeftToRequestDelivery() { return timeLeftToRequestDelivery; }
		public void setTimeLeftToRequestDelivery(int time) { this.timeLeftToRequestDelivery = time; }
		
	}
	
	private class PendingOrder{
		private ConceptOrder order;
		private int daysLeftToAssemble;
		
		public void setOrder(ConceptOrder order) { this.order = order; }
		public ConceptOrder getPendingOrder() { return order; }
		
		public void setDaysLeftToAssemble(int daysLeftToAssemble) { this.daysLeftToAssemble = daysLeftToAssemble; }
		public int getDaysLeftToAssemble() { return daysLeftToAssemble; }
	}
	
	private class PredictedOrderInformation{
		private int minimumPredictedCost;
		private int assemblyTime;
		private int positionInPendingOrdersList;
		private HashMap<Supplier, ArrayList<ConceptComponent>> componentsToOrderFromSupplier;
		private ConceptOrder order;
		
		public int getMinimumPredictedCost() { return minimumPredictedCost; }
		public void setMinimumPredictedCost(int minimumPredictedCost) { this.minimumPredictedCost = minimumPredictedCost; }
		
		public int getPredictedAssemblytime() { return assemblyTime; }
		public void setPredictedAssemblyTime(int predictedAssemblyTime) { this.assemblyTime = predictedAssemblyTime; }
		
		public int getPredictedPositionInPendingOrdersList() { return positionInPendingOrdersList; }
		public void setPredictedPositionInPendingOrdersList(int predictedPositionInPendingOrdersList) { this.positionInPendingOrdersList = predictedPositionInPendingOrdersList; }
		
		public HashMap<Supplier, ArrayList<ConceptComponent>> getComponentsToOrderFromSuppliers(){ return componentsToOrderFromSupplier; }
		public void setComponentsToOrderFromSuppliers(HashMap<Supplier, ArrayList<ConceptComponent>> componentsToOrderFromSuppliers) { this.componentsToOrderFromSupplier = componentsToOrderFromSuppliers; }
		
		public ConceptOrder getOrder() { return order; }		
		public void setOrder(ConceptOrder order) { this.order = order; }
	}
	
	//Calculates 
	//minimum cost from an order
	//ready time
	//update order list
	//retrieves components from select suppliers
	private PredictedOrderInformation calculateOrderInformation(ConceptOrder order) {
		
		PredictedOrderInformation predictedOrderInformation = new PredictedOrderInformation();
		int minimumPredictedCost = 0;
		int predictedPositionInPendingOrdersList = ordersPend.size();
		
		//Component specs
		ArrayList<ConceptComponent> componentsToFind = new ArrayList<ConceptComponent>();
		componentsToFind.add(order.getSmartphone().getScreen());
		componentsToFind.add(order.getSmartphone().getStorage());
		componentsToFind.add(order.getSmartphone().getRam());
		componentsToFind.add(order.getSmartphone().getBattery());
		
		//Whole statement checks prices of each supplier to set the cheapest component available
		for(Supplier supplier : suppliers) {
			
			//prices from suppliers to update the tracked lowest prices
			int screenPrice = 0;
			int storagePrice = 0;
			int ramPrice = 0;
			int batteryPrice = 0;


			HashMap<Supplier, ArrayList<ConceptComponent>> predictedComponentsToOrderFromSupplier = new HashMap<Supplier, ArrayList<ConceptComponent>>();

			int predictedCost = 0;
			int deliveryTime = supplier.getDeliveryTime();
			
			//Predicted cost of total components
			for(ConceptComponent component : componentsToFind) {
				int componentPrice;
				Supplier selectedSupplier = null;
				
				//Supplier doesn't have the component cheaper than others
				if(supplier.getComponentUsingProperty(component) == null) {
					int cheapestAlternativeComponentPrice = 0;
					for(Supplier alternativeSupplier : suppliers) {
						
						//if the next supplier is just the same supplier, or doesn't have the requested component, skip
						if(alternativeSupplier == supplier) { continue; } 
						else if(alternativeSupplier.getComponentUsingProperty(component) != null) {
							int alternativeComponentPrice = alternativeSupplier.getComponentUsingProperty(component).getCost();
							
							//changes cheapest available
							if( cheapestAlternativeComponentPrice == 0 || alternativeComponentPrice < cheapestAlternativeComponentPrice) {
								cheapestAlternativeComponentPrice = alternativeComponentPrice;
								selectedSupplier = alternativeSupplier;
								
								// Update delivery time based on the supplier that will take longer to deliver a component
								if(deliveryTime < alternativeSupplier.deliveryTime) {
									deliveryTime = alternativeSupplier.deliveryTime;
								}
							}
						}
					}
					componentPrice = cheapestAlternativeComponentPrice;
				} 
				else {
					componentPrice = supplier.getComponentUsingProperty(component).getCost();
					selectedSupplier = supplier;
				}
				
				//update price
				if(component instanceof ConceptScreen) { screenPrice = componentPrice; } 
				else if(component instanceof ConceptStorage) { storagePrice = componentPrice; } 
				else if(component instanceof ConceptRAM) { ramPrice = componentPrice; } 
				else { batteryPrice = componentPrice; }
				
				//Update cost
				predictedCost += componentPrice * order.getQuantity();
				
				// Add supplier-component to the hashmap with all the suppliers and the components that should be bought from them
				if(predictedComponentsToOrderFromSupplier.containsKey(selectedSupplier)) {
					ArrayList<ConceptComponent> components = predictedComponentsToOrderFromSupplier.get(selectedSupplier);
					components.add(selectedSupplier.getComponentUsingProperty(component));
					predictedComponentsToOrderFromSupplier.replace(selectedSupplier, components);
				} else {
					ArrayList<ConceptComponent> components = new ArrayList<ConceptComponent>();
					components.add(selectedSupplier.getComponentUsingProperty(component));
					predictedComponentsToOrderFromSupplier.put(selectedSupplier, components);
				}
			}

			//Cycles through all pending orders to calculate when the new order can be created and what the total time would be
			int assemblyTime = deliveryTime;
			for(PendingOrder pendingOrder : ordersPend) {
				if(assemblyTime == pendingOrder.getDaysLeftToAssemble()) {
					
					int numbSmartphonesToBeAssembledThatDay = 0;
					
					//check for how many items are going to be created on current day
					int i = ordersPend.indexOf(pendingOrder);
					
					while(i < ordersPend.size() && ordersPend.get(i).getDaysLeftToAssemble() == pendingOrder.getDaysLeftToAssemble()) {
						numbSmartphonesToBeAssembledThatDay += ordersPend.get(i).getPendingOrder().getQuantity();
						i++;
					}
					
					//If the current order can be created on the current day, time isn't updated
					if(numbSmartphonesToBeAssembledThatDay + order.getQuantity() <= maxPhonesThisDay) {
						predictedPositionInPendingOrdersList = ordersPend.indexOf(pendingOrder);
						break;
					}
					else { assemblyTime++; }
					
				} //if the current order can be created before a diferent order with no effects, time isn't updated
				else if(assemblyTime < pendingOrder.getDaysLeftToAssemble()) {
					predictedPositionInPendingOrdersList = ordersPend.indexOf(pendingOrder);
					break;
				}
			}

			//penalties added based on assemble time
			if(assemblyTime > order.getDueDate()) { predictedCost += (assemblyTime - order.getDueDate()) * order.getPenalties(); }

			//If the newly predicted cost is lower than min, change that to the min
			if(minimumPredictedCost == 0 || predictedCost < minimumPredictedCost) {
				
				//Updates min cost
				minimumPredictedCost = predictedCost;
				
				//Updates component prices
				order.getSmartphone().getScreen().setCost(screenPrice);
				order.getSmartphone().getStorage().setCost(storagePrice);
				order.getSmartphone().getRam().setCost(ramPrice);
				order.getSmartphone().getBattery().setCost(batteryPrice);
				
				//Updates the predicted orders
				predictedOrderInformation.setOrder(order);
				predictedOrderInformation.setMinimumPredictedCost(minimumPredictedCost);
				predictedOrderInformation.setPredictedAssemblyTime(assemblyTime);
				predictedOrderInformation.setPredictedPositionInPendingOrdersList(predictedPositionInPendingOrdersList);
				predictedOrderInformation.setComponentsToOrderFromSuppliers(predictedComponentsToOrderFromSupplier);
			}
		}
		return predictedOrderInformation;
	}
}
