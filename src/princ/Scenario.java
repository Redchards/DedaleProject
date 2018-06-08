package princ;

import env.Environment;
import env.Environment.ENVtype;

public class Scenario {

	// --- General
	public Environment env;

	// --- Wumpus
	public boolean 	wumpus;

	// --- Explorer
	public int		numberOfExplorerAgents;
	public String	explorerAgentsNamePrefix;
	public String	explorerAgentsContainer;
	public int  	numberOfExplorerMap; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
	public int  	numberOfExplorerLog; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)

	// --- Tanker
	public int		numberOfTankerAgents;
	public String	tankerAgentsNamePrefix;
	public String	tankerAgentsContainer;
	public int  	numberOfTankerMap; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
	public int  	numberOfTankerLog; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)
	
	// --- Collector 
	public int		numberOfCollectorAgents;
	public String	collectorAgentsNamePrefix;
	public String	collectorAgentsContainer;
	public int  	numberOfCollectorMap; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
	public int  	numberOfCollectorLog; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)
	
	public enum SCENARIO {
		DEFAULT,		// 1 explorer, 							grid
		E0T1C1_2017,	// 2 explorer, 1 tanker, 5 collector, 	2017
		E1T0C0_2017,	// 1 explorer, 							2017
		E2T0C0_G,		// 2 explorer, 							grid
		E2T0C0_2017,	// 2 explorer, 							2017
		E2T1C1_2017,	// 2 explorer, 							2017
		E2T1C5_2017,	// 2 explorer, 1 tanker, 5 collector, 	2017
		E2T1C5_2017_W,	// 2 explorer, 1 tanker, 5 collector, 	2017 	With WUMPUS
		E2T1C5_DOROG,	// 2 explorer, 1 tanker, 5 collector, 	DOROG
		SOUTENANCE,		// 3 explorer, 1 tanker, 4 collector, 	2018
	}
	
	public Scenario(SCENARIO choice) {
		
		// Default values. Each scenario can override these settings

		// --- Wumpus
		wumpus = false;

		// --- Explorer
		numberOfExplorerAgents   	= 0;
		explorerAgentsNamePrefix 	= "Explo";
		explorerAgentsContainer 	= "container0";
		numberOfExplorerMap 		= 0; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
		numberOfExplorerLog 		= -1; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)

		// --- Tanker
		numberOfTankerAgents   		= 0;
		tankerAgentsNamePrefix 		= "Tank";
		tankerAgentsContainer 		= "container1";
		numberOfTankerMap 			= 0; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
		numberOfTankerLog 			= -1; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)
		
		// --- Collector 
		numberOfCollectorAgents   	= 0;
		collectorAgentsNamePrefix 	= "col";
		collectorAgentsContainer 	= "container2";
		numberOfCollectorMap 		= 0; // Pour combien d'agents doit on afficher la carte ? (-1 pour tous)
		numberOfCollectorLog 		= -1; // Pour combien d'agents doit on afficher les logs ? (-1 pour tous)

		switch(choice) {
			case E2T1C5_2017:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");

				// --- Explorer
				numberOfExplorerAgents   	= 2;

				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 5;

				break;

			case E2T1C5_DOROG:
				env= new Environment(ENVtype.DOROGOVTSEV_T,15,null);

				// --- Explorer
				numberOfExplorerAgents   	= 2;

				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 5;

				break;

			case E0T1C1_2017:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");
				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 1;

				break;

			case E2T1C1_2017:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");
				// --- Explorer
				numberOfExplorerAgents   	= 2;

				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 1;

				break;

			case E2T1C5_2017_W:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");

				wumpus = true;

				// --- Explorer
				numberOfExplorerAgents   	= 2;

				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 5;

				break;

			case E2T0C0_G:
				env = new Environment(ENVtype.GRID_T,5,null);

				// --- Explorer
				numberOfExplorerAgents   	= 2;
				numberOfExplorerMap			= 0;
				break;

			case E2T0C0_2017:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");

				// --- Explorer
				numberOfExplorerAgents   	= 2;
				numberOfExplorerMap			= 1;
				break;

			case E1T0C0_2017:
				env = new Environment("ressources/map/map2017-2","ressources/map/map2017-config-2");

				// --- Explorer
				numberOfExplorerAgents   	= 1;
				numberOfExplorerMap			= 0;
				break;

			case SOUTENANCE:
				env = new Environment("ressources/map/map2018.txt","ressources/map/map2018-multiType-exam.txt");

				wumpus = true;

				// --- Explorer
				numberOfExplorerAgents   	= 3;

				// --- Tanker
				numberOfTankerAgents   		= 1;
				
				// --- Collector 
				numberOfCollectorAgents   	= 4;

				break;
			default:
				env = new Environment(ENVtype.GRID_T,5,null);

				// --- Explorer
				numberOfExplorerAgents   	= 1;
				numberOfExplorerMap			= 1;
				break;
		}
	}
}
