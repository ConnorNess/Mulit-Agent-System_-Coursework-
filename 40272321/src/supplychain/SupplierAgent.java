package supplychain;
import java.util.ArrayList;
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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import sc_ontology.OntologySupplyChain;
import sc_ontology_action.ActionSellSupplies;
import sc_ontology_concept.*;
import sc_ontology_predicate.*;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Acts as a supplier of materials
*/

public class SupplierAgent extends Agent{
	
	private Codec codec = new SLCodec();
	private Ontology ontology = OntologySupplyChain.getInstance();
	private AID AIDware;
	private AID AIDday;
	private PredicateSupplierInformation suppInfo;
	private ArrayList<SuppliesToDeliver> deliveriesPend;

	protected void setup() {
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);

		//Yellow bois time
		DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
		
		ServiceDescription sd = new ServiceDescription();
			sd.setType("Supplier");
			sd.setName(getLocalName() + "-supplier-agent");
			dfd.addServices(sd);
		
		try { DFService.register(this, dfd); } 
		catch(FIPAException e) { e.printStackTrace(); }

		Object[] args = this.getArguments();
		suppInfo = createSupplierInformation((String[])args[0], (int[])args[1], 
				(String[])args[2], (int[])args[3], 
				(String[])args[4], (int[])args[5], 
				(String[])args[6], (int[])args[7], 
				(int)args[8]);


		deliveriesPend = new ArrayList<SuppliesToDeliver>();

		//Wait for other agents then add behaviours
		doWait(3000);
		this.addBehaviour(new LocateWarehouse());
		this.addBehaviour(new LocateDay());
		this.addBehaviour(new DayWaiter());
		this.addBehaviour(new GetDetails());
		this.addBehaviour(new SupplyProcessor());
	}


	protected void takeDown() {
		System.out.println("Agent " + this.getLocalName() + " is terminating.");

		//Remove from yellows
		try { DFService.deregister(this); } 
		catch(FIPAException e) { e.printStackTrace(); }
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

					//Checks for new supplies to be sent to warehouse on each new day
					if(ce instanceof PredicateNewDay) {

						myAgent.addBehaviour(new SendSuppliesBehaviour());
					} 
					else { myAgent.doDelete(); } //If its not a new day, the simulation is finished
					
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} 
			else { block(); }
		}
	}
	
	public class GetDetails extends Behaviour{
		
		private boolean detailsSent = false;
		
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				try {

					ContentElement ce =  getContentManager().extractContent(msg);
					if(ce instanceof PredicateSupplierDetails) {

						ACLMessage response = new ACLMessage(ACLMessage.INFORM);
							response.addReceiver(AIDware);
							response.setLanguage(codec.getName());
							response.setOntology(ontology.getName());
						
						try {
							getContentManager().fillContent(response, suppInfo);
							myAgent.send(response);
						} 
						catch (CodecException codece) { codece.printStackTrace(); } 
						catch (OntologyException oe) { oe.printStackTrace(); }
						detailsSent = true;
					}
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} 
			else { block(); }
		}
		public boolean done() { return detailsSent; }
	}
	
	public class SupplyProcessor extends CyclicBehaviour{
		
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					if(ce instanceof Action) {
						Concept action = ((Action)ce).getAction();
						if(action instanceof ActionSellSupplies) {
							ActionSellSupplies sellSupplies = (ActionSellSupplies) action;
							
							SuppliesToDeliver suppliesToDeliver = new SuppliesToDeliver();
								suppliesToDeliver.setDeliveryDayTrackTimer(suppInfo.getTime());
								suppliesToDeliver.setSupplies(sellSupplies.getSupplies());
								deliveriesPend.add(suppliesToDeliver);
						}
					}
				} 
				catch (CodecException codece) { codece.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			}
		}
	}


	public class SendSuppliesBehaviour extends OneShotBehaviour{
		
		public void action() {
			ArrayList<SuppliesToDeliver> deliveriesToRemove = new ArrayList<SuppliesToDeliver>();
			
			for(SuppliesToDeliver delivery : deliveriesPend) {
				int days = delivery.getDeliveryDayTrackTimer();
				days--;
				
				if(days == 0) {
					PredicateSuppliesDelivered suppliesDelivered = new PredicateSuppliesDelivered();
						suppliesDelivered.setSupplies(delivery.getSupplies());

					//Send supplies
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.addReceiver(AIDware);
						msg.setLanguage(codec.getName());
						msg.setOntology(ontology.getName());
					
					try {
						getContentManager().fillContent(msg, suppliesDelivered);
						myAgent.send(msg);
					} 
					catch (CodecException codece) { codece.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }

					//adds this delivery to the list
					deliveriesToRemove.add(delivery);
				} else {
					// Update days left to deliver
					delivery.setDeliveryDayTrackTimer(days);
				}
			}

			deliveriesPend.removeAll(deliveriesToRemove);
			PredicateNoMoreSuppliesDay noMoreSuppliesToday = new PredicateNoMoreSuppliesDay();

			//Notify warehouse of no more supplies
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(AIDware);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
			
			try {
				getContentManager().fillContent(msg, noMoreSuppliesToday);
				myAgent.send(msg);
			} 
			catch (CodecException codece) { codece.printStackTrace(); } 
			catch (OntologyException oe) { oe.printStackTrace(); }
		}
	}
	
	
	private class SuppliesToDeliver{
		
		private ConceptSupplies supplies;
		private int deliveryDayTrackTimer;
		
		public void setSupplies(ConceptSupplies supplies) { this.supplies = supplies; }
		public ConceptSupplies getSupplies() { return supplies; }
		
		public void setDeliveryDayTrackTimer(int deliveryDayTrackTimer) { this.deliveryDayTrackTimer = deliveryDayTrackTimer; }
		public int getDeliveryDayTrackTimer() { return deliveryDayTrackTimer; }
		
	}


	//pulls supplier information, components
	private PredicateSupplierInformation createSupplierInformation(String[] screenSize, int[] screenSizePrice, 
			String[] storageSize, int[] storageSizePrice,
			String[] ramSize, int[] ramSizePrice, 
			String[] batterySize, int[] batterySizePrice, 
			int deliveryTime) {
		
		PredicateSupplierInformation suppInfo = new PredicateSupplierInformation();
		ArrayList<ConceptComponent> components = new ArrayList<ConceptComponent>();

		for(int i = 0; i < screenSize.length; i++) {
			ConceptScreen screen = new ConceptScreen();
			screen.setSize(screenSize[i]);
			screen.setCost(screenSizePrice[i]);
			components.add(screen);
		}

		for(int i = 0; i < storageSize.length; i++) {
			ConceptStorage storage = new ConceptStorage();
			storage.setStorageSize(storageSize[i]);
			storage.setCost(storageSizePrice[i]);
			components.add(storage);
		}

		for(int i = 0; i < ramSize.length; i++) {
			ConceptRAM ram = new ConceptRAM();
			ram.setRAMSize(ramSize[i]);
			ram.setCost(ramSizePrice[i]);
			components.add(ram);
		}

		for(int i = 0; i < batterySize.length; i++) {
			ConceptBattery battery = new ConceptBattery();
			battery.setBatterySize(batterySize[i]);
			battery.setCost(batterySizePrice[i]);
			components.add(battery);
		}
		suppInfo.setComponents(components);
		suppInfo.setTime(deliveryTime);
		return suppInfo;
	}
}
