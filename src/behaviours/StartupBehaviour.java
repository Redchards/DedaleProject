package behaviours;

import agents.AbstractAgent;
import jade.core.Agent;

public class StartupBehaviour extends AbstractFSMBehaviour {
	
	private static final long serialVersionUID = -7405725449841989659L;

	public StartupBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		myAgent.addLogEntry("sleeping for " + AbstractAgent.SLEEP_DURATION + " milliseconds");
		myAgent.trace(getBehaviourName(), false);
		myAgent.doWait(AbstractAgent.SLEEP_DURATION);

		myAgent.addLogEntry("waking up");
		myAgent.addLogEntry("Initializing");
		myAgent.start();
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
