package behaviours;

import agents.AbstractAgent.Topic;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import knowledge.Dedale;

public class ReceiveDedaleMessageBehaviour extends AbstractReceiveMessageBehaviour {

	private static final long serialVersionUID = 2686415765419039163L;

	public ReceiveDedaleMessageBehaviour(Agent a) {
		super(a);
		topic 		= Topic.DEDALE;
		bUseTimeout = false;
	}

	@Override
	public void handleMessage(ACLMessage msg) {
		myAgent.needPathPlanning();
		try { myAgent.getDedale().integrateKnowledge((Dedale) msg.getContentObject()); }
		catch (UnreadableException e) { e.printStackTrace(); }
	}
}
