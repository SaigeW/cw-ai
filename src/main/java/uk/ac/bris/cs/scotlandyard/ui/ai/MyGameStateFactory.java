package uk.ac.bris.cs.scotlandyard.ui.ai;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.*;
import javax.annotation.Nonnull;


import uk.ac.bris.cs.scotlandyard.model.*;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Ticket;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Transport;
import uk.ac.bris.cs.scotlandyard.model.Move.*;




/**
 * cw-model
 * Stage 1: Complete this class
 */
public class MyGameStateFactory{



    public MyGameState build(
            GameSetup setup,
            Player mrX,
            ImmutableList<Player> detectives) {

        // the piece of detectives in tempSet
        List<Player> tempList = new ArrayList<Player>();
        tempList.add(mrX);
        tempList.addAll(detectives);
        ImmutableList<Player> players = ImmutableList.copyOf(tempList);

        return new MyGameState(setup, players, ImmutableList.of(), mrX, detectives);
    }

    public MyGameState build(GameSetup setup, Player mrX, ImmutableList<Player> detectives,
                             ImmutableList<Player> remaining, ImmutableList<LogEntry> log){
        List<Player> tempList = new ArrayList<>();
        tempList.add(mrX);
        tempList.addAll(detectives);
        ImmutableList<Player> players = ImmutableList.copyOf(tempList);

        return new MyGameState(setup, remaining, log, mrX, detectives);
    }


      class MyGameState {

        private final GameSetup setup;
        private ImmutableList<Player> remaining;
        private ImmutableList<LogEntry> log;
        private Player mrX;
        private final List<Player> detectives;
        private final ImmutableList<Player> everyone;
        private ImmutableSet<Move> moves;
        private ImmutableSet<Piece> winner;
        /*
         * store the current player who need to move in this round
         */
        private Player currentPlayer;

