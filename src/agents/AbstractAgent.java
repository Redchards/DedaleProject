package agents;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.RendererType;
import org.graphstream.stream.file.FileSinkImages.Resolutions;

import behaviours.AbstractReceiveMessageBehaviour;
import behaviours.DeadlockBehaviour;
import behaviours.MoveBehaviour;
import behaviours.ObservationBehaviour;
import behaviours.PlanningBehaviour;
import behaviours.ReceiveDedaleMessageBehaviour;
import behaviours.ReceiveHelloMessageBehaviour;
import behaviours.ReceiveStateMessageBehaviour;
import behaviours.SendDedaleMessageBehaviour;
import behaviours.SendHelloMessageBehaviour;
import behaviours.SendStateMessageBehaviour;
import behaviours.StartupBehaviour;
import behaviours.TerminateBehaviour;
import env.EntityType;
import env.Environment;
import jade.core.AID;
import jade.core.behaviours.FSMBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import knowledge.Dedale;
import knowledge.Dedale.TreasureType;
import mas.abstractAgent;

public abstract class AbstractAgent extends abstractAgent {

	private static final long serialVersionUID = -9055009607200235815L;

	// ===================
	// ===== CONSTANTS =====
	// ===================
	
	public static final int 	FAILED_DEADLOCK_SOLVING_MAX_ATTEMPT = 3;		// Nombre de fois que le behaviour deadlock peut ne mener à aucun résultat avant un reset
	public static final int 	FAILED_MOVE_BEHAVIOUR_MAX_ATTEMPT 	= 3;		// Nombre de fois que le behaviour move peut ne mener à aucun résultat avant un reset
	public static final int 	FAILED_MOVE_MAX_ATTEMPT 			= 4;		// Nombre de fois qu'un movement peut échouer avant passer à une résolution d'interblocage

	public static final long 	SLEEP_DURATION 						= 1000;
	public static final long 	CYCLE_SPEED 						= 500;

//	public static final long 	TIMEOUT_BTW_FAILED_MOVE 			= 10;
	public static final long 	TIMEOUT_BTW_FAILED_MOVE 			= CYCLE_SPEED / FAILED_MOVE_MAX_ATTEMPT ;

//	public static final long 	WAIT_FOR_MESSAGE_DURATION 			= 50 ;
//	public static final long 	MESSAGE_TIMEOUT 					= 150;

	public static final long 	WAIT_FOR_MESSAGE_DURATION 			= CYCLE_SPEED / 3 ;
	public static final long 	MESSAGE_TIMEOUT 					= CYCLE_SPEED * 3;

	public static final int 	BLOCKED_ROOM_TIMEOUT 				= 5;		// Nombre de cycle avant de débloquer une salle

	public static final int 	COLLECTOR_BASE_PRIORITY 			= 100;
	public static final int 	EXPLORATOR_BASE_PRIORITY 			= 10;
	public static final int 	TANKER_BASE_PRIORITY 				= 1;
	
	// State names
	protected static final String STATE_STARTUP 		= "STATE_STARTUP";
	protected static final String STATE_OBSERVE 		= "STATE_OBSERVE";
	protected static final String STATE_SCAVENGE 		= "STATE_SCAVENGE";
	protected static final String STATE_EMPTY_BACKPACK 	= "STATE_EMPTY_BACKPACK";
	protected static final String STATE_SEND_HELLO 		= "STATE_SEND_HELLO";
	protected static final String STATE_SEND_DEDALE 	= "STATE_SEND_DEDALE";
	protected static final String STATE_SEND_STATE	 	= "STATE_SEND_STATE";
	protected static final String STATE_SEND_BLOCKED 	= "STATE_SEND_BLOCKED";
	protected static final String STATE_RECEIVE_HELLO	= "STATE_RECEIVE_HELLO";
	protected static final String STATE_RECEIVE_DEDALE	= "STATE_RECEIVE_DEDALE";
	protected static final String STATE_RECEIVE_BLOCKED = "STATE_RECEIVE_BLOCKED";
	protected static final String STATE_RECEIVE_STATE 	= "STATE_RECEIVE_STATE";
	protected static final String STATE_PLANNING 		= "STATE_PLANNING";
	protected static final String STATE_MOVE 			= "STATE_MOVE";
	protected static final String STATE_DEADLOCK 		= "STATE_DEADLOCK";
	protected static final String STATE_TERMINATE 		= "STATE_TERMINATE";
	
