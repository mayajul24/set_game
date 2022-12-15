package bguspl.set.ex;

import java.util.*;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    private Dealer dealer;

    private Queue<Integer> keyPressesTokens;
    private List<Integer> potentialSet;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.dealer = dealer;
        this.table = table;
        this.id = id;
        this.human = human;
        this.keyPressesTokens = new LinkedList<Integer>();
        this.potentialSet = new ArrayList<Integer>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            //TODO: check tokens list, add tokens to the table and check if we reached 3 tokens notify dealer and send him/her tokens
            if (keyPressesTokens.size() > 0) {
                int token = keyPressesTokens.remove();
                if (potentialSet.contains(token)) {
                    potentialSet.remove(potentialSet.indexOf(token));
                    table.removeToken(id, token);
                } else if (potentialSet.size() < 3) {
                    potentialSet.add(table.getSlotToCard()[token]);
                    table.placeToken(id, token);
                    if (potentialSet.size() == 3) {
                        dealer.checkPlayer(this);
                    }
                }
            }
        }
        if (!human) try {
            aiThread.join();
        } catch (InterruptedException ignored) {
        }
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                Random random = new Random();
                int slot = random.nextInt(11);
                keyPressed(slot);

                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignored) {
                }
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        try {
            while (table.countCards() < 12 & dealer.getDeck().size() != 0) {
                this.wait();
            }
        } catch (InterruptedException ignore) {
        }
        keyPressesTokens.add(slot);
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        score++;
        env.ui.setScore(id, score);
        try {
            env.ui.setFreeze(id, 1000);
            playerThread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement: if(playerThread.getState() != Thread.State.WAITING)
        long timer = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < timer) {
            env.ui.setFreeze(id, env.config.turnTimeoutMillis);
        }
        env.ui.setFreeze(id, -1000);
//        try {
//            env.ui.setFreeze(id,3000);
//            playerThread.sleep(3000);
//            env.ui.setFreeze(id,-1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
    }

    public int getScore() {
        return score;
    }

    public Queue<Integer> getKeyPressesTokens() {
        return keyPressesTokens;
    }

    public int getId() {
        return id;
    }

    public List<Integer> getPotentialSet() {
        return potentialSet;
    }
}
