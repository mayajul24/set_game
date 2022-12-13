package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    private Queue<Player> playersQueue;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playersQueue =  new LinkedList<Player>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();

        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
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
    private void removeSet(List<Integer> slots)
    {
        for(int slot: slots)
        {
            table.removeCard(slot);
            for(Player player: players)
            {
                List<Integer>tokens = player.getTokens();
                if(tokens.contains(slot))
                {
                    tokens.remove(slot);
                    table.removeToken(player.getId(),slot);
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        Integer[]slotToCard = table.getSlotToCard();
        for(int i=0;i<slotToCard.length;i++)
        {
            if(slotToCard[i]==null)
            {
                //TODO: randomCard
                int randomCard = randomCard();
                table.placeCard(randomCard,i);
                deck.remove(randomCard);
            }
        }
    }
    public int randomCard()
    {
        Random random = new Random();
        int randomCard = random.nextInt(deck.size()-1);
        return randomCard;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }

    public void checkSet(Player player)
    {
        int[]cards = new int[3];
        int i=0;
        //get player's cards and delete their tokens
        for(int token: player.getTokens()) {
            cards[i] = table.slotToCard[token];
            table.removeToken(player.getId(),token);
            i++;
        }
        //clear player's actions:
        player.getTokens().clear();

        boolean isSet = env.util.testSet(cards);

        if(isSet)
        {
            removeSet(player.getTokens());
            player.point();
            placeCardsOnTable();
        }
        else
        {
            player.penalty();
        }
    }
    public void checkMe(Player player){
        playersQueue.add(player);
        while(!playersQueue.isEmpty()){
            Player currentPlayer = playersQueue.remove();
            checkSet(currentPlayer);
        }

    }
}