	// ===================
	// ===== MEMBERS =====
	// ===================
	
	// --- Description ---
	protected int 				priority;   		// Priorité de l'agent face aux autres
	protected int				backpackCapacity;
	protected EntityType		type;				// Type de l'agent (Explorateur, collecteur, silo)
	protected Strategy 			strategy;  			// Stratégie de décision
	protected DeadlockState 	deadlockState;  	// Etat de l'agent si il est interbloqué
	protected FSMBehaviour 		fsm; 
	protected boolean 			bIsGoalReached; 
	
	// --- Long-lived knowledge ---
	protected Dedale 	  		dedale;				// Représentation du monde de l'agent selon ses connaissances

	protected Stack<String> 	plannedMoves;		// Chemin planifié	

	protected Set<AID>			explorerAgents;	
	protected Set<AID>			collectorAgents;
	protected Set<AID>			tankerAgents;

	// --- Short-lived knowledge ---
	protected Set<AID> 				sendingMapToAgents;		
	protected Map<String, State> 	knownNearAgents;		
	protected Map<String, String> 	knownNearTankers;		
	protected Map<Topic, Integer> 	messageReceived;	// Nombre de fois d'un message de type conv_id a été reçu
	protected Map<String, Integer> 	blockedRooms;		// Nombre de fois d'un message de type conv_id a été reçu
	protected String 				objective;			// Destination temporaire (pour rencontrer quelqu'un par exemple)

	
	// --- Misc ---
	
	// ~ Log
	protected boolean		bTrace;
	protected String		logFileName;
	protected List<String>	logEntries;
	
	// ~ Statistics
	protected long			startingTime;
	protected int			cycleCounter;
	
	// ~ Other
	protected boolean 		bNeedPathPlanning;	// Force to redo path planning
	protected Long			overrideWaitDuration;
	protected boolean		saveSwitch;	
	
	// =====================================
	// ===== INITIALIZATION & SHUTDOWN =====
	// =====================================

	protected void setup(){
		super.setup();

		// --- ARGS ---
		// args 0 = Env 	   : Environnement 
		// args 1 = EntityType : Type de l'agent
		// args 2 = Boolean	   : Affichage de la map 
		// args 3 = Boolean	   : Affichage des logs  
		final Object[] args = getArguments();

		if(args[0] == null) throw new IllegalArgumentException("Environment is null");
		if(args[1] == null) throw new IllegalArgumentException("EntityType is null");
		if(args[2] == null) throw new IllegalArgumentException("DisplayMap is null");
		if(args[3] == null) throw new IllegalArgumentException("Log is null");

		type 				= (EntityType) args[1];

		deployAgent((Environment) args[0], type);
		register();
		
		// --- Description ---
		backpackCapacity   	= getBackPackFreeSpace();
		deadlockState		= DeadlockState.getDefault();
		switch(type) {
			case AGENT_COLLECTOR:
				strategy 	= Strategy.TREASURE_HUNT;
				priority	= AbstractAgent.COLLECTOR_BASE_PRIORITY;
				break;

			case AGENT_EXPLORER:
				strategy 	= Strategy.EXPLORATION;
				priority  	= AbstractAgent.EXPLORATOR_BASE_PRIORITY;
				break;

			case AGENT_TANKER:
				strategy 	= Strategy.EXPLORATION;
				priority  	= AbstractAgent.TANKER_BASE_PRIORITY;
				break;

			default:
				throw new IllegalArgumentException("Agent type is unknown : " + type);
		}

		// ~~~~~ FSM  ~~~~~

		this.fsm = new FSMBehaviour(this) {
			private static final long serialVersionUID = -6846743268622891898L;

			public int onEnd() {
				myAgent.doDelete();
				return super.onEnd();
			}
		};
		defineFSM();

		// --- Long-lived knowledge ---
		dedale 			= new Dedale((Boolean) args[2]);
		plannedMoves 	= new Stack<String>();

		explorerAgents 	= new HashSet<AID>();
		tankerAgents 	= new HashSet<AID>();
		collectorAgents = new HashSet<AID>();

		// --- Short-lived knowledge ---
		sendingMapToAgents 	= new HashSet<AID>();
		knownNearAgents 	= new HashMap<String, State>();
		knownNearTankers	= new HashMap<String, String>();
		messageReceived		= new HashMap<Topic, Integer>();
		blockedRooms		= new HashMap<String, Integer>();

		for(Topic topic:Topic.values())
			this.messageReceived.put(topic, 0);

		// --- Misc ---
		// ~ Log
		bTrace		= (Boolean) args[3];
		logEntries	= new ArrayList<String>();
		logFileName = "log/" + getLocalName();
		String s	= 
					"~~~ DESCRIPTION ~~~" + "\n" +
					"Name             = " + getLocalName() + "\n" +
					"Type             = " + type + "\n" +
					"BackpackCapacity = " + backpackCapacity + "\n" +
					"TreasureType     = " + getMyTreasureType() + "\n" +
					"~~~ EXECUTION ~~~" + "\n";
		writeToFile(s, false);

		// ~ Statistics
		cycleCounter 	= 0;
		startingTime	= LocalTime.now().toNanoOfDay();
		
		// ~ Other
		saveSwitch 		= true;

	}
	
