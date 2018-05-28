package behaviours;

import java.io.IOException;

import agents.AbstractAgent.Topic;
import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class SendStateMessageBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = 4758282837132294129L;

	public SendStateMessageBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		String topic = Topic.STATE.getValue();

		myAgent.addLogEntry("sending ' " + topic + "' message");

		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		msg.setSender(myAgent.getAID());
		msg.setConversationId(topic);

		try { msg.setContentObject(myAgent.getCurrentState()); } 
		catch (IOException e) { e.printStackTrace(); }

		for (AID agent:myAgent.getAgentsAID(null)) 
			msg.addReceiver(agent);

		myAgent.sendMessage(msg);
	}

	@Override
	public boolean done() {
		myAgent.trace(getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return 0;
	}

}
