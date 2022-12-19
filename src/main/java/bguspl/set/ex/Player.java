package bguspl.set.ex;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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

    private BlockingQueue<Integer> keyPressesTokens;
    private int[] potentialSet;

    private int frozenState;

    private int potentialSetSize;

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
        this.keyPressesTokens = new LinkedBlockingQueue<Integer>();
        this.potentialSet = new int[3];
        for(int i=0; i<3; i++){
            potentialSet[i] = -1;
        }
        this.frozenState = 0;
        this.potentialSetSize = 0;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) {
            createArtificialIntelligence();
        }

        while (!terminate) {
            //TODO: check tokens list, add tokens to the table and check if we reached 3 tokens notify dealer and send him/her tokens
            //check if player is frozen:
            if (frozenState == 1) {
                point();
                frozenState = 0;
            }

            if (frozenState == 3) {
                penalty();
                frozenState = 0;
            }

            if (keyPressesTokens.size() > 0) {
                System.out.println("in the first if");
                int token = keyPressesTokens.remove();
                if (table.slotToCard[token] != null) {
                    System.out.println("in the second if");
                    int card = table.getSlotToCard()[token];
                    if (potentialSetContains(card)) {
                        System.out.println("in the contains");
                        removeFromPotentialSet(card);
                        table.removeToken(id, token);
                    } else if (potentialSetSize < 3) {
                        addToPotentialSet(card);
                        table.placeToken(id, token);
                        if (potentialSetSize == 3) {
                            dealer.checkPlayer(this);
                        }
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
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
        try {
            playerThread.join();
        } catch (InterruptedException e) {
        }

    }
        /**
         * This method is called when a key is pressed.
         *
         * @param slot - the slot corresponding to the key pressed.
         */
        public void keyPressed ( int slot){
            if (frozenState == 0) {
                if (table.slotToCard[slot] != null && keyPressesTokens.size() < 3) {
                    keyPressesTokens.add(slot);
                    System.out.println("pressed");
                }
            }
        }


        /**
         * Award a point to a player and perform other related actions.
         *
         * @post - the player's score is increased by 1.
         * @post - the player's score is updated in the ui.
         */
        public void point () {
            // TODO implement
            env.ui.setScore(id, ++score);
            long timer = System.currentTimeMillis() + 2000;
            while (System.currentTimeMillis() < timer - 1000) {
                env.ui.setFreeze(id, timer - System.currentTimeMillis());
            }
            env.ui.setFreeze(id, -1000);


            int ignored = table.countCards(); // this part is just for demonstration in the unit tests

        }

        /**
         * Penalize a player and perform other related actions.
         */
        public void penalty () {
            // TODO implement: if(playerThread.getState() != Thread.State.WAITING)

            long timer = System.currentTimeMillis() + 4000;
            while (System.currentTimeMillis() < timer - 1000) {
                env.ui.setFreeze(id, timer - System.currentTimeMillis());
            }
            env.ui.setFreeze(id, -1000);
        }

        public int score() {
            return score;
        }

        public Queue<Integer> getKeyPressesTokens () {
            return keyPressesTokens;
        }

        public int getId () {
            return id;
        }

        public synchronized int[] getPotentialSet () {
            return potentialSet;
        }

        public void setFrozenState ( int i){
            this.frozenState = i;
        }


    public synchronized int getPotentialSetSize(){
        return potentialSetSize;
    }

    public synchronized void addToPotentialSet(int card){
    //    System.out.println("set before add: "+ Arrays.toString(getPotentialSet()));
    //    System.out.println("size before remove: "+potentialSetSize);
        getPotentialSet()[potentialSetSize] = card;
        potentialSetSize++;
    //    System.out.println("set after add: "+ Arrays.toString(getPotentialSet()));
    //    System.out.println("size after remove: "+potentialSetSize);
    }

    public synchronized void removeFromPotentialSet(int card){
     //   System.out.println("set before remove: "+ Arrays.toString(getPotentialSet()));
     //   System.out.println("size before remove: "+potentialSetSize);
        boolean found = false;
        for(int i =0 ; i<3 & !found ; i++){
            if (getPotentialSet()[i] == card){
                found = true;

                for(int j = i; j<2;j++){
                    getPotentialSet()[j] = getPotentialSet()[j+1];
                }
                getPotentialSet()[2] = -1;
                potentialSetSize--;
            }
        }
     //   System.out.println("set after remove: "+ Arrays.toString(getPotentialSet()));
     //   System.out.println("size after remove: "+potentialSetSize);
    }

    public synchronized boolean potentialSetContains(int card){
        for(int i = 0; i< 3; i++){
            if(getPotentialSet()[i] == card){
                return true;
            }
        }
        return false;
    }
    public synchronized void clearSet()
    {
        for (int i = 0; i < 3; i++) {
            potentialSet[i] = -1;
        }
        potentialSetSize = 0;
    }
}