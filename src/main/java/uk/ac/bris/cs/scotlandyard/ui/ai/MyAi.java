package uk.ac.bris.cs.scotlandyard.ui.ai;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;

import uk.ac.bris.cs.scotlandyard.ui.ai.MyGameStateFactory;
import uk.ac.bris.cs.scotlandyard.ui.ai.MyGameStateFactory.MyGameState;

public class MyAi implements Ai {

    @Nonnull
    @Override
    public String name() {
        return "Ikea Shasha";
    }


    @Nonnull
    @Override
    public Move pickMove(
            @Nonnull Board board,
            Pair<Long, TimeUnit> timeoutPair) {
        // create list of players update with current status
        ImmutableList<Player> allPlayers = getPlayerList(board);
//        Player mrX = getMrX(allPlayers);
//        ImmutableList<Player> detectives = getDetectives(allPlayers);
        //System.out.format(String.valueOf(detectives));
        //System.out.format(String.valueOf(mrX));

//        System.out.println(mrX);
//        System.out.println(detectives);

        MyGameState copiedModel = copyOfModel(board, allPlayers);

        Tree tree = new Tree(copiedModel);

        int depth = 4;
        double score = minimax(tree.startNode, copiedModel, depth, true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        System.out.print(score);
        System.out.print("");

        // TODO: how to coordinate board during recursion & from score to move
// 		List<Move> moves = board.getAvailableMoves().asList();
// 		return moves.get(new Random().nextInt(moves.size()));
        return getBestMove(tree, score);
    }

    private double minimax(Vertex currentNode, MyGameState model,  int depth, boolean isMax, double alpha, double beta) {

        // currentBoard is actually a model object
        //Board currentBoard = model.getCurrentBoard();

        //If the leaves of the tree have been reached (the lowest layer which will be evaluated), calculate the score for the currentBoard.
        if (depth == 0) {

            ImmutableList<Player> players = model.getDetectivesAsPlayer();
            double score = scoring(model, model.getMrX().location(), getDetectives(players));

            currentNode.setScore(score); //Set the score stored in the node. This is required to find the best move.
            System.out.print(score);
            System.out.print("");
            return score;
        }
        // currently haven't been reach lowest level of tree
        if (isMax) {

            List<Move> moves = ImmutableList.copyOf(model.getAvailableMoves());

            if (!moves.isEmpty()) {

                // if it's mrX' turn, set maximum sore to node & update alpha.
                double scoreOfMax = Double.NEGATIVE_INFINITY;

                //elimination
                moves = elimination(moves);
                moves = eliminationForDoubleMove(moves);

                // continuing going left at first
                MyGameState nextModel;
                for (Move move : moves) {
                    // TODO: how to advance ?
                    nextModel = copyOfModel(model);
                    nextModel = nextModel.advance(move);
                    // update child node
                    Vertex child = new Vertex(nextModel);
                    child.setMove(move);
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
                double score = winnerScore(model);
                currentNode.setScore(score);
                return score;
            }
        }
        else {
            if (!model.getWinner().isEmpty()) return winnerScore(model);
            // if it's detective' turn, set minimum sore to node & update beta.
            double minScore = Double.POSITIVE_INFINITY;

            // TODO: how to generate moves with bunch of detectives?
            ImmutableList<List<Move>> combinations = combinationOfMoves(model);

            for (List<Move> combination : combinations) {
                MyGameState nextModel = copyOfModel(model);
                for (Move move : combination) {
                    nextModel = copyOfModel(model);
                    nextModel = nextModel.advance(move);
                }

                Vertex child = new Vertex(nextModel);
                currentNode.addChild(child);

                double childScore = minimax(child, nextModel, depth - 1, true, alpha, beta);
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

        public Tree(MyGameState currentBoard) {
            this.startNode = new Vertex(currentBoard);
        }

        public Vertex getStartNode() {
            return this.startNode;
        }
    }

    // father & children vertex , current state, current score, move
    private class Vertex {
        private List<Vertex> children = new ArrayList<>();
        private MyGameState currentBoard;
        private double score;
        private Move move;

        private Vertex(MyGameState currentBoard) {
            this.currentBoard = currentBoard;
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
            theTicketMap.put(Ticket.DOUBLE, board.getPlayerTickets(p).get().getCount(Ticket.DOUBLE));
            theTicketMap.put(Ticket.BUS, board.getPlayerTickets(p).get().getCount(Ticket.BUS));
            theTicketMap.put(Ticket.UNDERGROUND, board.getPlayerTickets(p).get().getCount(Ticket.UNDERGROUND));
            theTicketMap.put(Ticket.SECRET, board.getPlayerTickets(p).get().getCount(Ticket.SECRET));
            theTicketMap.put(Ticket.TAXI, board.getPlayerTickets(p).get().getCount(Ticket.TAXI));

            // get player location
            // TODO: if whether getAvailableMoves is empty?
            int locationOfPlayer = 0;
            if(p.isMrX()){locationOfPlayer = board.getAvailableMoves().iterator().next().source();}
            else {
                theTicketMap.remove(Ticket.DOUBLE);
                theTicketMap.remove(Ticket.SECRET);
                String color = p.webColour();
                switch (color) {
                    case "#f00":
                        locationOfPlayer = board.getDetectiveLocation(Piece.Detective.RED).get();
                        break;
                    case "#0f0":
                        locationOfPlayer = board.getDetectiveLocation(Piece.Detective.GREEN).get();
                        break;
                    case "#00f":
                        locationOfPlayer = board.getDetectiveLocation(Piece.Detective.BLUE).get();
                        break;
                    case "#fff":
                        locationOfPlayer = board.getDetectiveLocation(Piece.Detective.WHITE).get();
                        break;
                    case "#ff0":
                        locationOfPlayer = board.getDetectiveLocation(Piece.Detective.YELLOW).get();
                }
            }


            Player temp = new Player(p, ImmutableMap.copyOf(theTicketMap), locationOfPlayer);
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
    // TODO: debug
    public MyGameState copyOfModel(Board board, ImmutableList<Player> players) {
        Player mrX = getMrX(players);
        ImmutableList<Player> detectives = getDetectives(players);
        MyGameStateFactory factory = new MyGameStateFactory();
        return factory.build(board.getSetup(), mrX, detectives);
    }

    public MyGameState copyOfModel(MyGameState gameState){
        MyGameStateFactory factory = new MyGameStateFactory();
        return factory.build(gameState.getSetup(), gameState.getMrX(),gameState.getDetectivesAsPlayer());
    }

    public ImmutableList<List<Move>> combinationOfMoves(MyGameState board) {
        Set<Move> allMoves = board.getAvailableMoves();
        ImmutableList<Player> detectives = board.getDetectivesAsPlayer();
        HashMap<Piece, List<Move>> groupedMoves = groupedMoves(board, detectives);

        List<List<Move>> groupedMovesAsList = new ArrayList<>();
        for (Player d : detectives) {
            groupedMovesAsList.add(groupedMoves.get(d.piece()));
        }

        List<List<Move>> allCombinations =  Lists.cartesianProduct(groupedMovesAsList);
        List<ImmutableList<Move>> immutableAllCombination = new ArrayList<>();
        //change all sub-lists into immutable list
        for (List<Move> moves:allCombinations){
            immutableAllCombination.add(ImmutableList.copyOf(moves));
        }

        allCombinations = validateMove(immutableAllCombination, board, board.getPlayersAsPlayer());

        //TODO: combination algor
        return ImmutableList.copyOf(allCombinations);
    }

    public HashMap<Piece, List<Move>> groupedMoves(MyGameState board, ImmutableList<Player> detectives){
        Set<Move> allMoves = board.getAvailableMoves();
        HashMap<Piece, List<Move>> groupedMoves = new HashMap<Piece, List<Move>>();
        for (Player d : detectives) {
            groupedMoves.put(d.piece(), new ArrayList<Move>());
        }
        for (Move move : allMoves) groupedMoves.get(move.commencedBy()).add(move);
        return groupedMoves;
    }

    public List<List<Move>> validateMove(List<ImmutableList<Move>> movesList, MyGameState board, ImmutableList<Player> players){
        MyGameState model = copyOfModel(board);
        List<ImmutableList<Move>> result = new ArrayList<>();
        for (ImmutableList<Move> moves:movesList){
            for (Move move:moves){
                if (model.getAvailableMoves().contains(move)){
                    model = model.advance(move);
                }
                else break;
                result.add(moves);
            }
            model = copyOfModel(board);
        }
        return ImmutableList.copyOf(result);
   }

    // eliminate unnecessary and expensive move
    public ImmutableList<Move> elimination(List<Move> moves) {
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

    public ImmutableList<Move> eliminationForDoubleMove(List<Move> moves) {
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
    private double scoring(MyGameState board, int locationOfMrx, ImmutableList<Player> immutableDetectives) {
        List<Double> distances = new ArrayList<>();

        // check whether a detective cannot move.
        List<Player> detectives = new ArrayList<>(immutableDetectives);

        for (Player d : immutableDetectives) {
            if (board.makeSingleMoves(board.getSetup(), board.getDetectivesAsPlayer(), d, d.location()).isEmpty()) {
                detectives.remove(d);
            }
        }

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
            // store each distances
            distances.addAll(warehouseOfDistance);
        }
        // TODO: modify score with hashmap
        return baseScoreCalculator(distances, board);
    }

    private double baseScoreCalculator(List<Double> distances, MyGameState board){
        if (!board.getWinner().isEmpty()){
           return winnerScore(board);
        }
        double base = 10000;
        for(Double x : distances){
            base -= quadraticF(1/x);
        }
        List<LogEntry> logs = board.getMrXTravelLog();
        List<Boolean> rounds = board.getSetup().rounds;
        LogEntry revealedEntry = null;
        int p =logs.size()-1;
        while(p!=0){
            if (rounds.get(p)) {revealedEntry = logs.get(p); break;}
            p--;
        }
        if(revealedEntry.ticket().equals(Ticket.SECRET))
            base += 800;
        return base;
    }

    private Double winnerScore(MyGameState board){
        if(board.getWinner().contains(Piece.MrX.MRX)){
            return Double.POSITIVE_INFINITY;
            }
        else return Double.NEGATIVE_INFINITY;
    }

    private Double quadraticF(Double x){
        return 1500*x*x;
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