	public void start() {
		searchDFAll();
		dedale.start(getCurrentPosition());
	}
	
	protected void takeDown(){
		try { DFService.deregister(this); }
		catch(FIPAException fe) { fe.printStackTrace(); }
	}
	
	protected void defineFSM() {
		// ===== FSM STATES =====
		fsm.registerFirstState(new StartupBehaviour(this)			, STATE_STARTUP);

		fsm.registerState(new ObservationBehaviour(this)			, STATE_OBSERVE);

		fsm.registerState(new SendHelloMessageBehaviour(this)		, STATE_SEND_HELLO);
		fsm.registerState(new ReceiveHelloMessageBehaviour(this)	, STATE_RECEIVE_HELLO);

		fsm.registerState(new SendDedaleMessageBehaviour(this)		, STATE_SEND_DEDALE);
		fsm.registerState(new ReceiveDedaleMessageBehaviour(this)	, STATE_RECEIVE_DEDALE);

		fsm.registerState(new SendStateMessageBehaviour(this)		, STATE_SEND_STATE);
		fsm.registerState(new ReceiveStateMessageBehaviour(this)	, STATE_RECEIVE_STATE);

		fsm.registerState(new PlanningBehaviour(this)				, STATE_PLANNING);
		fsm.registerState(new MoveBehaviour(this)					, STATE_MOVE);
		fsm.registerState(new DeadlockBehaviour(this)				, STATE_DEADLOCK);

		fsm.registerLastState(new TerminateBehaviour(this)			, STATE_TERMINATE);

		// ===== FSM TRANSITIONS =====
		fsm.registerDefaultTransition(STATE_STARTUP		, STATE_OBSERVE);

		fsm.registerDefaultTransition(STATE_OBSERVE		, STATE_SEND_HELLO);

		fsm.registerDefaultTransition(STATE_SEND_HELLO	, STATE_RECEIVE_HELLO);

		fsm.registerTransition(STATE_RECEIVE_HELLO		, STATE_RECEIVE_HELLO		, AbstractReceiveMessageBehaviour.Outcome.MESSAGE_RECEIVED.getValue());
		fsm.registerTransition(STATE_RECEIVE_HELLO		, STATE_SEND_DEDALE			, AbstractReceiveMessageBehaviour.Outcome.NO_MORE_MESSAGE.getValue());
		fsm.registerTransition(STATE_RECEIVE_HELLO		, STATE_PLANNING			, AbstractReceiveMessageBehaviour.Outcome.NO_MESSAGE.getValue());

		fsm.registerDefaultTransition(STATE_SEND_DEDALE	, STATE_RECEIVE_DEDALE);

		fsm.registerTransition(STATE_RECEIVE_DEDALE		, STATE_RECEIVE_DEDALE 		, AbstractReceiveMessageBehaviour.Outcome.MESSAGE_RECEIVED.getValue());
		fsm.registerTransition(STATE_RECEIVE_DEDALE		, STATE_PLANNING 			, AbstractReceiveMessageBehaviour.Outcome.NO_MORE_MESSAGE.getValue());
		fsm.registerTransition(STATE_RECEIVE_DEDALE		, STATE_PLANNING 			, AbstractReceiveMessageBehaviour.Outcome.NO_MESSAGE.getValue());
		
		fsm.registerDefaultTransition(STATE_PLANNING		, STATE_MOVE);

		fsm.registerDefaultTransition(STATE_MOVE		, STATE_OBSERVE);
		fsm.registerTransition(STATE_MOVE				, STATE_OBSERVE				, MoveBehaviour.Outcome.SUCCESS.getValue());
		fsm.registerTransition(STATE_MOVE				, STATE_SEND_STATE			, MoveBehaviour.Outcome.IDLE.getValue());
		fsm.registerTransition(STATE_MOVE				, STATE_SEND_STATE			, MoveBehaviour.Outcome.FAILED.getValue());
		fsm.registerTransition(STATE_MOVE				, STATE_SEND_STATE			, MoveBehaviour.Outcome.DEADLOCK.getValue());
		fsm.registerTransition(STATE_MOVE				, STATE_OBSERVE				, MoveBehaviour.Outcome.RESET.getValue());

		fsm.registerDefaultTransition(STATE_SEND_STATE	, STATE_RECEIVE_STATE);

		fsm.registerTransition(STATE_RECEIVE_STATE		, STATE_RECEIVE_STATE		, AbstractReceiveMessageBehaviour.Outcome.MESSAGE_RECEIVED.getValue());
		fsm.registerTransition(STATE_RECEIVE_STATE		, STATE_DEADLOCK			, AbstractReceiveMessageBehaviour.Outcome.NO_MORE_MESSAGE.getValue());
		fsm.registerTransition(STATE_RECEIVE_STATE		, STATE_DEADLOCK			, AbstractReceiveMessageBehaviour.Outcome.NO_MESSAGE.getValue());

		fsm.registerDefaultTransition(STATE_DEADLOCK	, STATE_MOVE);
		fsm.registerTransition(STATE_DEADLOCK			, STATE_OBSERVE				, DeadlockBehaviour.Outcome.RESET.getValue());
		fsm.registerTransition(STATE_DEADLOCK			, STATE_OBSERVE				, DeadlockBehaviour.Outcome.NONE.getValue());
	}

