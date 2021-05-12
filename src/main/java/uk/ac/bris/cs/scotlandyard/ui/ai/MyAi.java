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


        MyGameState copiedModel = copyOfModel(board, allPlayers);

        Tree tree = new Tree(copiedModel);

        int depth = 2;
        Storage x = minimax(tree.startNode, copiedModel, depth, true, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);

        // TODO: how to coordinate board during recursion & from score to move
// 		List<Move> moves = board.getAvailableMoves().asList();
// 		return moves.get(new Random().nextInt(moves.size()));
        //return getBestMove(tree, score);
        return x.getMove();
    }

    private Storage minimax(Vertex currentNode, MyGameState model,  int depth, boolean isMax, double alpha, double beta) {

        // currentBoard is actually a model object
        //Board currentBoard = model.getCurrentBoard();
        List<Move> moves = ImmutableList.copyOf(model.getAvailableMoves());
        if (moves.isEmpty()) {
            double score = winnerScore(model);
            currentNode.setScore(score);
            return new Storage(score, null);
        }
        Move bestMove = moves.get(0);
        //If the leaves of the tree have been reached (the lowest layer which will be evaluated), calculate the score for the currentBoard.
        if (depth == 0) {

            ImmutableList<Player> players = model.getDetectivesAsPlayer();
            double score = scoring(model, model.getMrX().location(), getDetectives(players));
            currentNode.setScore(score); //Set the score stored in the node. This is required to find the best move.

            return new Storage(score, null);
        }
        // currently haven't been reach lowest level of tree
        if (isMax) {

            //List<Move> moves = ImmutableList.copyOf(model.getAvailableMoves());

            //if (!moves.isEmpty()) {

            // if it's mrX' turn, set maximum sore to node & update alpha.
            double scoreOfMax = Double.NEGATIVE_INFINITY;

            //elimination
            moves = elimination(moves);
//                moves = eliminationForDoubleMove(moves);

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
                Storage x = minimax(child, nextModel, depth - 1, false, alpha, beta);
                double scoreOfChild = x.score;

                // TODO: check whether it is right to do
                // ATTENTION! starting execute rest of code inside bracket from the second lowest level of tree
                //scoreOfMax = Math.max(scoreOfMax, scoreOfChild);
                if(scoreOfChild > scoreOfMax){
                    scoreOfMax = scoreOfChild;
                    bestMove = move;
                }
                currentNode.setScore(scoreOfMax);

                // update alpha, cuz it's mrX' turn
                alpha = Math.max(alpha, scoreOfChild);

                // Alpha Beta Pruning, if beta(minimum upper bound) and alpha(maximum lower bound)
                // do not have intersection any more, no need to continue recursion
                    if (beta <= alpha) {
                        break;
                    }
            }
            if(depth == 2){
                return new Storage(scoreOfMax, bestMove);
            }
            return new Storage(scoreOfMax, null);
        }
        else {
            // if it's detective' turn, set minimum sore to node & update beta.
            double minScore = Double.POSITIVE_INFINITY;

            // TODO: how to generate moves with bunch of detectives?
            ImmutableList<List<Move>> combinations = combinationOfMoves(model);

            MyGameState nextModel;
            boolean invalidMove;
            for (List<Move> combination : combinations) {
                nextModel = copyOfModel(model);
                invalidMove = false;
                for (Move move : combination) {
                    if (nextModel.getAvailableMoves().isEmpty()){break;}
                    if (!nextModel.getAvailableMoves().contains(move)){
                        invalidMove = true;
                        break; }
                    nextModel = nextModel.advance(move);
                }
                if (invalidMove) continue;

                Vertex child = new Vertex(nextModel);
                currentNode.addChild(child);

                Storage x = minimax(child, nextModel, depth - 1, true, alpha, beta);
                double childScore = x.score;
                minScore = Math.min(minScore, childScore); //Maintain the minimum score of the child nodes.
                currentNode.setScore(minScore);

                // Alpha Beta Pruning, if beta(minimum upper bound) and alpha(maximum lower bound)
                // do not have intersection any more, no need to continue recursion
                beta = Math.min(beta, childScore);
                if (beta <= alpha) {
                    break;
                }
            }
            return new Storage(minScore, null);
        }
    }

    private class Storage{
        private final Double score;
        private final Move move;

        public  Storage(Double score, Move move){
            this.score = score;
            this.move = move;
        }

        public Move getMove(){
            return move;
        }

        public Double getScore(){
            return score;
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
        System.out.println(score);
        return null;
    }

    // build a copy of current board
    // TODO: debug
    public MyGameState copyOfModel(Board board, ImmutableList<Player> players) {
        Player mrX = getMrX(players);
        ImmutableList<Player> detectives = getDetectives(players);
        MyGameStateFactory factory = new MyGameStateFactory();
        ImmutableList<LogEntry> log = board.getMrXTravelLog();
        return factory.build(board.getSetup(), mrX, detectives, log);
    }

    public MyGameState copyOfModel(MyGameState gameState){
        MyGameStateFactory factory = new MyGameStateFactory();
        return factory.build(gameState.getSetup(), gameState.getMrX(),gameState.getDetectivesAsPlayer(),
                gameState.getRemaining(), gameState.getMrXTravelLog());
    }

    public ImmutableList<List<Move>> combinationOfMoves(MyGameState board) {
        ImmutableList<Player> detectives = board.getDetectivesAsPlayer();
        HashMap<Piece, List<Move>> groupedMoves = groupedMoves(board);

        List<List<Move>> groupedMovesAsList = new ArrayList<>();
        for (Player d : detectives) {
            if(!groupedMoves.get(d.piece()).isEmpty()){
            groupedMovesAsList.add(groupedMoves.get(d.piece())); }
        }

        List<List<Move>> allCombinations =  Lists.cartesianProduct(groupedMovesAsList);
        List<ImmutableList<Move>> immutableAllCombination = new ArrayList<>();

        //change all sub-lists into immutable list
        for (List<Move> moves:allCombinations){
            immutableAllCombination.add(ImmutableList.copyOf(moves));
        }

        //allCombinations = validateMove(immutableAllCombination, board, board.getPlayersAsPlayer());

        //TODO: combination algor
        return ImmutableList.copyOf(immutableAllCombination);
    }

    public HashMap<Piece, List<Move>> groupedMoves(MyGameState board){
        ImmutableList<Player> detectives = board.getDetectivesAsPlayer();
        Set<Move> allMoves = board.getAvailableMoves();
        HashMap<Piece, List<Move>> groupedMoves = new HashMap<Piece, List<Move>>();
        for (Player d : detectives) {
            groupedMoves.put(d.piece(), new ArrayList<Move>());
        }
        for (Move move : allMoves) groupedMoves.get(move.commencedBy()).add(move);

        return groupedMoves;
    }


    // eliminate unnecessary and expensive move
    public ImmutableList<Move> elimination(List<Move> moves) {
        ArrayList<Move> finalMoves = new ArrayList<>();
        ArrayList<Move> secretMoves = new ArrayList<>();

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


    // TODO: how to calculate score ?
    // get score by using Dijkstra algorithm(shortest distance between mrX and detectives)
    private double scoring(MyGameState board, int locationOfMrx, ImmutableList<Player> immutableDetectives) {
        List<Double> distances = new ArrayList<>();
        double tempDistance = 0;

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


            for(int i = 0; i <= listOfUnevaluatedNodes.size(); i++){
                warehouseOfDistance.add(Double.POSITIVE_INFINITY);
            }

            //System.out.println(warehouseOfDistance.size());

            warehouseOfDistance.set(locationOfMrx, 0.0);

            // starting calculation until we find the shortest distance
            Integer currentNode = locationOfMrx;

            // iterating through all nodes while there are nodes that unevaluated
            while (currentNode != detectiveLocation && (!listOfUnevaluatedNodes.isEmpty())) {
                // select a new currentNode with shortest distance(under current circumstance)
                currentNode = nodeWithShortestDistance(warehouseOfDistance);

                // TODO: if there is no adjNodes ?
                List<Integer> adjNodes = new ArrayList<>(board.getSetup().graph.adjacentNodes(currentNode));

                for(Integer node: adjNodes) {
                    // cuz all paths' weight equal to 1
                    double ValueOfExtendShortestPath = warehouseOfDistance.get(currentNode) + 1;

                    // check whether currentNode is the shortest one
                    //System.out.printf("node number  %d%n", node);
                    if (ValueOfExtendShortestPath < warehouseOfDistance.get(node) && listOfUnevaluatedNodes.contains(node)) {
                        warehouseOfDistance.set(node, ValueOfExtendShortestPath);
                    }
                }
                // never go through this node again
                warehouseOfDistance.set(currentNode, Double.POSITIVE_INFINITY);
                listOfUnevaluatedNodes.remove(currentNode);
            }
            // store each distances
            // TODO: debug, separate distances for each detective
            distances.addAll(warehouseOfDistance);
            tempDistance += warehouseOfDistance.get(detectiveLocation);
        }
          return baseScoreCalculator(distances, board);
    }

    private double baseScoreCalculator(List<Double> distances, MyGameState board){
        if (!board.getWinner().isEmpty()){
            return winnerScore(board);
        }
        double base = 10000000;
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
        if (revealedEntry != null){
            if(revealedEntry.ticket().equals(Ticket.SECRET))
                base += 800;}

        //save double move
        base += board.getMrX().tickets().get(Ticket.DOUBLE)*10000;


        return base;
    }

    private Double winnerScore(MyGameState board){
        if(board.getWinner().contains(Piece.MrX.MRX)){
            System.out.println("mr X win");
            return Double.POSITIVE_INFINITY;
        }
        else {
            System.out.println("detectives win");
            return Double.NEGATIVE_INFINITY;}
    }

    private Double quadraticF(Double x){
        return 150000*x*x;
    }


    // TODO: maybe do some optimization ?
    // find the index of node which got shortest distance as the next node
    private Integer nodeWithShortestDistance(List<Double> warehouseOfDistance) {
        int indexOfShortestNode = 0;
        for (int i = 0; i < warehouseOfDistance.size(); i++) {
            if (warehouseOfDistance.get(indexOfShortestNode) > warehouseOfDistance.get(i)) {
                indexOfShortestNode = i;
            }
        }
        return indexOfShortestNode;
    }
}
