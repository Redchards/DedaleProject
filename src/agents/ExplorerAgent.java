package agents;

public class ExplorerAgent extends AbstractAgent {

	private static final long serialVersionUID = -4930667920457528901L;
	
	protected void setup() {
		super.setup();

		// ~~~~~ Define FSM ~~~~~

		// ~~~ FSM STATES

		// ~~~ FSM TRANSITIONS

		addBehaviour(fsm);
	}

	@Override
	public Strategy computeStrategy() {
		if( isGoalReached() ){
			strategy = Strategy.RANDOM_WALK;
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
		if( dedale.isExplored() ){
			if(bIsGoalReached == false) goalReached();
			return true;
		}
		return false;
	}
}
