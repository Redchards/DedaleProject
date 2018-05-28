package behaviours;

import jade.core.Agent;

public class TerminateBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -580387440966790284L;

	public TerminateBehaviour(Agent a) {
		super(a);
	}

	@Override
	public void action() {
		this.myAgent.addLogEntry("my goal on this world has been achieved !");
		this.myAgent.addLogEntry("Launching termination procedure");
		this.myAgent.addLogEntry("I'll be back ...");
		
//		this.myAgent.doDelete();
	}

	@Override
	public boolean done() {
		this.myAgent.trace(this.getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return 0;
	}

}
