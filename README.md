# Set_Card_Game

Multithreaded version of the well-known game "Set". This is an assignment part of the SPL course in my Bachelor's degree. I have implemented the "Dealer", "Table", and "Player" classes, along with game logic.

# About

game Flow : The game contains a deck of 81 cards. Each card contains a drawing with four features (color,number, shape, shading). The game starts with 12 drawn cards from the deck that are placed on a 3x4 grid on the table. The goal of each player is to find a combination of three cards from the cards on the table that are said to make up a “legal set”. The game ends when there is no legal sets in the deck.

Supply the main thread that runs the table logics, the main thread that runs the "Dealer" that manages the game flow, and a single thread for each player.


# Main features 
1.	Use locks and atomic variables to manage the threads. 
2.	Employ synchronization concepts for a "Fair" game.
3.	FFully support human players and computer players.


# Screen Shots
| | |
|:-------------------------:|:-------------------------:|
|<img style="max-width:200px; width:100%"  src="![Set Game Image 1 ](https://github.com/omer2080/set-game/assets/118855264/cdaa227c-057f-4e12-8cd6-b5034669f58d)" alt="SreenShot1" >|<img style="max-width:200px; width:100%"  src="https://user-images.githubusercontent.com/101994161/210243154-e6b83830-a6ad-4c05-8855-7c7bc95fa58a.png" alt="SreenShot2" >




