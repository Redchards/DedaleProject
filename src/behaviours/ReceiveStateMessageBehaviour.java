package behaviours;

import agents.AbstractAgent.State;
import agents.AbstractAgent.Topic;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;

public class ReceiveStateMessageBehaviour extends AbstractReceiveMessageBehaviour {

	private static final long serialVersionUID = -3789924687792071790L;

	public ReceiveStateMessageBehaviour(Agent a) {
		super(a);
		topic 		= Topic.STATE;
		bUseTimeout = true;
	}

	@Override
	public void handleMessage(ACLMessage msg) {
		try { myAgent.addKnownNearAgents((State) msg.getContentObject()); }
		catch (UnreadableException e) { e.printStackTrace(); }
	}
}