        /*
        private constructor which used by MyGameStateFactory and advanced method (as callee)
        */
        MyGameState(
                final GameSetup setup,
                final ImmutableList<Player> remaining,
                final ImmutableList<LogEntry> log,
                final Player mrX,
                final List<Player> detectives) {

            // checks for parameters
            if (setup.rounds.isEmpty()) throw new IllegalArgumentException("Rounds is empty!");

			/*
			if the hashcode of this graph is 0, which means graph is empty
			*/
            if (Objects.equals(setup.graph.hashCode(), 0)) throw new IllegalArgumentException("Graph is empty!");

            /*
            if (mrX == null) ... --- this statement cannot pass testNullMrXShouldThrow(),
             */

            if (!mrX.isMrX()) throw new IllegalArgumentException("You did not pass Mr.X in");
            if (detectives.isEmpty()) throw new IllegalArgumentException("detectives is empty!");

            var locations = new ArrayList<>(mrX.location());

			/*
			By iterate through detectives,
			also covered : MoreThanOneMrXShouldThrow() & testSwappedMrXShouldThrow()
			*/
            for (Player d : detectives) {
				/*
				check whether there is any detective holds double
				*/
                if (d.has(Ticket.DOUBLE)) throw new IllegalArgumentException("detectives cant have double");
				/*
				check whether detectives overlap each other
				*/
                if (!locations.contains(d.location())) {
                    locations.add(d.location());
                } else {
                    throw new IllegalArgumentException("detectives overlap");
                }
                if (d.has(Ticket.SECRET)) {
                    throw new IllegalArgumentException(d.piece() + " has secret cards");
                }
            }

            List<Player> copy = new ArrayList<>();
            copy.add(mrX);
            copy.addAll(detectives);

            this.setup = setup;
            this.remaining = remaining;
            this.log = log;
            this.mrX = mrX;
            this.detectives = detectives;
            this.winner = ImmutableSet.copyOf(Set.of());
            this.everyone = ImmutableList.copyOf(copy);
            HashSet<Piece> detectivesPiece = new HashSet<>();
            for (Player d : detectives) detectivesPiece.add(d.piece());

			/*
			 after update constructor, check if there is a winner (overlap or detective can not make any move)
			*/
            if (checkWinnerIsDetective(mrX, detectives)) {
                System.out.println("d win");
                this.currentPlayer = null;
                this.moves = ImmutableSet.copyOf(Set.of());
                this.winner = ImmutableSet.copyOf(detectivesPiece);
            } else if (checkWinnerIsMrX(mrX, detectives, setup)) {
                System.out.println("Mr X win");
                this.currentPlayer = null;
                this.moves = ImmutableSet.copyOf(Set.of());
                this.winner = ImmutableSet.of(mrX.piece());
            } else {
				/*
				update if it is time to go to the next round
				*/
                if (remaining.isEmpty()) {
                    this.remaining = ImmutableList.copyOf(new ArrayList<>(this.everyone));
                    this.currentPlayer = mrX;
                } else this.currentPlayer = this.remaining.get(0);

				/*
				get the current move for current player,
				then jump to next round if the rest detectives in the remaining list are unable to move
				*/
                ImmutableSet<Move> currentMoves = makeAllMoves(setup, detectives, currentPlayer, currentPlayer.location());

                /*
                check whether remaining detectives holds empty TicketBoard
                 */
                boolean restDetectiveStuck = true;
                for (Player p : remaining) {
                    if (p.isDetective())
                        restDetectiveStuck = makeAllMoves(setup, detectives, p, p.location()).isEmpty();
                }
                if (currentMoves.isEmpty() && (remaining.isEmpty() || restDetectiveStuck)) {
                    this.remaining = ImmutableList.copyOf(new ArrayList<>(this.everyone));
                    this.currentPlayer = mrX;
                }

				/*
				Update moves according to identity of current player, going to next round now
				 */
                if (currentPlayer.isDetective()) {
                    var DetectivesAllMoves = new HashSet<Move>();
                    for (Player p : remaining) {
                        if (p.isDetective())
                            DetectivesAllMoves.addAll(makeAllMoves(setup, detectives, p, p.location()));
                    }
                    this.moves = ImmutableSet.copyOf(DetectivesAllMoves);
                } else this.moves = ImmutableSet.copyOf(makeAllMoves(setup, detectives, mrX, mrX.location()));

				/*
				check whether there is a winner after entering the next round
				 */

                // if our guy is mrX
                if (currentPlayer.isMrX()) {
                    boolean NoRounds = setup.rounds.size() <= log.size();
                    // catch if mrX stuck
                    if (moves.isEmpty()) {
                        this.currentPlayer = null;
                        this.moves = ImmutableSet.copyOf(Set.of());
                        this.winner = ImmutableSet.copyOf(detectivesPiece);
                    }
                    // no rounds left for mrX
                    if (NoRounds) {
                        this.currentPlayer = null;
                        this.moves = ImmutableSet.copyOf(Set.of());
                        this.winner = ImmutableSet.of(mrX.piece());
                    }
                }
            }

        }



//         @Nonnull
//        @Override
        public GameSetup getSetup() {
            return this.setup;
        }

//        @Nonnull
//        @Override
        public ImmutableSet<Piece> getPlayers() {

            List<Player> tempArray = new ArrayList<>(detectives);
            tempArray.add(mrX);

			/*
			 iterate through the set, adding their piece to an new ImmutableSet
			 */
            Set<Piece> tempSet = new HashSet<>();
            for (Player player : tempArray) {
                tempSet.add(player.piece());
            }
            return ImmutableSet.<Piece>builder().addAll(tempSet).build();
        }

//        @Nonnull
//        @Override
        public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
            return Optional.empty();
        }

        public ImmutableList<Player> getRemaining(){return remaining;}

//        public Optional<Integer> getDetectiveLocation(Player detective) {
//			/*
//			using piece as key, compare them to get location
//			 */
//            for (Player player : this.detectives) {
//                if (Objects.equals(player.piece().toString(), detective.name())) {
//                    return Optional.of(player.location());
//                }
//            }
//            return Optional.empty();
//        }