	// ==========================
	// ===== PUBLIC METHODS =====
	// ==========================
	
	public abstract Strategy computeStrategy();

	public abstract boolean isGoalReached();
	
	// Return true if prioritary compare to the other agent.
	public boolean computePriority(State agent) {
		
		if(strategy == Strategy.IDLE || plannedMoves.isEmpty() || bIsGoalReached)
			return false;
		

		boolean	res		= false;
		int		comp	= 0;

		int 	myPriority 		= getPriority();
		int 	agentPriority	= agent.priority;
		
		DeadlockState	myDeadlockState		= getDeadlockState();
		DeadlockState	agentDeadlockState	= agent.deadlockState;
		
		// I/ Qui est prioritaire si on ne compare que la priorité des agents
		comp = myPriority - agentPriority;
		if(comp<0) 			{ res = false; } // Je ne suis pas prioritaire
		else if(comp > 0) 	{ res = true; } // Je suis prioritaire
		else {
			comp = getCurrentRoom().compareTo(agent.currentPosition);	// Forcément différents (pas deux mêmes salles)
			if(comp<0) 			{ res = false; }
			else if(comp > 0) 	{ res = true; }
		}
		
		// II/ Regardons les états de chacun pour déterminer celui qui dois bouger (donc celui qui n'est pas prioritaire)
		// Le raisonnement doit être symmétrique !
		addLogEntry("    my state is : " + myDeadlockState.toString());
		addLogEntry("    " + agent.agentName +" is : " + agentDeadlockState.toString());

		switch(myDeadlockState) {
			case HARD_BLOCKED:
				if(agentDeadlockState.equals(DeadlockState.HARD_BLOCKED)) { res = false; }
				else { res = true; }
				break;

			case SOFT_BLOCKED:
				if(agentDeadlockState.equals(DeadlockState.HARD_BLOCKED)) 		{ res = false; }
				else if(agentDeadlockState.equals(DeadlockState.SOFT_BLOCKED)) 	{}
				else { res = true; }
				break;

			default:
				if(agentDeadlockState.equals(DeadlockState.HARD_BLOCKED) || agentDeadlockState.equals(DeadlockState.SOFT_BLOCKED)) { res = false; }
				break;
		}
		return res;
	}
	
	public boolean needPathPlanning() {
		return plannedMoves.isEmpty() || bNeedPathPlanning;
	}
	
	public void overrideNextMoves(Stack<String> moves) {
		plannedMoves = moves;
	}

	public String getNextMove() {
		if(!plannedMoves.isEmpty()) 
			return plannedMoves.peek();
		else
			return "";
	}

