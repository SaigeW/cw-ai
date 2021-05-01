package uk.ac.bris.cs.scotlandyard.ui.ai;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.*;
import io.atlassian.fugue.Pair;
import org.checkerframework.checker.units.qual.A;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.*;


import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MyAi implements Ai {

	@Nonnull @Override public String name() { return "MyAi"; }


	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		// create list of players update with current status
		ImmutableList<Player> allPlayers = getPlayerList(board);
		Player mrX = getMrX(allPlayers);
		ImmutableList<Player> detectives = getDetectives(allPlayers);
		Model copyOfModel = copyOfModel(board, allPlayers);
		
		Tree tree = new Tree(copyOfModel);

		int depth = 4;
		double score = minimax(tree.startNode, Model copyOfModel, depth, -99999, 99999);

		// TODO: how to coordinate board during recursion & from score to move
		List<Move> moves = board.getAvailableMoves().asList();

		return moves.get(new Random().nextInt(moves.size()));
	}

	private double minimax(Vertex currentNode, Model model,  int depth, double alpha, double beta) {

		// currentBoard is actually a model object
		Model currentBoard = currentNode.getCurrentBoard();

		//If the leaves of the tree have been reached (the lowest layer which will be evaluated), calculate the score for the currentBoard.
		if (depth == 0) {
			double score = scoring(currentBoard.getCurrentBoard());
			currentNode.setScore(score); //Set the score stored in the node. This is required to find the best move.
			return score;
		}
		// currently haven't been reach lowest level of tree
		else {
			List<Move> moves = currentBoard.getCurrentBoard().getAvailableMoves().asList();

			if (!moves.isEmpty()) {
				Move checkAttribution = moves.get(0);

				// check whether it is mrX' turn
				if (checkAttribution.commencedBy().isMrX()) {

					// if it's mrX' turn, set maximum sore to node & update alpha.
					double scoreOfMax = -99999;
					// continuing going left at first
					for (Move move : moves) {
						// TODO: how to advance ?

						// update child node
						Vertex child = new Vertex(currentBoard.advance(move));
						child.move = move;
						currentNode.addChild(child);

						// get mrX' location
						mrXLocation = getDestination(move);

						// calculate & set the scores for each child node.
						double scoreOfChild = minimax(child, mrXLocation, detectives, remainingDepth - 1, alpha, beta);

						// TODO: check whether it is right to do
						// ATTENTION! starting execute rest of code inside bracket from the second lowest level of tree
						scoreOfMax = Math.max(scoreOfMax, scoreOfChild);
						currentNode.setScore(scoreOfMax);

						// update alpha, cuz it's mrX' turn
						alpha = Math.max(alpha, scoreOfChild);

						// Alpha Beta Pruning, if beta(minimum upper bound) and alpha(maximum lower bound)
						// do not have intersection any more, no need to continue recursion
 						if (beta <= alpha) {
							break;
						}
					}
					return scoreOfMax;
				}
				else {

					// if it's detective' turn, set minimum sore to node & update beta.
					double minScore = 99999;

					// TODO: how to generate moves with bunch of detectives?

					for (Move move : moves) {
						// TODO: how to advance ?

						Vertex child = new Vertex(currentBoard.advance(move));
						child.move = move;
						currentNode.addChild(child);

						double childScore = minimax(child, mrXLocation, detectives, remainingDepth - 1, alpha, beta);
						minScore = Math.min(minScore, childScore); //Maintain the minimum score of the child nodes.
						currentNode.setScore(minScore);

						// Alpha Beta Pruning, if beta(minimum upper bound) and alpha(maximum lower bound)
						// do not have intersection any more, no need to continue recursion
						beta = Math.min(beta, childScore);
						if (beta <= alpha) {
							break;
						}
					}
					return minScore;
				}
			}
			// cannot make any move
			else {
				double score = scoring(currentBoard.getCurrentBoard());
				currentNode.setScore(score);
				return score;
			}
		}
	}



	// store Tree from the starting point
	private class Tree {

		private final Vertex startNode;

		public Tree(Model currentBoard) {
			this.startNode = new Vertex(currentBoard);
		}

		public Vertex getStartNode() {
			return this.startNode;
		}
	}

	// father & children vertex , current state, current score, move
	private class Vertex {
		private Vertex father;
		private List<Vertex> children = new ArrayList<>();
		private Model currentBoard;
		private double score;
		private Move move;

		private Vertex(Model currentBoard) {
			this.currentBoard = currentBoard;
		}

		private Model getCurrentBoard() {
			return this.currentBoard;
		}

		private List<Vertex> getChildren() {
			return this.children;
		}

		private void addChild(Vertex node) {
			children.add(node);
		}

		private Vertex getFather(){
			return this.father;
		}

		private void setScore(double score) {
			this.score = score;
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
	

		private int scoring(Board board){
			return new Random().nextInt();
		}
		

		public ImmutableList<Player> getPlayerList(Board board){
			List<Player> allPlayers = new ArrayList<>();
			for (Piece p: board.getPlayers()){
				// iterate through all players
				Optional<Board.TicketBoard> tempTicketBoardForEach = board.getPlayerTickets(p);
				HashMap<Ticket, Integer> theTicketMap = new HashMap<>();

				// update tickets for every player
				if (tempTicketBoardForEach.isPresent()) {
					Board.TicketBoard mid = tempTicketBoardForEach.get();
					for (Ticket t : Ticket.values()) {
						theTicketMap.put(t,mid.getCount(t));
					}
				}
				ImmutableMap<Ticket, Integer> tempMap = ImmutableMap.copyOf(theTicketMap);

				// get player location
				// TODO: if whether getAvailableMoves is empty?
				int	locationOfPlayer = board.getAvailableMoves().asList().get(0).source();

				Player temp = new Player(p, tempMap, locationOfPlayer);
				allPlayers.add(temp);
			}
			return ImmutableList.copyOf(allPlayers);
		}

	public Player getMrX(ImmutableList<Player> players) {
		Player mrX = null;
		for (Player p : players) { if (p.isMrX()) mrX = p; }
		return mrX;
	}

	public ImmutableList<Player> getDetectives(ImmutableList<Player> players){
		List<Player> detectives = new ArrayList<>();
		for (Player p : players) {
			if (p.isDetective()) {
				detectives.add(p);
			}
		}
		return ImmutableList.copyOf(detectives);
	}
	
		// build a copy of current board
		public Model copyOfModel(Board board, ImmutableList<Player> players) {
			Player mrX = getMrX(players);
			ImmutableList<Player> detectives = getDetectives(players);
			return new MyModelFactory().build(board.getSetup(), mrX, detectives);
		}

		public ImmutableList<ImmutableList<Move>> combinationOfMoves(Board board){
		
		}
}
