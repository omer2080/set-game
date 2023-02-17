package bguspl.set.ex;

import java.util.concurrent.ArrayBlockingQueue;
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
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;


    /**
     * The dealer of the game  
     */
    private Dealer dealer;

    /**
     * The number of set the current player sent to check
     */
    private int numSetForCheck;

    
    /**
     * The keys that pressed enter the queue (the queue has 3 cells)  
     */
    protected ArrayBlockingQueue<Integer> playerPressesQueue;


    /**
     * The current number of tokens that the player holds (initiallizing with env.config.featureSize) 
     */
    private int tokensCounter;

    /**
     * true iff the player claimed a false set
     */
    volatile private long inFreeze;


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
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        tokensCounter = env.config.featureSize;
        playerPressesQueue = new ArrayBlockingQueue<>(3);
        score = 0;
        inFreeze = 0; 
        numSetForCheck=0;

    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        // main running loop of player:
        while (!terminate) {
            if (!playerPressesQueue.isEmpty()){
                pressToAct();
            }
        }
        // closing/terminating computer-thread if exists:
        if (!human){
            try { aiThread.join();} 
            catch (InterruptedException tryAgain) { // playerThread interupted from Player::terminate() before ai is closed
                try { aiThread.join();} catch (InterruptedException warning) {
                    env.logger.warning("Thread " + Thread.currentThread().getName() + " was interrupted again while waiting for aiThread to join 2nd time.");
                }
            }
        }
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated. end of player::run()");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                int randomSlot = (int)(Math.random() * env.config.tableSize);
                keyPressed(randomSlot);
                try {
                    synchronized (this) { wait(5); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated. end of aiThread::lambda");
        }, "computer-" + (id+1) + "");
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        if(!human){
            aiThread.interrupt();
        }
        playerThread.interrupt();
    }

    /**
     * This method is called when a key is pressed, and pushes the int of the slot to player presses array
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        playerPressesQueue.offer(slot);
    }

    /**
     * Pulls a key-press from player-queue and acts.
     */
    public void pressToAct() {
        Integer slotNumFromQueue = playerPressesQueue.poll(); //can be null if the queue is empty
        if (slotNumFromQueue == null){
            env.logger.warning("we pulled from queue but the queue was empty");
            return;
        }
        if(tokensCounter == 0 && table.tokensArray[slotNumFromQueue][id] == false){ // already used all tokens and tries to add another one.
                return;
        }
        boolean currentlyTokenPlaced;
        synchronized(table){
            if(table.slotToCard[slotNumFromQueue] == null) { // trying to place token on empty slot
                return;
            }
            currentlyTokenPlaced = table.flipToken(id, slotNumFromQueue);
        }
        if (currentlyTokenPlaced){ // player placed a token
            tokensCounter--;
            if (tokensCounter == 0){ //player placed his last token
                PlacedThirdToken();
            }   
        }
        else{ // player removed a token
            tokensCounter++;
        }       
    }

     /**
     * After placing third token, claims the dealer for a set, waiting for his check and handling freezes, and clears keyQueue
     */
    synchronized private void PlacedThirdToken(){
        setClaimed();
        dealer.playerClaimSet(id);
        try { wait(); } 
        catch (InterruptedException interruptedException) {Thread.currentThread().interrupt();}

        try {
            for(long time = inFreeze ;  time >= 1000 ; time -= 1000){
                env.ui.setFreeze(id,time);
                wait(1000);
            }
            env.ui.setFreeze(id,0);
        } 
        catch (InterruptedException interruptedException) { Thread.currentThread().interrupt(); }

        inFreeze = 0;  
        playerPressesQueue.clear();
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    synchronized public void point() {
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        penalty(env.config.pointFreezeMillis);
    }

    /**
     * increases the number of set the player claimed by one.
     */
    synchronized public void setClaimed() {
        numSetForCheck++;
    }

    /**
     * Penalize a player and perform other related actions.
     */
    synchronized public void penalty(long millis) {
        inFreeze = millis;
        notifyAll();
    }

    /**
     * returns the player's score
     */
    public int score() {
        return score;
    }

    /**
     * returns the number of tokens currently in the player's hands (not placed on the table).
     */
    public int getNumTokensHolding(){
        return tokensCounter;
    }

    public void returnToken(){
        tokensCounter++;
        if(tokensCounter > env.config.featureSize){
            env.logger.warning("token counter of player " + id + " is " + tokensCounter + ". It's more than he could hava.");
        }
    }

    public int numSetForCheck() {
        return numSetForCheck;
    }

}
