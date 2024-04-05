# Set_Card_Game

Multithreaded version of the well-known game "Set". This is an assignment part of the SPL course in my Bachelor's degree. I have implemented the "Dealer", "Table", and "Player" classes, along with game logic.

# About

Game Flow : The game contains a deck of 81 cards. Each card contains a drawing with four features (color,number, shape, shading). The game starts with 12 drawn cards from the deck that are placed on a 3x4 grid on the table. The goal of each player is to find a combination of three cards from the cards on the table that are said to make up a “legal set”. The game ends when there is no legal sets in the deck.

Supply the main thread that runs the table logics, the main thread that runs the "Dealer" that manages the game flow, and a single thread for each player.

# Installing and Running the Game

1. Download all of the files.
2. Navigate to "set-game-main\target\classes\config.properties" to change the game settings (number of human players, number of computer players, clock settings, etc). Note: Modify config.properties, not config_Original.properties.
3. Navigate to "set-game-main\src\main\java\bguspl\set\Main.java" and run the project.

# How To Play

1. At the bottom of the config.properties file, we define the 12 buttons for 2 human players as shown in the image:
<img style="max-width:200px; width:50%"  src="https://github.com/omer2080/set-game/assets/118855264/572e9362-e1a1-4cba-b214-36ca5542ecdf" alt="SreenShot1" >

The first player plays with the 12 red buttons and the second player with the 12 blue buttons. Each key represents a slot.
  
2. When you press 3 different buttons, if the 3 form a set, you receive a point. Otherwise, you incur a penalty for a few seconds (as defined in the settings).
3. If you press incorrectly, first press again the slots you pressed and wish to unpress.
4. The winner is the player with the most points when no more sets are available in the deque.

# Main features 
1.	Use locks and atomic variables to manage the threads. 
2.	Employ synchronization concepts for a "Fair" game.
3.	FFully support human players and computer players.


# Screen Shots
| | |
|:-------------------------:|:-------------------------:|
|<img style="max-width:200px; width:100%"  src="https://github.com/omer2080/set-game/assets/118855264/cdaa227c-057f-4e12-8cd6-b5034669f58d" alt="SreenShot2" >|<img style="max-width:200px; width:100%"  src="https://github.com/omer2080/set-game/assets/118855264/3ad72dbb-3996-4152-b65d-9687cb3eebbe" alt="SreenShot3" >