        @Nonnull
        public Optional<Board.TicketBoard> getPlayerTickets(Piece piece) {
			/*
			return an implementation of TicketBoard
			 */
            for (Player player : this.everyone) {
                if (Objects.equals(player.piece().toString(), piece.toString())) {
                    return Optional.of(ticket -> player.tickets().get(ticket));
                }
            }
            return Optional.empty();
        }


//        @Nonnull
//        @Override
        public ImmutableList<LogEntry> getMrXTravelLog() {
            return this.log;
        }

//        @Nonnull
//        @Override
        public ImmutableSet<Piece> getWinner() {
            return this.winner;
        }


//        @Nonnull
//        @Override
        public ImmutableSet<Move> getAvailableMoves() {
            return this.moves;
        }


//        @Nonnull
//        @Override
        public MyGameStateFactory.MyGameState advance(Move move) {
            if (!remaining.contains(pieceToPlayer(move.commencedBy()))) {
                return this;
            }
            if (!moves.contains(move)) throw new IllegalArgumentException("Illegal move: " + move);
			/*
			 get the player by move and piece
			 */
            Piece which = move.commencedBy();
            Player who = pieceToPlayer(which);
			/*
			 If the player is mr.X
			 */
            var tempList = new ArrayList<>(remaining);
            tempList.remove(who);


            if (which.isMrX()) {
                List<LogEntry> newLogs = new ArrayList<>(log);
				/*
				update remaining & remove the currentPlayer(get from move), kinda like pop() of stack
				when being given/using card or updating location, a new player object will be returned
				*/
                mrX = advanceMrXHelper(who, move);
                if (isDouble(move)) {
					/*
					if it is a ticket for double move:
					firstly, let log get updated for the first destination
					then update with second move
					 */
                    boolean doFirst = true;
                    for (Ticket t : move.tickets()) {
                        if (t == Ticket.DOUBLE) continue;
                        if (doFirst) {
							/*
							check the length of log to determine if it is a round for mrX to reveal
							 */
                            if (setup.rounds.get(log.size()))
                                newLogs.add(LogEntry.reveal(t, getIntermediateDestination(move)));
                            else newLogs.add(LogEntry.hidden(t));
                            doFirst = false;
                        } else {
							/*
							if it is second move, this step is in the same round with first move
							 */
                            if (setup.rounds.get(log.size() + 1))
                                newLogs.add(LogEntry.reveal(t, destinationHelper(move)));
                            else newLogs.add(LogEntry.hidden(t));
                        }
                    }
                } else {
					/*
					Single move
					 */
                    for (Ticket t : move.tickets()) {
                        if (setup.rounds.get(log.size())) newLogs.add(LogEntry.reveal(t, destinationHelper(move)));
                        else newLogs.add(LogEntry.hidden(t));
                    }
                }
                this.log = ImmutableList.copyOf(newLogs);
            } else {
				/*
				if our guy is detective
				 */
                int indexOfWho = detectives.indexOf(who);
                who = advanceDetectiveHelper(who, move);
                List<Player> tempDetectives = new ArrayList<>(detectives);
				/*
				MrX will draw ticket from TickerBoard
				 */
                mrX = mrX.give(move.tickets());
                for (Player player : this.detectives) {
                    if (Objects.equals(player.piece(), move.commencedBy())) {
                        tempDetectives.set(indexOfWho, who);
                        return new MyGameState(setup, ImmutableList.copyOf(tempList), log, mrX, tempDetectives);
                    }
                }
            }
            return new MyGameState(setup, ImmutableList.copyOf(tempList), log, mrX, detectives);
        }