	public String getDestination() {
		if(!plannedMoves.isEmpty()) 
			return plannedMoves.firstElement();
		else
			return "";
	}
	
	public void movedTo(String position) {
		dedale.moveTo(position);
		deadlockState = DeadlockState.getDefault();
		plannedMoves.pop();
	}
	public void newCycle() {
		cycleCounter++;
		clearShortLivedKnowledge();
	}
	
	/**
	 * @param agentType - Si null, récupère les AIDs de tous les agents, sinon uniquement le type spécifié
	 */
	public Set<AID> getAgentsAID(EntityType agentType) {
		HashSet<AID> agents = new HashSet<AID>();

		if(agentType == null) {
			searchDFAll();
			agents.addAll(explorerAgents);
			agents.addAll(tankerAgents);
			agents.addAll(collectorAgents);
		}else {
			switch(agentType) {
				case AGENT_EXPLORER:
					if(explorerAgents.isEmpty()) searchDF(EntityType.AGENT_EXPLORER);
					agents.addAll(explorerAgents);
					break;

				case AGENT_TANKER:
					if(this.collectorAgents.isEmpty()) searchDF(EntityType.AGENT_COLLECTOR);
					agents.addAll(tankerAgents);
					break;

				case AGENT_COLLECTOR:
					if(this.tankerAgents.isEmpty()) searchDF(EntityType.AGENT_TANKER);
					agents.addAll(collectorAgents);
					break;

				default:
					throw new IllegalArgumentException("Agent type is unknown : " + agentType);
			}
		}
		return agents;
	}
	
	public boolean isNextToMe(String position) {
		return dedale.getNodeNeighbours(getCurrentRoom()).contains(position);
	}
	
	public void addBlockedRoom(String id) {
		blockedRooms.put(id, AbstractAgent.BLOCKED_ROOM_TIMEOUT);
	}

	public boolean isBackpackEmpty() {
		return getBackpackCapacity() == getBackPackFreeSpace();
	}
	
	public void forcePathPlanning() {
		bNeedPathPlanning = true;
	}
	
	public void donePathPlanning() {
		bNeedPathPlanning = false;
	}
	
	public TreasureType getTreasureType() {
		return TreasureType.fromValue(getMyTreasureType());
	}
	
	public void clearKnownNearAgents() {
		knownNearAgents.clear();
	}

	// ===========================
	// ===== PRIVATE METHODS =====
	// ===========================
	
 	private void clearShortLivedKnowledge() {
		sendingMapToAgents.clear();
		knownNearAgents.clear();
		knownNearTankers.clear();
		clearMessageReceived();
		clearTimedOutBlockedRoom();
		objective = "";
	}

 	private void clearMessageReceived() {
		for(Topic topic:Topic.values())
			this.messageReceived.put(topic, 0);
 	}

 	private void clearTimedOutBlockedRoom() {

 		Iterator<Map.Entry<String, Integer>> it = blockedRooms.entrySet().iterator();

		while(it.hasNext()) {
			Map.Entry<String, Integer> blockedRoom = it.next();

			if(blockedRoom.getValue() == 0) {
				addLogEntry(blockedRoom.getKey() + " is now a valid destination");
				it.remove();
			}
			else { blockedRooms.put(blockedRoom.getKey(), blockedRoom.getValue() - 1); }
		}
		addLogEntry("Remaining blocked rooms : " + blockedRooms);
 	}

	private void register() {
		// --- Register the agent in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd  = new ServiceDescription();

		dfd.setName(getAID());

		sd.setType(type.toString());
		sd.setName(getLocalName());
		
		dfd.addServices(sd);
		
		try { DFService.register(this, dfd); }
		catch(FIPAException fe) { fe.printStackTrace(); }
	}
	
	protected void goalReached() {
		addLogEntry("my goal has been reached in : ");
		addLogEntry(" 	Cycle : " + getCycleCounter());
		addLogEntry(" 	Time : " + (LocalTime.now().toNanoOfDay() - startingTime)/1000000 + " milliseconds");
		bIsGoalReached = true;
		priority -= 1000;
		forcePathPlanning();
		System.out.println(getLocalName() + " : GOAL REACHED");
		trace("GOAL REACHED", false);
	}
	// =======================================================
	// ==================== COMMUNICATION ==================== 
	// =======================================================
	
