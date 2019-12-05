package supplychain;
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
import sc_ontology_concept.ConceptOrder;
import sc_ontology_predicate.*;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Acts as a manufacturer who takes supplies to create the item
*/

public class ManufacturerAgent extends Agent{
	
	private Codec codec = new SLCodec();
	private Ontology ontology = OntologySupplyChain.getInstance();
	
	private AID[] AIDcust;
	private AID AIDware;
	private AID AIDday;
	
	private int thisDayProfit;
	private int thisDayPurchases;
	private int thisDayPenalties;
	private int thisDayStorages;
	private int thisDayPayments;
	private int profitTotaled;
	
	private static final int minPM = 3; //profit margin
	
	//Agent Initialisation
	protected void setup() {
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		//Yellow Pages Registration
		DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
			sd.setType("Manufacturer");
			sd.setName(getLocalName() + "-manufacturer-agent");
			dfd.addServices(sd);
		
		try { DFService.register(this, dfd); } 
		catch(FIPAException e) { e.printStackTrace(); }

		profitTotaled = 0;
		thisDayProfit = 0;
		thisDayPurchases = 0;
		thisDayPenalties = 0;
		thisDayStorages = 0;
		thisDayPayments = 0;

		doWait(3000);
		this.addBehaviour(new LocateCustomers());
		this.addBehaviour(new LocateWarehouse());
		this.addBehaviour(new LocateDay());
		this.addBehaviour(new DayWaiter());
	}


	protected void takeDown() {

		System.out.println("Agent " + this.getLocalName() + " is terminating.");

		try { DFService.deregister(this); } 
		catch(FIPAException e) { e.printStackTrace(); }
	}
	
	private class LocateCustomers extends OneShotBehaviour{
		public void action() {
			DFAgentDescription customerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
				sd.setType("Customer");
				customerTemplate.addServices(sd);
			
			try {
				DFAgentDescription[] customerAgents = DFService.search(myAgent, customerTemplate);
				int size = customerAgents.length;
				AIDcust = new AID[size];
				
				for(int i = 0; i < size; i++) { AIDcust[i] = customerAgents[i].getName(); }
			} 
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}
	
	private class LocateWarehouse extends OneShotBehaviour{
		
