package bgu.spl.multipleClientServer;


import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.game.Bluffer;
import bgu.spl.game.Game;
import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;
/**
 * A class represents a room in the server, contains the needed information for us to handle a room
 */
public class Room {
	
  private final String name;
  /**
  * Indicates if the room is now at play mode
  */
  private boolean isPlaying;
  private ConcurrentLinkedQueue<Player> playersList;
  /**
  * Last game that was played in this room
  * 
  */
  private Game game;
  /**
  * Represents progress in a game.
  * if gameState == 0, no game is being played in this room right now. 
  * if gameState == 1, A game is being played and questions were being asked now. 
  * otherwise, gameState == 2 which means that a game is being played and answer choices were given now.
  */
  private int gameState;

  public Room(String name){
    this.name=name;
    this.isPlaying=false;
    this.playersList= new ConcurrentLinkedQueue<Player>();
    this.game = null;
    this.gameState = 0;
  }

  public void addPlayer(Player player) {
    this.playersList.add(player);
  }

  public void removePlayer(Player player){
    this.playersList.remove(player);
  }

  public void sendMessageToAllPlayers(String msg){
    Iterator<Player> it= this.playersList.iterator();

    while (it.hasNext()){
      try{
        it.next().getCallback().sendMessage(new StringMessage(msg));
      }
      catch(IOException e){}
    }
  }

  public void sendMessageToAllPlayersExceptSender(String msg, Player player){
    Iterator<Player> it= this.playersList.iterator();
    Player iPlayer;

    while (it.hasNext()){
      iPlayer=it.next();
      try{
        if (iPlayer!=player)
          iPlayer.getCallback().sendMessage(new StringMessage(msg));
      }
      catch(IOException e){}
    }
  }

  /**
  * Auxiliary method for bluffer game only. Change this method if add support for other games.
  * @param answer the answer that the player chose
  * @return true if the answer to the last question is correct. False otherwise.
  */
  public boolean isCorrect(String answer){
    if (game.getClass().getSimpleName().equals("Bluffer")){
      return ((Bluffer)game).isCorrect(answer);
    }

    return false;
  }
  /**
  * Auxiliary method for bluffer game only. Change this method if add support for other games.
  * @param answer the answer that the player chose
  * @return true if the bluff (represented by answer) given by the player was already given by another player. False otherwise.
  */
  public boolean isBluff(String answer){
    if (game.getClass().getSimpleName().equals("Bluffer")){
      return ((Bluffer)game).isBluff(answer);
    }

    return false;
  }

  /**
  * Starts a games in this room.
  * @param gameName the game to start
  * @param callback represents the player who wants to start the game
  */	
  public void startGame(String gameName,ProtocolCallback<StringMessage> callback){ 
    Iterator<Player> it = this.playersList.iterator();
    Player player;

    while (it.hasNext()){
      player =it.next();
      player.setTotalScore(0);
      player.setRoundScore(0);
    }
    this.game.startGame(gameName, callback);
  }

  /**
  * Auxiliary method for bluffer game only.
  * @param player the player to check if already bluffed
  * @return true if the player bluffed already, false otherwise.
  */
  public boolean hasBluffed(Player player){
    return ((Bluffer)game).hasBluffed(player);
  }

  /**
  * Auxiliary method for bluffer game only.
  * @param the player to check if already answered
  * @return true if the player answered already, false otherwise.
  */
  public boolean hasSelected(Player player){
    return ((Bluffer)game).hasSelected(player);
  }


  public final boolean isPlaying() {
    return isPlaying;
  }

  public void setIsPlaying(boolean isPlay){
    this.isPlaying=isPlay;
  }

  public final ConcurrentLinkedQueue<Player> getPlayersList() {
    return playersList;
  }

  public final String getfName() {
    return name;
  }

  public void setGame(Game toSet){
    this.game = toSet;
  }

  public Game getGame(){
    return this.game;
  }

  public int getGameState(){
    return this.gameState;
  }

  public void setGameState(int state){
    this.gameState=state;
  }
	
}
