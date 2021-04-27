package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.Ai;
import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MyAi"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// returns a random move, replace with your own implementation
		var moves = board.getAvailableMoves().asList();
		// TODO: replacing this place holder
		return moves.get(new Random().nextInt(moves.size()));
	}

	// using visitor pattern to get destination of each availableMoves
	private Integer getIntermediateDestination(Move move) {
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
	//       2) how to determine the mini step
	public class MiniMax{
		private HashMap<Integer, Boolean> marked;// if this vertex has been through
		private HashMap<Integer, Integer> collectionOfEdges;// using for trace
		private final int startLocation;// mrX'  starting location
		private ArrayList<Move> avaliableMoves;

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

		// TODO:  1) set up the depth
		//		2) figure out how to walk back, and go to another child vertex
		private void mX(ArrayList<Move> availableMoves,
						 int currentLocation){
			// mark the vertex
			this.marked.put(currentLocation, true);
			for (Move avaliableMove : availableMoves) {
				Integer oneOfNextLocation = getIntermediateDestination(avaliableMove);
				// determine if child vertex has been used
				if (!marked.get(oneOfNextLocation)) {
					// store to a list, which used to produce the trace
					this.collectionOfEdges.put(oneOfNextLocation, currentLocation);
					// TODO: update availableMoves

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
	}

}
