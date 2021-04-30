package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.*;
import com.google.errorprone.annotations.Immutable;
import io.atlassian.fugue.Pair;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.bris.cs.scotlandyard.event.GameStarted;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;
import uk.ac.bris.cs.scotlandyard.ui.GameControl;
import uk.ac.bris.cs.scotlandyard.ui.model.PlayerProperty;
import uk.ac.bris.cs.scotlandyard.ui.model.TicketProperty;


import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MyAi"; }


	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {

		//Initialization of players and models for simulation
		List<Player> tempDetectives = new ArrayList<>();
		Player mrX = null;
				/*
				initialize player classes with default tickets
				 */
		for(Piece p: board.getPlayers()) {
			PlayerProperty<Piece> playerProperty = new PlayerProperty<>(p);
			Player player = playerProperty.asPlayer();
			if (player.isDetective()) tempDetectives.add(player);
			else mrX = player;
		}
		ImmutableList<Player> detectives = ImmutableList.copyOf(tempDetectives);
		Model initialModel = build(board.getSetup(), mrX, detectives);

		//calculate the best move
		List<Move> moves = board.getAvailableMoves().asList();


		// TODO: replacing this place holder
		return moves.get(new Random().nextInt(moves.size()));
	}


	// alpha : max      beta : min
	private int getIndex(List<Move> moves, int score, int index, int alpha, int beta,
						 Board board, Player mrX, ImmutableList<Player> detectives, int pointer){
		for (Move move: moves) {
			if (index <= moves.size() - 1) {
				Model model = build(board.getSetup(), mrX, detectives);
				model.chooseMove(move);
				// TODO : alpha-beta in first layer
				int newScore = minimax(3, false, alpha, beta, model);
				if(newScore < score){
					getIndex(moves, score, index, alpha, beta,
							model.getCurrentBoard(), mrX, detectives, pointer);
				}
				else{
					getIndex(moves, newScore, index, alpha, beta,
							model.getCurrentBoard(), mrX, detectives, pointer+1);
				}
			}
			else return pointer;
		}
	}

	// using visitor pattern to get destination of each availableMoves
	private Integer getDestination(Move move) {
		return move.visit(new Move.Visitor<>() {
			@Override
			public Integer visit(Move.SingleMove singleMove) {
				return singleMove.destination;
			}
			@Override
			public Integer visit(Move.DoubleMove doubleMove) {
				return doubleMove.destination2;
			}
		});
	}

	// TODO: 1) get the max value:
	//  			add a container tpo store the last vertex of each path & compare them during searching
	//       2) how to determine the  mini step
	//public class MiniMax{
		private HashMap<Integer, Boolean> marked;// if this vertex has been through
		private HashMap<Integer, Integer> collectionOfEdges;// using for trace
		private final int startLocation;// mrX'  starting location
		private ArrayList<Move> avaliableMoves;
		private int min;
		private int max;


		// TODO: get mrX' current location for startLocation from the outside
		public MiniMax(ImmutableList<Move> moves,
								int startLocation) {
			marked = new HashMap<>();
			// record the trace from every vertex to the starting point
			this.collectionOfEdges = new HashMap<>();
			this.startLocation = startLocation;
			this.avaliableMoves = new ArrayList<>(moves);
			// starting recursion
			mX(this.avaliableMoves, startLocation);
		}


		public int minimax(int depth, Boolean isMax, int alpha, int beta, Model currentBoard)
		{
			// Terminating condition. i.e
			// leaf node is reached
			if (depth == 4)
				return scoring(currentBoard.getCurrentBoard());

			if (isMax)
			{
				int best = min;
				// Recur for left and
				// right children
				/*
				current GameState
				 */

				for (Move move:theBoard.getAvailableMoves())
				{
					//int val = minimax(depth + 1, nodeIndex * 2 + i, false, values, alpha, beta
//					List<Player> detectives = new ArrayList<>();
//					Player mrX = null;
//
//					for(Piece p: theBoard.getPlayers()){
//						PlayerProperty<Piece> playerProperty = new PlayerProperty<>(p);
//						Player temp = playerProperty.asPlayer();
//						if (temp.isDetective()) {
//							detectives.add(temp);
//						}
//						else {
//							mrX = temp;
//						}
//					}
					ImmutableList<Player> immutableDetectives = ImmutableList.copyOf(detectives);
					Model newModel = build(theBoard.getSetup(), )
					int val = minimax(depth+1, false, alpha, beta);
					best = Math.max(best, val);
					alpha = Math.max(alpha, best);

					// Alpha Beta Pruning
					if (beta <= alpha)
						break;
				}
				return best;
			}
			else
			{
				int best = max;
				// Recur for left and
				// right children
				for (int i = 0; i < 2; i++)
				{
					int val = minimax(depth + 1, nodeIndex * 2 + i,
							true, values, alpha, beta);
					best = Math.min(best, val);
					beta = Math.min(beta, best);

					// Alpha Beta Pruning
					if (beta <= alpha)
						break;
				}
				return best;
			}
		}

		private int scoring(Board board){
			return new Random().nextInt();
		}

		public ImmutableMap getTickets(Optional tickets){

		}

		public Player getPlayer(Piece piece,
								@Nonnull ImmutableMap<Ticket, Integer> tickets,
								int location){
			return new Player(piece, tickets, location);
		}


		// get current state
		public Model build(GameSetup setup,
						   Player mrX,
						   ImmutableList<Player> detectives) {
			/*
			 * return a game Model which should hold a GameState and Observer list
			 */
			return new MyModelFactory().build(setup, mrX, detectives);
		}


		// TODO:  1) set up the depth
		//		2) figure out how to walk back, and go to another child vertex
		private void mX(ArrayList<Move> availableMoves,
						 int currentLocation){
			// mark the vertex
			this.marked.put(currentLocation, true);
			for (Move avaliableMove : availableMoves) {
				Integer oneOfNextLocation = getDestination(avaliableMove);
				// determine if child vertex has been used
				if (!marked.get(oneOfNextLocation)) {
					// store to a list, which used to produce the trace
					this.collectionOfEdges.put(oneOfNextLocation, currentLocation);
					// TODO: update availableMoves
					/* return a new GameState with advance*/
					// recursion step, walk to oneOfNextLocation
					mX(availableMoves,oneOfNextLocation);
				}
			}
		}

		public boolean hasPathTo(int currentLocation) { return marked.get(currentLocation); }

		// trace back, get the path
		public Iterable<Integer> pathTo(int currentLocation)
		{
			if (!hasPathTo(currentLocation)) return null;
			Stack<Integer> path = new Stack<>();
			// is it right ?
			for (int x = currentLocation; x != this.startLocation; x = collectionOfEdges.get(x))
				path.push(x);
			path.push(this.startLocation);
			return path;
		}
	//}

}
