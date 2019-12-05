package supplychain;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import sc_ontology.OntologySupplyChain;

import sc_ontology_predicate.PredicateNewDay;
import sc_ontology_predicate.PredicateEndDay;
import sc_ontology_predicate.PredicateEndRun;

/*
*	40272321
*	Connor Ness
*	Multi-Agent System Coursework
*	Tracks day cycles and used to end the simulation on max days
*/

public class DayCoordAgent extends Agent{
	
	private Codec codec = new SLCodec();
	private Ontology ontology = OntologySupplyChain.getInstance();
	
	private AID AIDmanu;
	private AID AIDware;
	private AID[] AIDcust;
	private AID[] AIDsupp;
	
	private static final int DayCounter = 100; //Change this value for total day cycles

	protected void setup() {
		
		getContentManager().registerLanguage(codec);
		getContentManager().registerOntology(ontology);
		
		// Register agent into the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
		
			ServiceDescription sd = new ServiceDescription();
			sd.setType("DayCoordinator");
			sd.setName(getLocalName() + "-dayCoordinator-agent");
			dfd.addServices(sd);
		
		try { DFService.register(this, dfd); } 
		catch(FIPAException e) { e.printStackTrace(); }

		doWait(3000);
		this.addBehaviour(new LocateManufactureAgent());
		this.addBehaviour(new LocateWarehouseAgent());
		this.addBehaviour(new LocateCustomerAgent());
		this.addBehaviour(new LocateSupplierAgent());
		this.addBehaviour(new SynchroniseAgents());
	}


	protected void takeDown() {

		System.out.println("Agent " + this.getLocalName() + " is terminating.");

		//Remove from yellow pages
		try { DFService.deregister(this); } 
		catch(FIPAException e) { e.printStackTrace(); }
	}


	private class LocateManufactureAgent extends OneShotBehaviour{
		
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


	private class LocateWarehouseAgent extends OneShotBehaviour{
		
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


	private class LocateCustomerAgent extends OneShotBehaviour{
		
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
	
	
	private class LocateSupplierAgent extends OneShotBehaviour{
		
		public void action() {
			DFAgentDescription supplierTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
				sd.setType("Supplier");
				supplierTemplate.addServices(sd);
			
			try {
				DFAgentDescription[] supplierAgents = DFService.search(myAgent, supplierTemplate);
				
				int size = supplierAgents.length;
				AIDsupp = new AID[size];
				
				for(int i = 0; i < size; i++) { AIDsupp[i] = supplierAgents[i].getName(); }
			} 
			catch(FIPAException e) { e.printStackTrace(); }
		}
	}
	
	public class SynchroniseAgents extends Behaviour{
		
		private int step = 0;
		private int thisDay = 1;
		private int counterDay = 1;
		
		@Override
		public void action() {
			switch(step) {
			
			case 0:
				PredicateNewDay newDay = new PredicateNewDay();
				newDay.setDayNumber(thisDay);

				//Notify each agent that a new day has begun
				ACLMessage msgNewDay = new ACLMessage(ACLMessage.INFORM);
					msgNewDay.setLanguage(codec.getName());
					msgNewDay.setOntology(ontology.getName());
					msgNewDay.addReceiver(AIDmanu);
					msgNewDay.addReceiver(AIDware);
				
				for(AID aid : AIDcust) { msgNewDay.addReceiver(aid); }
				for(AID aid : AIDsupp) { msgNewDay.addReceiver(aid); }
				
				try {
					getContentManager().fillContent(msgNewDay, newDay);
					myAgent.send(msgNewDay);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); }
				
				step++;
				thisDay++;
				break;
			
			case 1:
				//Waiting for manufacturer message
				MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchSender(AIDmanu));
				ACLMessage msg = myAgent.receive(mt);
				
				if(msg != null) {
					try {
						ContentElement ce = getContentManager().extractContent(msg);
						if(ce instanceof PredicateEndDay) {
							System.out.println("End of day: " + thisDay);
							step++;
						}
					} 
					catch (CodecException ce) { ce.printStackTrace(); } 
					catch (OntologyException oe) { oe.printStackTrace(); }
				} else { block(); }
				break;
			}
		}
		
		@Override
		public boolean done() { return step == 2; }
		
		@Override
		public void reset() {
			super.reset();
			step = 0;
		}
		
		@Override
		public int onEnd() {
			if(thisDay == DayCounter) {
				PredicateEndRun endSimulation = new PredicateEndRun();

				//Notifying agents that simulation has ended
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setLanguage(codec.getName());
					msg.setOntology(ontology.getName());
					msg.addReceiver(AIDmanu);
					msg.addReceiver(AIDware);
				
				for(AID aid : AIDcust) { msg.addReceiver(aid); }
				for(AID aid : AIDsupp) { msg.addReceiver(aid); }
				
				try {
					getContentManager().fillContent(msg, endSimulation);
					myAgent.send(msg);
				} 
				catch (CodecException ce) { ce.printStackTrace(); } 
				catch (OntologyException oe) { oe.printStackTrace(); } 
				myAgent.doDelete();
			} else {
				reset();
				myAgent.addBehaviour(this);
			}
			return 0;
		}
	}
}
