package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.*;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = 0;

    private BlockingQueue<Player> playersQueue;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playersQueue = new LinkedBlockingQueue<Player>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        for (Player player : players) {
            Thread playerThread = new Thread(player);
            playerThread.start();
        }

        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis + 1000;
        while (!terminate && System.currentTimeMillis() <= reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            placeCardsOnTable();
        }
        for(Player player: players){
            synchronized (player){
                player.notifyAll();
            }
        }
    }
    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        long timer = System.currentTimeMillis() + env.config.endGamePauseMillies;
        while (timer >= System.currentTimeMillis()) {
        }
        terminate = true;
        for (Player player : players) {
            player.terminate();
        }
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
    }

    protected void removeSet(Player player) {
        System.out.println(player.id + " player set: " + Arrays.toString(player.getPotentialSet()));
        System.out.println(" set: " + Arrays.toString(table.getSlotToCard()));

        for (int i = 0; i < 3; i++) {
                int card = player.getPotentialSet()[i];
                int slot = table.getCardToSlot()[card];
                for (Player p : players) {
                    if (p.getId() != player.getId()) {
                        System.out.println("player " + p.id +" contains card " + card+ " :" +p.potentialSetContains(card));
                        System.out.println(Arrays.toString(p.getPotentialSet()));
                        if (p.potentialSetContains(card)) {

                            table.removeToken(p.getId(), slot);
                            System.out.println("before " + card +" was removed from " +p.id+": "+ Arrays.toString(p.getPotentialSet()));
                            p.removeFromPotentialSet(card);
                            System.out.println("after " + card +" was removed from " +p.id+": "+ Arrays.toString(p.getPotentialSet()));
                        }
                    }
                    else {
                            table.removeToken(player.getId(), slot);
                    }
                }

                    table.removeCard(slot);


            }
            player.clearSet();

    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        Integer[] slotToCard = table.getSlotToCard();
        boolean placedCards = false;
        for (int i = 0; i < slotToCard.length; i++) {
            if (slotToCard[i] == null) {
                if (deck.size() != 0) {
                    table.placeCard(deck.remove(0), i);
                    placedCards = true;
                }
            }
        }
//        if(env.config.hints==true&placedCards)
//        {
//            table.hints();
//        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement


            Player playerToCheck = null;
            try {

                playerToCheck = playersQueue.poll(1000, TimeUnit.MILLISECONDS);
                if(playerToCheck != null){

                    checkSet(playerToCheck);
                        synchronized (playerToCheck){
                            playerToCheck.notifyAll();
                        }

                }

            }

            catch (InterruptedException e) {}



    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset) {
            reshuffleTime = env.config.turnTimeoutMillis + System.currentTimeMillis();
        } else {
            long currentTime = System.currentTimeMillis();
            env.ui.setCountdown(Math.round((reshuffleTime - currentTime + 1000) / 1000) * 1000, env.config.turnTimeoutWarningMillis >= reshuffleTime - currentTime + 1000);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for (int i = 0; i < 12; i++) {
            if (table.slotToCard[i] != null) {
                int card = table.slotToCard[i];
                table.removeCard(i);
                deck.add(card);
                for (Player player : players) {
                    if (player.potentialSetContains(card)) {
                        player.removeFromPotentialSet(card);
                        table.removeToken(player.getId(), i);
                    }
                }
            }
        }
    }

    public void removeAllCardsFromTableTest() {
        removeAllCardsFromTable();
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        List<Integer> winnersList = new ArrayList<Integer>();
        int maxScore = 0;
        int winnerId = 0;
        for (int i = 0; i < players.length; i++) {
            int playerScore = players[i].score();
            if (playerScore > maxScore) {
                maxScore = playerScore;
                winnerId = i;
            }
        }
        winnersList.add(winnerId);

        for (int i = 0; i < players.length; i++) {
            int playerScore = players[i].score();
            if (i != winnerId & playerScore == players[winnerId].score()) {
                winnersList.add(i);
            }
        }
        int[] winnersArray = new int[winnersList.size()];
        for (int i = 0; i < winnersArray.length; i++) {
            winnersArray[i] = winnersList.get(i);
        }
        env.ui.announceWinner(winnersArray);
    }

    public void checkSet(Player player) {
        if (player.getPotentialSetSize() == 3) {
            boolean isSet = env.util.testSet(player.getPotentialSet());

            if (isSet) {
                //clear player's actions:
                System.out.println("checked set of "+player.getId());
                removeSet(player);
                player.setFrozenState(1);
                placeCardsOnTable();
                updateTimerDisplay(true);
            } else {
                player.setFrozenState(3);
            }

            }
        synchronized (player){
            player.notifyAll();
        }
        }



//    public void checkPlayer(Player player) {
//        if (!playersQueue.contains(player)) {
//            synchronized (player) {
//                playersQueue.add(player);
//                try{player.wait();} catch (InterruptedException e){}
//            }
//        }
//    }

    public List<Integer> getDeck() {
        return deck;
    }

    public synchronized BlockingQueue<Player> getPlayersQueue() {
        return playersQueue;
    }


}
