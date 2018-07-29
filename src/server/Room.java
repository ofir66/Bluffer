package server;


import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import game.Bluffer;
import game.Game;
import protocol.ProtocolCallback;
import tokenizer.StringMessage;
/**
 * A class represents a room in the server, contains the needed information for us to handle a room
 */
public class Room {
	
	/**
	 * The name of the room
	 */
	private final String name;
	/**
	 * Field that indicates if the room is now at play mode
	 */
	private boolean isPlaying;
	/**
	 * the players list in the room (null-> no player in it)
	 */
	private ConcurrentLinkedQueue<Player> playersList;
	/**
	 * which game is last played
	 * 
	 */
	private Game game;
	/**
	 * represents progress in the game
	 * mapped to 0 by default. if mapped to 1-> question asked. if mapped to 2-> choices were given
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
	
	/** Auxiliary method
	 * send a message to all players in this Room
	 * @param msg the message to send
	 */
	public void sendMessageToAllPlayers(String msg){
		Iterator<Player> it= this.playersList.iterator();
		while (it.hasNext()){
			try{
				it.next().getCallback().sendMessage(new StringMessage(msg));
			}
			catch(IOException e){}
		}
	}
	
	/** Auxiliary method
	 * send a message to all players in this Room, except a certain player
	 * @param msg the message to send
	 * @param player the player that we don't want to send message to
	 */
	public void sendMessageToAllPlayersExceptSender(String msg, Player player){
		Iterator<Player> it= this.playersList.iterator();
		while (it.hasNext()){
			Player iPlayer=it.next();
			try{
				if (iPlayer!=player)
					iPlayer.getCallback().sendMessage(new StringMessage(msg));
			}
			catch(IOException e){}
		}
	}
	
	/** Auxiliary method (needed for a case a player choose to bluff with correct answer. we won't allow this)
	 * MEANT ONLY FOR THE BLUFFER GAME - NEED TO BE CHANGED IF ADDING OTHER GAMES!!!
	 * @param answer the answer the player chose
	 * @return true if the answer to the last question is correct. false otherwise.
	 */
	public boolean isCorrect(String answer){
		if (game.getClass().getSimpleName().equals("Bluffer")){
			return ((Bluffer)game).isCorrect(answer);
		}
		return false;
	}
	/** Auxiliary method (needed for a case a player choose to bluff with a different player's bluff. we won't allow this)
	 * MEANT ONLY FOR THE BLUFFER GAME - NEED TO BE CHANGED IF ADDING OTHER GAMES!!!
	 * @param answer the answer the player chose
	 * @return true if the bluff (represented by answer) given by the player was already given by another player. false otherwise.
	 */
	public boolean isBluff(String answer){
		if (game.getClass().getSimpleName().equals("Bluffer")){
			return ((Bluffer)game).isBluff(answer);
		}
		return false;
	}
	
	/**
	 * The method starts a games in this Room.
	 * @param gameName the game to start
	 * @param callback represents the player who wants to start the game
	 */	
	public void startGame(String gameName,ProtocolCallback<StringMessage> callback){ 
		Iterator<Player> it = this.playersList.iterator();
		while (it.hasNext()){
			Player player =it.next();
			player.setTotalScore(0);
			player.setRoundScore(0);
		}
		this.game.startGame(gameName, callback);
	}
	
	/**
	 * Let us know if a given player has already bluffed in this round.
	 * MEANT FOR BLUFFER GAME ONLY!!!
	 * @param player the player we want to check if already bluffed
	 * @return true if the player bluffed already, false otherwise.
	 */
	public boolean hasBluffed(Player player){
		return ((Bluffer)game).hasBluffed(player);
	}
	
	/**
	 * Let us know if a given player has already selected an answer in this round.
	 * MEANT FOR BLUFFER GAME ONLY!!!
	 * @param player the player we want to check if already answered
	 * @return true if the player bluffed already, false otherwise.
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
