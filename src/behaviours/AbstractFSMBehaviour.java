package behaviours;

import agents.AbstractAgent;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public abstract class AbstractFSMBehaviour extends SimpleBehaviour {

	private static final long serialVersionUID = -933252582709302922L;
	
	protected AbstractAgent myAgent;

	public AbstractFSMBehaviour(Agent a) {
		super(a);
		this.myAgent = (AbstractAgent) a;
	}

	public abstract void action();

	public abstract boolean done();
	
	public abstract int onEnd();
}