		public void action() {
			DFAgentDescription warehouseTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
				sd.setType("Warehouse");
				warehouseTemplate.addServices(sd);
			
			try {
				AIDware = new AID();
				DFAgentDescription[] warehouseAgents = DFService.search(myAgent, warehouseTemplate);
				AIDware = warehouseAgents[0].getName();
			} 
			catch(FIPAException e) { e.printStackTrace(); }
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
	
	public class DayWaiter extends CyclicBehaviour{
		
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(AIDday));
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					
					//Methods carried out upon each new day by the manufacturer
					if(ce instanceof PredicateNewDay) {
						SequentialBehaviour dailyActivity = new SequentialBehaviour();
							dailyActivity.addSubBehaviour(new ProcessOrderBehaviour());
							dailyActivity.addSubBehaviour(new ProcessOrdersReadyBehaviour());
							dailyActivity.addSubBehaviour(new CalculateDailyProfitBehaviour());
							dailyActivity.addSubBehaviour(new EndDayBehaviour());
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
	
	private class ProcessOrderBehaviour extends Behaviour{
		int i = 0;
		int recievedOrderCounter = 0;
		ConceptOrder currentOrder;
		
		public void action() {
			
			switch(i) {
			
			//Sell order recieved from customer
			case 0:
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
				ACLMessage msg = myAgent.receive(mt);
				
				if(msg != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						if(ce instanceof Action) {
							Concept action = ((Action)ce).getAction();
							if(action instanceof ActionSell) {
								ActionSell sellOrder = (ActionSell) action;
								
								//Action Creation
								ActionOrderTotal actionOrderTotal = new ActionOrderTotal();
									actionOrderTotal.setOrder(sellOrder.getOrder());
								
								//Wrapper Creation
								Action request = new Action();
									request.setAction(actionOrderTotal);
									request.setActor(AIDware);
								
								//Warehouse is requested to calculate cost
								ACLMessage costRequestMsg = new ACLMessage(ACLMessage.REQUEST);
									costRequestMsg.addReceiver(AIDware);
									costRequestMsg.setLanguage(codec.getName());
									costRequestMsg.setOntology(ontology.getName());
								
								try {
									getContentManager().fillContent(costRequestMsg, request);
									myAgent.send(costRequestMsg);
								} 
								catch (CodecException codece) { codece.printStackTrace(); } 
								catch (OntologyException oe) { oe.printStackTrace(); }
								
								currentOrder = sellOrder.getOrder();
								i++;
								recievedOrderCounter++;
							}
						}
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				break;
			
			//Reciever for costs from warehouse
			case 1:
				MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg1 = myAgent.receive(mt1);
				
				if( msg1 != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg1);
						if(ce instanceof PredicatePredictCost) {
							PredicatePredictCost costs = (PredicatePredictCost) ce;

							//Benefit is generated from the costs of the order creation vs the sale price
							int thisCostSell = currentOrder.getThisCost() * currentOrder.getQuantity();
							int profit = thisCostSell - costs.getCost();

							//Decision of accepting or denying the order is based on the profit margins minimum value
							float profitMargin = ((float)profit / (float)thisCostSell) * 100.0f;
							if(profitMargin >= minPM) {
								
								//Action Creation
								ActionAssemblePreperation orderToAssemble = new ActionAssemblePreperation();
									orderToAssemble.setOrder(currentOrder);
								
								//Wrapper Creation
								Action request = new Action();
									request.setAction(orderToAssemble);
									request.setActor(AIDware);
								
								//Request sent to warehouse to prepare order assembly
								ACLMessage prepareOrderRequestMsg = new ACLMessage(ACLMessage.REQUEST);
									prepareOrderRequestMsg.addReceiver(AIDware);
									prepareOrderRequestMsg.setLanguage(codec.getName());
									prepareOrderRequestMsg.setOntology(ontology.getName());
								
								try {
									getContentManager().fillContent(prepareOrderRequestMsg, request);
									myAgent.send(prepareOrderRequestMsg);
								} 
								catch (CodecException codece) { codece.printStackTrace(); } 
								catch (OntologyException oe) { oe.printStackTrace(); }
							}
							//Check for any orders still to be recieved
							if(recievedOrderCounter == AIDcust.length) { i++; } 
							else { i = 0; }
						}
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				break;

			//Lets warehouse know no more orders are to be completed on current day
			case 2:
				PredicateNoMoreOrdersDay  noMoreOrdersToday = new PredicateNoMoreOrdersDay();
				
				ACLMessage noMoreOrdersInformMsg = new ACLMessage(ACLMessage.INFORM);
					noMoreOrdersInformMsg.addReceiver(AIDware);
					noMoreOrdersInformMsg.setLanguage(codec.getName());
					noMoreOrdersInformMsg.setOntology(ontology.getName());
				
				try {
					getContentManager().fillContent(noMoreOrdersInformMsg, noMoreOrdersToday);
					myAgent.send(noMoreOrdersInformMsg);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
				i++;
				break;
			}
		}
		public boolean done() { return i == 3; }
	}
	
	private class ProcessOrdersReadyBehaviour extends Behaviour{
		private int i = 0;
		private int deliveredReadyOrdersCounter = 0;
		private int recievedPaymentCounter = 0;
		
		public void action() {
			
			switch(i) {
			
			//Get ready orders
			case 0:
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg = myAgent.receive(mt);
				
				if(msg != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						
						if(ce instanceof PredicateReadyOrders) {
							PredicateReadyOrders ordersReady = (PredicateReadyOrders) ce;
							//Ready orders are sent to the relevant customer
							if(ordersReady.getOrders() == null || ordersReady.getOrders().isEmpty()) { i = 2; } 
							else
							{
								for(ConceptOrder orderReady : ordersReady.getOrders()) {
									PredicateDeliveredOrder orderDelivered = new PredicateDeliveredOrder();
										orderDelivered.setOrder(orderReady);

									//Let customer know order is ready
									ACLMessage orderDeliveredMsg = new ACLMessage(ACLMessage.INFORM);
										orderDeliveredMsg.addReceiver(orderReady.getAID());
										orderDeliveredMsg.setLanguage(codec.getName());
										orderDeliveredMsg.setOntology(ontology.getName());
									
									try {
										getContentManager().fillContent(orderDeliveredMsg, orderDelivered);
										myAgent.send(orderDeliveredMsg);
									} 
									catch (CodecException codece) { codece.printStackTrace(); } 
									catch (OntologyException oe) { oe.printStackTrace(); }
								}
								deliveredReadyOrdersCounter = ordersReady.getOrders().size();
								i = 1;
							}
						} 
						else { myAgent.postMessage(msg); }
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				} 
				else { block(); }
				break;
				
			//Recieve customer payment
			case 1:
				MessageTemplate mt1 = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg1 = myAgent.receive(mt1);
				
				if(msg1 != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg1);
						
						if(ce instanceof PredicatePayment) {
							//Add payment
							PredicatePayment payment = (PredicatePayment) ce;
							thisDayPayments += payment.getTotal();
							recievedPaymentCounter++;

							//After payments are recieved, i is incremented
							if(deliveredReadyOrdersCounter == recievedPaymentCounter) {
								i++;
							}
						}
					}
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				} 
				else { block(); }
				break;
			}
		}
		public boolean done() { return i == 2; }
	}
	
	private class CalculateDailyProfitBehaviour extends Behaviour{
		private int i = 0;
		
		public void action() {
			
			switch(i) {
			//Asks warehouse agent for daily total costs
			case 0:
				PredicateWarehouseExpensesDay warehouseExpensesToday = new PredicateWarehouseExpensesDay();
				
				ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
					msg.addReceiver(AIDware);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
				
				try {
					getContentManager().fillContent(msg, warehouseExpensesToday);
					myAgent.send(msg);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
				
				i++;
				break;
			
			//Warehouse costs reciever
			case 1:
				MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
				ACLMessage msg1 = myAgent.receive(mt);
				if(msg1 != null) {
					
					try {
						ContentElement ce = getContentManager().extractContent(msg1);
						if(ce instanceof PredicateWarehouseExpenses) {
							//Daily Storage Costs
							PredicateWarehouseExpenses expenses = (PredicateWarehouseExpenses) ce;
							thisDayPurchases = expenses.getExpenseSupplies();
							thisDayPenalties = expenses.getExpensePenalties();
							thisDayStorages = expenses.getExpenseStorage();
							i++;
						}
					} 
					catch (CodecException codece) { codece.printStackTrace(); }
					catch (OntologyException oe) { oe.printStackTrace(); }
				}
				break;

			//Daily profit calculation
			case 2:
				thisDayProfit = thisDayPayments - thisDayPenalties - thisDayStorages - thisDayPurchases;
				profitTotaled += thisDayProfit;
				
				System.out.println("Daily profit of: " + thisDayProfit);
				System.out.println("Total profit of: " + profitTotaled);
				
				i++;
				break;
			}
		}
		public boolean done() { return i == 3; }
	}


	private class EndDayBehaviour extends OneShotBehaviour{
		public void action() {

			thisDayProfit = 0;
			thisDayPurchases = 0;
			thisDayPenalties = 0;
			thisDayStorages = 0;
			thisDayPayments = 0;

			PredicateEndDay dayEnd = new PredicateEndDay();

			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(AIDday);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
			
			try {
				getContentManager().fillContent(msg, dayEnd);
				myAgent.send(msg);
			} 
			catch (CodecException codece) { codece.printStackTrace(); } 
			catch (OntologyException oe) { oe.printStackTrace(); }
		}
	}
}