        private ImmutableSet<Move> makeAllMoves(
                GameSetup setup,
                List<Player> detectives,
                Player player,
                int source) {
            Set<Move> movesSet = new HashSet<>();
            ImmutableSet<SingleMove> singleMoves = makeSingleMoves(setup, detectives, player, source);
            ImmutableSet<DoubleMove> doubleMoves = makeDoubleMoves(setup, detectives, player, source);
            movesSet.addAll(singleMoves);
            movesSet.addAll(doubleMoves);
            return ImmutableSet.copyOf(movesSet);
        }


        public ImmutableSet<SingleMove> makeSingleMoves(
                GameSetup setup,
                List<Player> detectives,
                Player player,
                int source) {
            var singleMoves = new ArrayList<SingleMove>();
            SingleMove singleMove;

            for (int destination : setup.graph.adjacentNodes(source)) {
                if (!occupied(player, destination, detectives)) {
                    for (Transport t : Objects.requireNonNull(
                            setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
						/*
						check whether there is  ticket for this destination
						 */
                        singleMove = checkAvailabilityOfMove(player, t, player.tickets(), source, destination);
                        if (singleMove != null) {
                            singleMoves.add(singleMove);
                        }
                    }
					/*
					check whether secret card is available for this destination
					 */
                    singleMove = checkAvailabilityOfSecretMove(player, player.tickets(), source, destination);
                    if (singleMove != null) {
                        singleMoves.add(singleMove);
                    }
                }
            }
            return ImmutableSet.copyOf(singleMoves);
        }

        private ImmutableSet<DoubleMove> makeDoubleMoves(
                GameSetup setup,
                List<Player> detectives,
                Player mrX,
                int source) {
			/*
			get all possible first move
			 */
            var firstMoves = makeSingleMoves(setup, detectives, mrX, source);
            var doubleMoves = new ArrayList<DoubleMove>();
            var copyTickets = new HashMap<>(mrX.tickets());
            int destination1;


            for (SingleMove move : firstMoves) {
                destination1 = move.destination;
                for (int destination2 : setup.graph.adjacentNodes(destination1)) {
					/*
					skip it as this node is occupied
					 */
                    if (occupied(mrX, destination2, detectives)) continue;
					/*
					skip if no double
					 */
                    if (!mrX.has(Ticket.DOUBLE)) continue;
					/*
					 skip if NotEnoughRoundLeft
					 */
                    var roundStatus = new ArrayList<>(setup.rounds);
                    if (Objects.equals(roundStatus.get(0), true) && roundStatus.size() == 1) continue;

					/*
					get the tickets set after this move, modifying the number of ticket of this type
					 */
                    copyTickets.put(move.ticket, copyTickets.get(move.ticket) - 1);

					/*
					 calculate second move
					 */
                    for (Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(destination1, destination2, ImmutableSet.of()))) {
                        SingleMove move2 = checkAvailabilityOfMove(mrX, t, ImmutableMap.copyOf(copyTickets), destination1, destination2);
                        if (move2 != null) {
							/*
							make DoubleMove object from 2 move, without secret move
							 */
                            doubleMoves.add(makeDoubleMove(move, move2));
                        }
						/*
						check secret move
						 */
                        move2 = checkAvailabilityOfSecretMove(mrX, ImmutableMap.copyOf(copyTickets), destination1, destination2);
                        if (move2 != null) {
							/*
							make DoubleMove object from 2 move, with secret move
							 */
                            doubleMoves.add(makeDoubleMove(move, move2));
                        }
                    }
                    copyTickets = new HashMap<>(mrX.tickets());
                }
            }

            return ImmutableSet.copyOf(doubleMoves);
        }

        /*
         check: if destination has been occupied
         */
        private Boolean occupied(Player player, int destination, List<Player> detectives) {
            var detectivesLocations = new ArrayList<Integer>();
            for (Player d : detectives) {
                if (!d.equals(player)) {
                    detectivesLocations.add(d.location());
                }
            }
            return detectivesLocations.contains(destination);
        }

