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

    @Nonnull
    @Override
    public String name() {
        return "MyAi";
    }


    @Nonnull
    @Override
    public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        // create list of players update with current status
        ImmutableList<Player> allPlayers = getPlayerList(board);
        Player mrX = getMrX(allPlayers);
        ImmutableList<Player> detectives = getDetectives(allPlayers);
        Model copiedModel = copyOfModel(board, allPlayers);

        Tree tree = new Tree(copiedModel);

        int depth = 4;
        double score = minimax(tree.startNode, Model copiedModel, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // TODO: how to coordinate board during recursion & from score to move
// 		List<Move> moves = board.getAvailableMoves().asList();
// 		return moves.get(new Random().nextInt(moves.size()));
        return getBestMove(tree, score);
    }

    private double minimax(Vertex currentNode, Model model, int depth, boolean isMax, double alpha, double beta) {

        // currentBoard is actually a model object
        Board currentBoard = model.getCurrentBoard();

        //If the leaves of the tree have been reached (the lowest layer which will be evaluated), calculate the score for the currentBoard.
        if (depth == 0) {
            double score = scoring(model.getCurrentBoard(), );
            currentNode.setScore(score); //Set the score stored in the node. This is required to find the best move.
            return score;
        }
        // currently haven't been reach lowest level of tree
        if (isMax) {
            List<Move> moves = model.getCurrentBoard().getAvailableMoves().asList();

            if (!moves.isEmpty()) {

                // if it's mrX' turn, set maximum sore to node & update alpha.
                double scoreOfMax = Double.NEGATIVE_INFINITY;
                // continuing going left at first
                Model nextModel;
                for (Move move : moves) {
                    // TODO: how to advance ?
                    nextModel = copyOfModel(currentBoard, getPlayerList(currentBoard));
                    nextModel.chooseMove(move);
                    // update child node
                    Vertex child = new Vertex(nextModel);
                    child.move = move;
                    currentNode.addChild(child);
                    // calculate & set the scores for each child node.
                    double scoreOfChild = minimax(child, nextModel, depth + 1, false, alpha, beta);

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
            // cannot make any move
            else {
                double score = scoring(model, );
                currentNode.setScore(score);
                return score;
            }
        } else {
            // if it's detective' turn, set minimum sore to node & update beta.
            double minScore = Double.POSITIVE_INFINITY;

            // TODO: how to generate moves with bunch of detectives?
            ImmutableList<List<Move>> combinations = combinationOfMoves(model.getCurrentBoard(),
                    getDetectives(getPlayerList(model.getCurrentBoard())));

            Model nextModel;

            for (List<Move> combination : combinations) {
                for (Move move : combination) {
                    nextModel = copyOfModel(currentBoard, getPlayerList(currentBoard));
                    nextModel.chooseMove(move);
                }
                Vertex child = new Vertex(nextModel);
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

        private void setScore(double score) {
            this.score = score;
        }

        private void setMove(Move move) {
            this.move = move;
        }
    }

    private class dVertex extends Vertex {

        private dVertex(Model currentBoard) {
            super(currentBoard);
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



    public ImmutableList<Player> getPlayerList(Board board) {
        List<Player> allPlayers = new ArrayList<>();
        for (Piece p : board.getPlayers()) {
            // iterate through all players
            Optional<Board.TicketBoard> tempTicketBoardForEach = board.getPlayerTickets(p);
            HashMap<Ticket, Integer> theTicketMap = new HashMap<>();

            // update tickets for every player
            if (tempTicketBoardForEach.isPresent()) {
                Board.TicketBoard mid = tempTicketBoardForEach.get();
                for (Ticket t : Ticket.values()) {
                    theTicketMap.put(t, mid.getCount(t));
                }
            }
            ImmutableMap<Ticket, Integer> tempMap = ImmutableMap.copyOf(theTicketMap);

            // get player location
            // TODO: if whether getAvailableMoves is empty?
            int locationOfPlayer = board.getAvailableMoves().asList().get(0).source();

            Player temp = new Player(p, tempMap, locationOfPlayer);
            allPlayers.add(temp);
        }
        return ImmutableList.copyOf(allPlayers);
    }

    public Player getMrX(ImmutableList<Player> players) {
        Player mrX = null;
        for (Player p : players) {
            if (p.isMrX()) mrX = p;
        }
        return mrX;
    }

    public ImmutableList<Player> getDetectives(ImmutableList<Player> players) {
        List<Player> detectives = new ArrayList<>();
        for (Player p : players) {
            if (p.isDetective()) {
                detectives.add(p);
            }
        }
        return ImmutableList.copyOf(detectives);
    }


    public Move getBestMove(Tree tree, double score) {
        List<Vertex> children = new ArrayList<>(tree.getStartNode().getChildren());
        for (Vertex best : children) {
            if (score == best.score) {
                return best.move;
            }
        }
        return null;
    }

    // build a copy of current board
    public Model copyOfModel(Board board, ImmutableList<Player> players) {
        Player mrX = getMrX(players);
        ImmutableList<Player> detectives = getDetectives(players);
        return new MyModelFactory().build(board.getSetup(), mrX, detectives);
    }

    public ImmutableList<List<Move>> combinationOfMoves(Board board, ImmutableList<Player> detectives) {
        Set<Move> allMoves = board.getAvailableMoves();
        HashMap<Piece, List<Move>> groupedMoves = new HashMap<Piece, List<Move>>();
        List<List<Move>> allCombinations = new ArrayList<>();
        ArrayList<Integer> lims = new ArrayList<>();
        ArrayList<Integer> indice = new ArrayList<>();
        ArrayList<ArrayList<Integer>> combinationsOfIndex = new ArrayList<>();
        int temp;

        for (Player d : detectives) {
            groupedMoves.put(d.piece(), new ArrayList<Move>());
            indice.add(0);
        }
        for (Move move : allMoves) groupedMoves.get(move.commencedBy()).add(move);
        for (Player d : detectives) {
            lims.add(groupedMoves.get(d.piece()).size());
        }

        for (int i = 0; i < indice.size() - 1; i++) {
            combinationsOfIndex.add(new ArrayList<>(indice));
            temp = indice.get(i);
            while (temp < lims.get(i)) {
                temp++;
                indice.set(i, temp);
                combinationsOfIndex.add(new ArrayList<>(indice));
            }
        }
        //TODO: combination algor
        return ImmutableList.copyOf(allCombination);
    }

    // eliminate unnecessary and expensive move
    public ImmutableList<Move> elimination(ImmutableList<Move> moves) {
        ArrayList<Move> finalMoves = new ArrayList<>();
        ArrayList<Move> secretMoves = new ArrayList<>();
        //destinations of bus/underground moves
        ArrayList<Integer> destinations = new ArrayList<>();
        ArrayList<Move> otherMoves = new ArrayList<>();
        for (Move move : moves) {

            //only if it is secret and it is not double move
            if (move.tickets().iterator().next().equals(Ticket.SECRET) &&
                    !move.tickets().iterator().hasNext()) secretMoves.add(move);
            else {
                otherMoves.add(move);
                destinations.add(getDestination(move));
            }
        }

        for (Move move : secretMoves) {
            if (!destinations.contains(getDestination(move))) finalMoves.add(move);
        }
        finalMoves.addAll(otherMoves);

        return ImmutableList.copyOf(finalMoves);
    }

    public ImmutableList<Move> eliminationForDoubleMove(ImmutableList<Move> moves) {
        ArrayList<Move> finalMoves = new ArrayList<>();
        ArrayList<Move> doubleSecretMoves = new ArrayList<>();
        //destinations of bus/underground moves
        ArrayList<Integer> destinations = new ArrayList<>();
        ArrayList<Move> otherMoves = new ArrayList<>();
        for (Move move : moves) {
            if (move.tickets().iterator().next().equals(Ticket.SECRET) &&
                    move.tickets().iterator().next().equals(Ticket.SECRET)) {
                doubleSecretMoves.add(move);
            } else {
                otherMoves.add(move);
                destinations.add(getDestination(move));
            }
        }
        for (Move move : doubleSecretMoves) {
            if (!destinations.contains(getDestination(move))) finalMoves.add(move);
        }
        finalMoves.addAll(otherMoves);
        return ImmutableList.copyOf(finalMoves);
    }

    // TODO: how to calculate score ?
    // get score by using Dijkstra algorithm(shortest distance between mrX and detectives)
    private double scoring(Board board, int locationOfMrx, ImmutableList<Player> detectives) {
        int scoreOfBoard = 0;

        // iterate through list of detectives to find the shortest distance for each detective
        for (Player detective : detectives) {
            int detectiveLocation = detective.location();

            // using a list to store listOfUnevaluatedNodes
            List<Integer> listOfUnevaluatedNodes = new ArrayList<>(board.getSetup().graph.nodes());

            // initialize, shortest distance of other nodes are all infinity
            // references: sion's lecture & princeton java source file for Dijkstra algorithm
            // warehouseOfDistance list : mapping each node with distance to currentNode
            List<Double> warehouseOfDistance = new ArrayList<>();

            for (int i = 0; i < listOfUnevaluatedNodes.size(); i++) {
                warehouseOfDistance.set(i, Double.POSITIVE_INFINITY);
            }
            warehouseOfDistance.set(locationOfMrx, 0.0);

            // starting calculation until we find the shortest distance
            Integer currentNode = locationOfMrx;
            while (currentNode != detectiveLocation && !listOfUnevaluatedNodes.isEmpty()) {
                // select a new currentNode with shortest distance(under current circumstance)
                currentNode = nodeWithShortestDistance(warehouseOfDistance);

                List<Integer> adjNodes = new ArrayList<>(board.getSetup().graph.adjacentNodes(currentNode));

                // TODO: check if setting all distance from current node to adjacentNodes with same value(int 1) is right ?
                for(Integer node: adjNodes) {
                    // cuz all paths' weight equal to 1
                    double ValueOfExtendShortestPath = warehouseOfDistance.get(currentNode) + 1;
                    // check whether currentNode is the shortest one
                    if (ValueOfExtendShortestPath < warehouseOfDistance.get(node) && listOfUnevaluatedNodes.contains(node)) {
                        warehouseOfDistance.set(node, ValueOfExtendShortestPath);
                    }
                }
                // never go through this node again
                warehouseOfDistance.set(currentNode, Double.NEGATIVE_INFINITY);
                listOfUnevaluatedNodes.remove(currentNode);
            }
            // sum up the warehouseOfDistance
            scoreOfBoard += warehouseOfDistance.get(detectiveLocation);
        }
        // TODO: should we just use reciprocal ?
        return scoreOfBoard/((double) detectives.size());
    }


    // TODO: maybe do some optimization ?
    // find a node which got shortest distance
    private Integer nodeWithShortestDistance(List<Double> warehouseOfDistance) {
        int indexOfShortestNode = 0;
        for (int i = 0; i < warehouseOfDistance.size(); i++) {
            if (warehouseOfDistance.get(indexOfShortestNode) > warehouseOfDistance.get(i)) {
                indexOfShortestNode = i;
            }
        }
        return indexOfShortestNode;
    }

    private void ifNodeValid(List<Double> warehouseOfDistance, int v) {
        int sizeOfDistances = warehouseOfDistance.size();
        if (v < 0 || v >= sizeOfDistances)
            throw new IllegalArgumentException("error");
    }

}
