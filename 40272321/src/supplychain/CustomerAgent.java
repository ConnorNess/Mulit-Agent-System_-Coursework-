package supplychain;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import sc_ontology.OntologySupplyChain;

import sc_ontology_concept.ConceptOrder;
import sc_ontology_concept.ConceptSmartphone;
import sc_ontology_concept.ConceptSmall;
import sc_ontology_concept.ConceptPhablet;
import sc_ontology_concept.ConceptRAM;
import sc_ontology_concept.ConceptStorage;

import sc_ontology_predicate.PredicateNewDay;
import sc_ontology_predicate.PredicateDeliveredOrder;
import sc_ontology_predicate.PredicatePayment;

import sc_ontology_action.ActionSell;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Acts as a representative of a customer in a supply chain during the simulation 
*/

public class CustomerAgent extends Agent{
	
	private Codec codec = new SLCodec();
	private Ontology ontology = OntologySupplyChain.getInstance();
	private AID AIDmanu;
	private AID AIDday;
	
	//Initialisation
	protected void setup() {
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		//Yellow Pages
		DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			
		ServiceDescription sd = new ServiceDescription();
			sd.setType("Customer");
			sd.setName(getLocalName() + "-customer-agent");
			dfd.addServices(sd);
		
		try { DFService.register(this, dfd); } 
		catch(FIPAException e) { e.printStackTrace(); }
		
		doWait(3000);
			this.addBehaviour(new LocateManufactererAgent());
			this.addBehaviour(new LocateDayAgent());
			this.addBehaviour(new DayWaiterAgent());
			this.addBehaviour(new RecieveOrder());
	}
	
	protected void takeDown() {
		System.out.println("Agent " + this.getLocalName() + " is terminating.");
		
		//Remove from Yellow Pages
		try { DFService.deregister(this); } 
		catch(FIPAException e) { e.printStackTrace(); }
	}
	
	//Find manufacturer
	private class LocateManufactererAgent extends OneShotBehaviour{
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
	
	//Find daycoord
	private class LocateDayAgent extends OneShotBehaviour{
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
	
	public class DayWaiterAgent extends CyclicBehaviour{
		public void action() {
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(AIDday));
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					
					//New request is made by customer on every new day
					if(ce instanceof PredicateNewDay) { myAgent.addBehaviour(new OrderRequest()); } //Adds behaviour
					else { myAgent.doDelete(); } //Checking if the predicate is a new day, if not - the simulation is ended
					
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} else {
				block();
			}
		}
	}
	
	public class OrderRequest extends OneShotBehaviour{
		
		public void action() {
			
			//Order Creation
			ConceptOrder order = createOrder(myAgent.getAID());
			
			//Action Creation
			ActionSell sellOrder = new ActionSell();
				sellOrder.setOrder(order);
			
			//Wrapper Creation
			Action request = new Action();
				request.setAction(sellOrder);
				request.setActor(AIDmanu);
			
			//Request manufacture
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				msg.addReceiver(AIDmanu);
				msg.setLanguage(codec.getName());
				msg.setOntology(ontology.getName());
			
			try {
				getContentManager().fillContent(msg, request);
				myAgent.send(msg);
			} 
			catch (CodecException ce) { ce.printStackTrace(); } 
			catch (OntologyException oe) { oe.printStackTrace(); } 
		}
		
	}
	
	public class RecieveOrder extends CyclicBehaviour{
		
		public void action() {
			
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(AIDmanu));
			ACLMessage msg = myAgent.receive(mt);
			
			if(msg != null) {
				
				try {
					ContentElement ce = getContentManager().extractContent(msg);
					if(ce instanceof PredicateDeliveredOrder) {
						PredicateDeliveredOrder orderDelivered = (PredicateDeliveredOrder) ce;
						
						//Generate the Payment
						PredicatePayment payment = new PredicatePayment();
							payment.setTotal(orderDelivered.getOrder().getQuantity() * orderDelivered.getOrder().getThisCost());
						
						//Send
						ACLMessage paymentMsg = new ACLMessage(ACLMessage.INFORM);
							paymentMsg.addReceiver(msg.getSender());
							paymentMsg.setLanguage(codec.getName());
							paymentMsg.setOntology(ontology.getName());
						
						try { //Converting obj to string
							getContentManager().fillContent(paymentMsg, payment);
							myAgent.send(paymentMsg);
						} 
						catch (CodecException codece) { codece.printStackTrace(); } 
						catch (OntologyException oe) { oe.printStackTrace(); }
					}
					
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
			} else { block(); }
		}
	}
	
	private ConceptOrder createOrder(AID myAgent) {
		ConceptSmartphone smartphone;
		
		//Rand fucntion for choosing smartphone type
		if(Math.random() < 0.5){ smartphone = new ConceptSmall(); } 
		else { smartphone = new ConceptPhablet(); }
		
		//Rand fucntion for choosing RAM type
		ConceptRAM ram = new ConceptRAM();
		if(Math.random() < 0.5) { ram.setRAMSize("4"); }
		else { ram.setRAMSize("8"); }
			smartphone.setRam(ram);
		
		//Rand fucntion for choosing Storage type
		ConceptStorage storage = new ConceptStorage();
		if(Math.random() < 0.5) { storage.setStorageSize("64"); }
		else { storage.setStorageSize("256"); }
			smartphone.setStorage(storage);
		
		//Order Creation
		ConceptOrder order = new ConceptOrder();
			order.setAID(myAgent);
			order.setSmartphone(smartphone);
			order.setQuantity((int)Math.floor(1 + 50 * Math.random()));
			order.setThisCost((int)Math.floor(100 + 500 * Math.random()));
			order.setDueDate((int)Math.floor(1 + 10 * Math.random()));
			order.setPenalties(order.getQuantity() * (int)Math.floor(1 + 50 * Math.random()));
		return order;
	}
}