        /*
        check if player have the ticket, then return an new object of SingleMove
         */
        private SingleMove checkAvailabilityOfMove(Player player,
                                                   Transport T,
                                                   ImmutableMap<Ticket, Integer> tickets,
                                                   int source, int destination) {
            if (tickets.getOrDefault(Objects.requireNonNull(T.requiredTicket()), 0) != 0) {
                return new SingleMove(player.piece(), source, T.requiredTicket(), destination);
            } else return null;
        }

        /*
        same stuff for SecretMove
         */
        private SingleMove checkAvailabilityOfSecretMove(Player player,
                                                         ImmutableMap<Ticket, Integer> tickets,
                                                         int source, int destination) {
            if (tickets.getOrDefault(Objects.requireNonNull(Ticket.SECRET), 0) != 0) {
                return new SingleMove(player.piece(), source, Ticket.SECRET, destination);
            } else return null;
        }

        /*
        return an object of DoubleMove
         */
        private DoubleMove makeDoubleMove(SingleMove move1, SingleMove move2) {
            return new DoubleMove(mrX.piece(), move1.source(), move1.ticket, move1.destination, move2.ticket, move2.destination);
        }


        /*
        to get the second destination while passing the double move
         */
        private Integer destinationHelper(Move move) {
            Integer destination = move.visit(new Visitor<>() {
                @Override
                public Integer visit(SingleMove singleMove) {
                    return singleMove.destination;
                }

                @Override
                public Integer visit(DoubleMove doubleMove) {
                    return doubleMove.destination2;
                }
            });
            return destination;
        }

        /*
        to get the first destination while passing the double move
         */
        private Integer getIntermediateDestination(Move move) {
            return move.visit(new Visitor<>() {
                @Override
                public Integer visit(SingleMove singleMove) {
                    return -1;
                }

                @Override
                //TODO: why not just return?
                public Integer visit(DoubleMove doubleMove) {
                    return doubleMove.destination1;
                }
            });
        }

        /*
        Make mrX moving
         */
        private Player advanceMrXHelper(Player x, Move move) {
            x = x.use(move.tickets());
            x = x.at(destinationHelper(move));
            return x;
        }

        /*
        check if it is a DoubleMove
         */
        private boolean isDouble(Move move) {
            Integer isDouble = move.visit(new Visitor<>() {
                @Override
                public Integer visit(SingleMove singleMove) {
                    return 0;
                }

                @Override
                public Integer visit(DoubleMove doubleMove) {
                    return 1;
                }
            });
            return (isDouble == 1);
        }

        /*
        helper function, we need to get Player object from given piece for many times
         */
        private Player pieceToPlayer(Piece piece) {
            for (Player player : this.everyone) {
                if (player.piece().equals(piece)) return player;
            }
            return null;
        }


        /*
            Make detective moving
             */
        private Player advanceDetectiveHelper(Player detective, Move move) {
            detective = detective.use(move.tickets());
            detective = detective.at(destinationHelper(move));
            return detective;
        }


        /*
        check whether Detective is the winner when overlap happens
         */
        private boolean checkWinnerIsDetective(Player x, List<Player> detectives) {
            boolean caught = false;
            for (Player d : detectives) {
                caught = caught || d.location() == x.location();
            }
            return caught;
        }

        /*
        check whether mrX is the winner for the circumstance that detectives cannot make any move
         */
        private boolean checkWinnerIsMrX(Player x, List<Player> detectives, GameSetup setup) {
            Set<Move> allMoves = new HashSet<>();
            for (Player d : detectives) {
                allMoves.addAll(makeAllMoves(setup, detectives, d, d.location()));
            }
            return allMoves.isEmpty();
        }

        public ImmutableList<Player> getDetectivesAsPlayer(){
            return ImmutableList.copyOf(detectives);
        }

        public ImmutableList<Player> getPlayersAsPlayer(){
            return everyone;
        }

        public Player getMrX(){
            return mrX;
        }
    }
}