	/**
	 * Màj des agents connus
	 */
	private void searchDFAll() {
		searchDF(EntityType.AGENT_COLLECTOR);
		searchDF(EntityType.AGENT_EXPLORER);
		searchDF(EntityType.AGENT_TANKER);
	}
	
	/**
	 * 
	 * @param agentType - Type d'agent (donc de services proposés) dont on veut avoir connaissance.
	 */
	private void searchDF(EntityType agentType) {
        DFAgentDescription dfd 	= new DFAgentDescription();
        ServiceDescription sd 	= new ServiceDescription();
        SearchConstraints ALL 	= new SearchConstraints();

        sd.setType(agentType.toString());
        dfd.addServices(sd);
        ALL.setMaxResults(new Long(-1));

        HashSet<AID> agents = new HashSet<AID>();

        try
        {
            DFAgentDescription[] result = DFService.search(this, dfd, ALL);
			AID aid = null;
			for (int i=0; i<result.length; i++) {
				aid = result[i].getName();
				if(aid.compareTo(getAID()) != 0) agents.add(aid);
            }
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
        
		switch(agentType) {
			case AGENT_EXPLORER:
				explorerAgents.addAll(agents);
				break;

			case AGENT_TANKER:
				tankerAgents.addAll(agents);
				break;

			case AGENT_COLLECTOR:
				collectorAgents.addAll(agents);
				break;

			default:
				throw new IllegalArgumentException("Agent type is unknown : " + agentType);
		}
	}

	// ===========================
	// ===== UTILITY METHODS =====
	// ===========================
	
	public void addLogEntry(String s) {
		String prefix = "... ";
		if (bTrace) this.logEntries.add(prefix + s);
	}
	
	public void trace(String title, boolean refreshGraph) {
		if(!this.bTrace) { return; }

		String s = "";

		String header = "~~~ [" + getLocalName() + "] - " + title + " ~~~";
		String footer = "";

		for(int i = 0; i< header.length(); i++)
			footer += "~";

		s += header + "\n";

		for(String log:logEntries)
			s += log + "\n";

		s += footer + "\n\n";
		
		writeToFile(s, true);
		logEntries.clear();
		
		if( refreshGraph ) {
			// Due to how fast one cycle can be, the program may be shutdown during this operation, which may result in a corrupted file.
			FileSinkImages pic = new FileSinkImages(OutputType.png, Resolutions.VGA);
			pic.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
			pic.setQuality(Quality.LOW);
			pic.setRenderer(RendererType.SCALA);
			try {
				pic.writeAll(dedale.getGraph(), logFileName + "_world.png");
			} catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	private void writeToFile(String s, boolean append) {
		try {
			if(bTrace) {
				PrintWriter pw = new PrintWriter(new FileWriter(logFileName + ".log", append), true);
				pw.println(s);
				pw.close();
			}
		} catch (IOException e) { e.printStackTrace(); }
	}

	// =======================
	// ===== NESTED ENUM =====
	// =======================
	
	// Enum describing the nature of deadlock
	// 		UNKNOWN 		- State not computed 
	// 		ALTERNATIVE 	- An alternative way has been found
	// 		SOFT_BLOCKED 	- Can move localy, but won't solve the deadlock
	// 		HARD_BLOCKED 	- Cannot move at all
	public enum DeadlockState {
		NONE("NONE"),
		UNKNOWN("UNKNOWN"),
		ALTERNATIVE("ALTERNATIVE"),
		SOFT_BLOCKED("SOFT_BLOCKED"),
		HARD_BLOCKED("HARD_BLOCKED");
		
		private final String value;
		
		DeadlockState(String value){ this.value = value; }

		public String getValue() { return value; }
		public String toString() { return value; }
		public static DeadlockState getDefault() { return UNKNOWN; }
		
		public static DeadlockState fromValue(String value) {
			if(value != null) {
				for(DeadlockState deadlockState : values())
						if (deadlockState.value.equals(value)) { return deadlockState; }

			}else { throw new IllegalArgumentException("value is null"); }

			return getDefault();
		}
	}

	// Enum describing how the agent will planify his path
	// 		NONE 			- Initialization value 
	// 		IDLE 			- Stay in position 
	// 		GOTO 			- Go to objective
	// 		RANDOM_WALK 	- Randomly move to a neighbour node
	// 		EXPLORATION 	- Explore the graph 
	// 		TREASURE_HUNT 	- Look for treasure
	public enum Strategy {
		NONE("None"),
		IDLE("Idle"),
		GOTO("GOTO"),
		RANDOM_WALK("Random walk"),
		EXPLORATION("Exploration"),
		RENDEZ_VOUS("Rendez_vous"),
		TREASURE_HUNT("Treasure hunt");

		private final String value;
		
		Strategy(String value){ this.value = value; }

		public String getValue() { return value; }
		public String toString() { return value; }
		public static Strategy getDefault() { return NONE; }
		
		public static Strategy fromValue(String value) {
			if(value != null) {
				for(Strategy strategy : values())
					if (strategy.value.equals(value)) { return strategy; }

			}else { throw new IllegalArgumentException("value is null"); }

			return getDefault();
		}
	}
	
	// Enum describing the subject of a conversation
	// 		NONE 			- Initialization value 
	// 		UNKNOWN 		- State not computed 
	// 		ALTERNATIVE 	- An alternative way has been found
	// 		SOFT_BLOCKED 	- Can move localy, but won't solve the deadlock
	// 		HARD_BLOCKED 	- Cannot move at all
	public enum Topic {
		NONE("NONE"),
		HELLO("HELLO"),
		STATE("STATE"),
		DEDALE("DEDALE");
		
		private final String value;
		
		Topic(String value){ this.value = value; }

		public String getValue() { return value; }
		public String toString() { return value; }
		public static Topic getDefault() { return NONE; }
		
		public static Topic fromValue(String value) {
			if(value != null) {
				for(Topic topic : values())
						if (topic.value.equals(value)) { return topic; }

			}else { throw new IllegalArgumentException("value is null"); }

			return getDefault();
		}
	}

	// Class used to describe the state of an AbstractAgent in a serializable data structures
	public class State implements Serializable{

		private static final long serialVersionUID = 6979391665218248610L;

		public AID 				agentAID;
		public EntityType 		agentType;
		public String 			agentName;
		public String 			currentPosition;
		public String 			nextMove;
		public String 			destination;
		public Stack<String> 	plannedMoves;
		public int 				priority;
		public DeadlockState	deadlockState;
		
		public State(AbstractAgent A) {
			this.agentAID 			= A.getAID();
			this.agentType 			= A.getType();
			this.agentName 			= A.getLocalName();
			this.currentPosition 	= A.getCurrentRoom();
			this.nextMove			= A.getNextMove();
			this.destination		= A.getDestination();
			this.plannedMoves		= A.getPlannedMoves();
			this.priority			= A.getPriority();
			this.deadlockState		= A.getDeadlockState();
		}

		@SuppressWarnings("unchecked")
		private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
			agentAID		= (AID) ois.readObject();
			agentType		= (EntityType) ois.readObject();
			agentName 		= ois.readUTF();
			currentPosition	= ois.readUTF();
			nextMove		= ois.readUTF();
			destination		= ois.readUTF();
			plannedMoves	= (Stack<String>) ois.readObject();
			priority		= ois.readInt();
			deadlockState	= DeadlockState.fromValue(ois.readUTF());
	    }

	    private void writeObject(ObjectOutputStream oos) throws IOException {
	    	oos.writeObject(agentAID);
	    	oos.writeObject(agentType);
			oos.writeUTF(agentName);
			oos.writeUTF(currentPosition);
			oos.writeUTF(nextMove);
			oos.writeUTF(destination);
	    	oos.writeObject(plannedMoves);
	    	oos.writeInt(priority);
	    	oos.writeUTF(deadlockState.getValue());
	    }
	}

	// ===================
	// ===== GETTERS =====
	// ===================

	public State getCurrentState() {

		// Estimation de son état
		if (deadlockState == DeadlockState.UNKNOWN ) {
			if(dedale.getNodeNeighbours(getCurrentRoom()).size() <=2 ) { deadlockState = DeadlockState.SOFT_BLOCKED; }
		}
		return new State(this);
	}

	public EntityType getType() {
		return type;
	}

	public Strategy getCurrentStrategy() {
		return strategy;
	}

	public int getPriority() {
		return priority;
	}

	public DeadlockState getDeadlockState() {
		return deadlockState;
	}

	public int getBackpackCapacity() {
		return backpackCapacity;
	}

	public Dedale getDedale() {
		return dedale;
	}

	public Stack<String> getPlannedMoves() {
		return plannedMoves;
	}

	public Set<AID> getExplorerAgents() {
		return explorerAgents;
	}

	public Set<AID> getCollectorAgents() {
		return collectorAgents;
	}

	public Set<AID> getTankerAgents() {
		return tankerAgents;
	}

	public Set<AID> getSendingMapToAgents() {
		return sendingMapToAgents;
	}

	public Map<String, State> getKnownNearAgents() {
		return knownNearAgents;
	}

	public Map<String, String> getKnownNearTankers() {
		return knownNearTankers;
	}

	public int getMessageReceived(Topic topic) {
		return messageReceived.get(topic);
	}

	public void incrementMessageReceived(Topic topic) {
		messageReceived.put(topic, messageReceived.get(topic)+1);
	}
	
	public void addSendingMapToAgents(AID aid){
		this.sendingMapToAgents.add(aid);
	}
	
	public void confirmMapSendingToAgent(AID agent) {
		this.sendingMapToAgents.remove(agent);
	}
	
	public void addKnownNearAgents(State agentState){
		if(agentState == null) throw new IllegalArgumentException("Agent State is null");

		knownNearAgents.put(agentState.agentName, agentState);

		switch(agentState.agentType) {
			case AGENT_COLLECTOR:
				this.collectorAgents.add(agentState.agentAID); break;
			case AGENT_EXPLORER:
				this.explorerAgents.add(agentState.agentAID); break;
			case AGENT_TANKER:
				this.tankerAgents.add(agentState.agentAID); break;
			default:
				throw new IllegalArgumentException("Agent type unknown : " + agentState.agentType);
		}
	}

	public void addKnownNearTankers(String name, String pos) {
		this.knownNearTankers.put(name, pos);
	}
		
	public List<String> getLogEntries() {
		return logEntries;
	}

	public boolean isDisplayLog() {
		return bTrace;
	}

	public long getStartingTime() {
		return startingTime;
	}

	public int getCycleCounter() {
		return cycleCounter;
	}

	public Long getOverrideWaitDuration() {
		return overrideWaitDuration;
	}

	public String getCurrentRoom() {
		return dedale.getCurrentNode();
	}

	public Set<String> getBlockedRooms() {
		return new HashSet<String>(blockedRooms.keySet());
	}

	public String getObjective() {
		return objective;
	}
	// ===================
	// ===== SETTERS =====
	// ===================

	public void setType(EntityType type) {
		this.type = type;
	}

	public void setCurrentStrategy(Strategy currentStrategy) {
		this.strategy = currentStrategy;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public void setDeadlockState(DeadlockState deadlockState) {
		this.deadlockState = deadlockState;
	}

	public void setBackpackCapacity(int backpackCapacity) {
		this.backpackCapacity = backpackCapacity;
	}

	public void setDedale(Dedale dedale) {
		this.dedale = dedale;
	}

	public void setPlannedMoves(Stack<String> plannedMoves) {
		this.plannedMoves = plannedMoves;
	}

	public void setExplorerAgents(Set<AID> explorerAgents) {
		this.explorerAgents = explorerAgents;
	}

	public void setCollectorAgents(Set<AID> collectorAgents) {
		this.collectorAgents = collectorAgents;
	}

	public void setTankerAgents(Set<AID> tankerAgents) {
		this.tankerAgents = tankerAgents;
	}

	public void setSendingMapToAgents(Set<AID> sendingMapToAgents) {
		this.sendingMapToAgents = sendingMapToAgents;
	}

	public void setKnownNearAgents(Map<String, State> knownNearAgents) {
		this.knownNearAgents = knownNearAgents;
	}

	public void setKnownNearTankers(Map<String, String> knownNearTankers) {
		this.knownNearTankers = knownNearTankers;
	}

	public void setMessageReceived(Map<Topic, Integer> messageReceived) {
		this.messageReceived = messageReceived;
	}

	public void setLogEntries(List<String> logEntries) {
		this.logEntries = logEntries;
	}

	public void setDisplayLog(boolean bDisplayLog) {
		this.bTrace = bDisplayLog;
	}

	public void setStartingTime(Long startingTime) {
		this.startingTime = startingTime;
	}

	public void setCycleCounter(int cycleCounter) {
		this.cycleCounter = cycleCounter;
	}

	public void setOverrideWaitDuration(Long overrideWaitDuration) {
		this.overrideWaitDuration = overrideWaitDuration;
	}

	public void setObjective(String objective) {
		this.objective = objective;
	}

}
