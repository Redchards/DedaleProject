package behaviours;

import agents.AbstractAgent.Topic;
import env.EntityType;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class ReceiveHelloMessageBehaviour extends AbstractReceiveMessageBehaviour {

	private static final long serialVersionUID = 6530845233035635879L;

	public ReceiveHelloMessageBehaviour(Agent a) {
		super(a);
		topic 		= Topic.HELLO;
		bUseTimeout = true;
	}

	@Override
	public void handleMessage(ACLMessage msg) {
		String 	name	= msg.getSender().getLocalName();
		String 	content = msg.getContent();
		String 	pos 	= content.split(";")[1];

		this.myAgent.addLogEntry("        He is at " + pos);
		this.myAgent.addLogEntry("        Adding him to the list of agents I have to send a map");
		this.myAgent.addSendingMapToAgents(msg.getSender());

		if(content.contains(EntityType.AGENT_TANKER.toString())) this.myAgent.addKnownNearTankers(name, pos);
	}
}
