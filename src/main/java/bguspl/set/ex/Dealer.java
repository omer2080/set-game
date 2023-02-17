package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
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
    private final Thread[] playersThreads;
    private final ArrayBlockingQueue<Integer> waitForCheckQueue;  

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    protected volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * Reference for the thread of the dealer
     */
    Thread dealerThread;

    /**
     * holding the time in millis when the timer should be updated at
     */
    private long nextUpdateTime;

    /**
     * indicates if a set exists on the table, only when the end of game is approaching
     */
    private boolean setExists = true;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        playersThreads = new Thread[players.length];
        waitForCheckQueue = new ArrayBlockingQueue<>(players.length, true);

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        dealerThread = Thread.currentThread();
        // generate players threads and run them:
        for(int i=0; i<players.length ; i++){
            playersThreads[i] = new Thread(players[i], "Player-" + Integer.toString(i+1)); 
            playersThreads[i].start();
        }

        env.ui.setCountdown(env.config.turnTimeoutMillis,false);

        while (!shouldFinish()) {
            synchronized(table){ placeCardsOnTable(); }
            updateTimerDisplay(true);
            timerLoop();
            if(!terminate){ // for faster termination
                updateTimerDisplay(true);
                synchronized(table){ removeAllCardsFromTable(); }
            }
        }

        if(!terminate){
            for(int i = 0 ; i<env.config.players ; i++)
                env.logger.info("player number " + (i+1) + " claimed a set: " + players[i].numSetForCheck() + " times!");
            for(int i = 0 ; i<env.config.players ; i++)
                env.logger.info("player number " + (i+1) + " has " + players[i].score() + " points.");
            
            announceWinners();
            for(Player player : players){
                if(player != null) player.terminate();
            }
        }
        
        // gracefully closing all threads:
        for(Thread playerThread : playersThreads){
            try{playerThread.join();} 
            catch(InterruptedException ignored){
                env.logger.warning("dealer thread was interrupted while waiting for " + playerThread.getName() + "to join:" + ignored.getMessage());
            }
        }
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime && setExists) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;
        dealerThread.interrupt();
       
        // closing and joining players threads:
        for(Player player : players){
            if(player != null) player.terminate();
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
        while(!waitForCheckQueue.isEmpty() && !terminate){
            int playerTested = waitForCheckQueue.poll();
            if(players[playerTested].getNumTokensHolding() != 0){ // not needed to be in queue anymore, got his token back before
                synchronized(players[playerTested]){
                    players[playerTested].notifyAll();
                }
                continue; // to next while() itteration
            }
            int[] cardsToTest = new int[env.config.featureSize];
            int ind = 0; // number of cards found with player's token
            for(int i=0 ; i < table.slotToCard.length && ind<env.config.featureSize; i++){
                if(table.tokensArray[i][playerTested]){
                    cardsToTest[ind] = table.slotToCard[i];
                    ind++;
                }
            }
            // legal set:
            if(env.util.testSet(cardsToTest)){ 
                players[playerTested].point();
                for(int card : cardsToTest){
                    int slot = table.cardToSlot[card];
                    synchronized(table){
                        table.removeCard(slot);
                        removeTokens(slot);
                    }
                    updateTimerDisplay(true);
                }
            }
            // not a set:
            else{
                players[playerTested].penalty(env.config.penaltyFreezeMillis);
            }
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    protected void removeAllCardsFromTable() {
        for (Integer slot = 0 ; slot < table.slotToCard.length && !terminate ; slot++){
            if (table.slotToCard[slot] != null){
                deck.add(table.slotToCard[slot]);
                table.removeCard(slot);
                removeTokens(slot);
            }
        }
    }
    
    /**
     * removes all tokens that are placed on the card in this slot
     */
    protected void removeTokens(int slot){
        for(int playerId = 0; playerId < players.length ; playerId++){
            if(table.removeToken(playerId,slot)){
                players[playerId].returnToken();
            }
        }
    }
    
    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    void placeCardsOnTable() {
        boolean cardWasPlaced = false;
        for (Integer i=0; i<table.slotToCard.length && !terminate ; i++){
            if(deck.isEmpty()){
                List<Integer> cardsOnTableList = convertToCleanList(table.slotToCard);
                if(env.util.findSets(cardsOnTableList, 1).size() == 0)
                        setExists = false;
                return; 
            }
            if (table.slotToCard[i] == null){ // emptySlot
                int randIndex = (int) (Math.random() * deck.size());
                int randCard = deck.remove(randIndex); // random card from the deck
                table.placeCard(randCard, i);
                cardWasPlaced = true;
            }
        }
        if(cardWasPlaced && env.config.hints)
            table.hints();
    }

    private List<Integer> convertToCleanList(Integer[] array) {
        List<Integer> list = new LinkedList<Integer>();
        for (Integer i : array) {
            if (i != null)
                list.add(i);
        }
        return list;
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    synchronized private void sleepUntilWokenOrTimeout() {
        try {
            wait(Math.max(nextUpdateTime-System.currentTimeMillis(), 0));
        }
        catch(InterruptedException InterruptedException){}        

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        boolean warn;
        if(reset){
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            warn = (reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis);
            env.ui.setCountdown(env.config.turnTimeoutMillis , warn);
            if(warn) nextUpdateTime = System.currentTimeMillis() + 10;
            else nextUpdateTime = System.currentTimeMillis() + 1000;
        }
        else if(nextUpdateTime - System.currentTimeMillis() <= 0 ){ // need to update now:
            long newCountDown = reshuffleTime - nextUpdateTime;
            warn = (newCountDown <= env.config.turnTimeoutWarningMillis);
            env.ui.setCountdown(newCountDown,warn);
            if(warn) nextUpdateTime += 49;
            else nextUpdateTime += 1000;
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = -1; //so if all players have 0 points, it will be tie  
        for (int i = 0; i < players.length ; i++){ //only updating the winner's score
            if (players[i].score() > maxScore){
                maxScore = players[i].score();
            }
        }
        int numOfWinners = 0;
        for (int i = 0; i < players.length ; i++){ //only checking the number of winners
            if (players[i].score() == maxScore){
                numOfWinners++;
            }
        }
        int[] winnersArray = new int[numOfWinners];
        int counter = 0;
        for (int i = 0; i < players.length ; i++){ //puts the players' ids in an array
            if (players[i].score() == maxScore){
                winnersArray[counter] = i;
                counter++;
            }
        }
        env.ui.announceWinner(winnersArray);
    }


    synchronized protected void playerClaimSet(int playerId){
        try{waitForCheckQueue.put(playerId);}
        catch(InterruptedException exception){};
        notifyAll(); // wakeup dealer
    }
}
