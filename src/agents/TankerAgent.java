package agents;

public class TankerAgent extends AbstractAgent {

	private static final long serialVersionUID = 1458943879971399320L;

	protected void setup() {
		super.setup();

		// ~~~~~ Define FSM ~~~~~

		// ~~~ FSM STATES

		// ~~~ FSM TRANSITIONS

		addBehaviour(fsm);
	}

	@Override
	public Strategy computeStrategy() {
		if( dedale.isExplored() ){
			strategy = Strategy.RENDEZ_VOUS;
			addLogEntry("no room to explore");
		}else { 
			strategy = Strategy.EXPLORATION;
		}
		addLogEntry("Strategy : " + strategy);
		trace("STRATEGY", false);
		return strategy;
	}

	@Override
	public boolean isGoalReached() {
		return false;
	}
}
