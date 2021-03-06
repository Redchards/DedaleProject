package behaviours;

import agents.AbstractAgent;
import agents.AbstractAgent.Strategy;
import jade.core.Agent;

public class MoveBehaviour extends AbstractFSMBehaviour {

	private static final long serialVersionUID = -66338063934978642L;

	public enum Outcome{
		NONE(-1),
		FAILED(0),
		SUCCESS(1),
		IDLE(2),
		DEADLOCK(3),
		RESET(4);

		private int value;
		private Outcome(int value) { this.value = value; }
		public int getValue() { return this.value; }
	}
	
	private int 	failedCounter;
	private Outcome outcome;

	public MoveBehaviour(Agent a) {
		super(a);
		outcome = Outcome.NONE;
		failedCounter = 0;
	}

	@Override
	public void action() {
		outcome = Outcome.NONE;
		
		String plannedMove = myAgent.getNextMove();

		if(myAgent.getCurrentStrategy() == Strategy.IDLE || plannedMove.isEmpty()) {
			myAgent.addLogEntry("idle");
			if(myAgent.getSendingMapToAgents().isEmpty())
				outcome = Outcome.IDLE; 
			else
				outcome = Outcome.DEADLOCK;
			return;
		}

		boolean bHasMoved 	= false;
		int		failedMove 	= 0;
		
		myAgent.addLogEntry("trying to move from " + myAgent.getCurrentRoom() + " to " + plannedMove);
		while(outcome == Outcome.NONE) {
			if(failedMove >= AbstractAgent.FAILED_MOVE_MAX_ATTEMPT) {
				failedCounter++;
				myAgent.addLogEntry("failed too many times");
				outcome = Outcome.FAILED;
			}else {
				bHasMoved = myAgent.moveTo(plannedMove);
				if(!bHasMoved) {
					failedMove += 1;
					myAgent.addLogEntry("failed");
					block(AbstractAgent.TIMEOUT_BTW_FAILED_MOVE);
				}else {
					failedCounter = 0;
					myAgent.addLogEntry("success");
					myAgent.movedTo(plannedMove);
					outcome = Outcome.SUCCESS;
				}
			}
		}

	}

	@Override
	public boolean done() {
		if(failedCounter >= AbstractAgent.FAILED_MOVE_BEHAVIOUR_MAX_ATTEMPT && outcome != Outcome.SUCCESS && outcome != Outcome.IDLE) {
			failedCounter = 0;
			outcome = Outcome.RESET;
		}
		myAgent.trace(getBehaviourName(), false);
		return true;
	}

	@Override
	public int onEnd() {
		return outcome.getValue();
	}

}
