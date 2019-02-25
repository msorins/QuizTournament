# Quiz Tournament

## Youtube demo video
[![](http://img.youtube.com/vi/Rxi7d6pLImM/0.jpg)](http://www.youtube.com/watch?v=Rxi7d6pLImM "Quiz Tournament Demo")

## Screens
![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/0.png?raw=true "Demo Image")

![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/1.png?raw=true "Demo Image")

![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/2.png?raw=true "Demo Image")

![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/3.png?raw=true "Demo Image")

![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/4.png?raw=true "Demo Image")

![Demo Image ](https://github.com/msorins/HexaXAnd0/blob/QuizTournament/5.png?raw=true "Demo Image")

# Idea
A multiplayer quizz app. Initially it should have contained only logo quizzes,but then it spread across multiple categories.

All the quizz battles happen between two players, but if another opponent doesn't join soon, silently an in-game robot is going to take over.

Users can contribute to the pool of questions directly from the application (only after the submitted quizzes are going to be evaluated from a web admin panel)

# How does it work

The server is waiting for players to join the que and then it places them into a room. Depending on the player's status and action the room is going to cycle through the following events:

* *waitingForPlayers*: until two players join the room
* *preparing*: while the phone is internally loading all the quizz data
* *ready*: everything is ready for the round to start
* *running*: in-round time
* *done*: after both players have submitted a result or the time has passed, if there have been 5 rounds, the game will finish, otherwise it will cycle back to *preparing*
* *finished*: show end game screen with final results (containg the acquired points)

Firebase is used as a real time database and the players phone are going to subscribe to changes in their assigned game room.

# Technologies used

### Mobile:
* Android ~ Java

### BackEnd:
* NodeJS
* Firebase


> The Project was realised in the 1st year of University