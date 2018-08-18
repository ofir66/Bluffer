package bgu.spl.multipleClientServer;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.game.Bluffer;
import bgu.spl.game.Game;
import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;
/**
 * DataBase saves information about the players, rooms, and games
 *
 */
public class DataBase {
	
	// fields
	/**
	 * Maps each player in the server to it's callback
	 */
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, Player> players;
	/**
	 * Maps each {@link Room Room} by it's name
	 */
	private ConcurrentHashMap<String, Room> rooms; 
	/**
	 * Maps for each room (by name) a boolean value, which is true if and only if a game is being played in it right now
	 */
	private ConcurrentHashMap <String, Boolean> roomsPlaying;
	/**
	 * List of all the taken nicknames
	 */
	private ConcurrentLinkedQueue<String> nicks;

	
	private static class DataBaseHolder {
	      private static DataBase instance = new DataBase();
	}
			    
	private DataBase(){ 
		this.players = new ConcurrentHashMap<ProtocolCallback<StringMessage>, Player>();
		this.rooms = new ConcurrentHashMap<String, Room>();
		this.roomsPlaying = new ConcurrentHashMap<String, Boolean>();
		this.nicks = new ConcurrentLinkedQueue<String>();
	}
	
	public static DataBase getInstance() {
		 return DataBaseHolder.instance;
	}
	
	// methods

	public ConcurrentLinkedQueue<String> getNicks() {
		synchronized(nicks){
			return nicks;
		}
	}

	public ConcurrentHashMap<ProtocolCallback<StringMessage>,Player> getPlayers() {
		return players;
	}

	public ConcurrentHashMap<String, Room> getRooms() {
		return rooms;
	}


	public ConcurrentHashMap<String, Boolean> getRoomsPlaying() {
		return roomsPlaying;
	}
	
	public void setRun(String room,boolean bol){
		roomsPlaying.remove(room);
		roomsPlaying.put(room, bol);
	}
	
	/**
	 * Sends a MSG from the player (represented by his callback) to all the players in his room
	 * @param callback the callback representing the player
	 * @param msg the message to send
	 */
	public void handleMSG(ProtocolCallback<StringMessage> callback, String msg){
		Player player=this.players.get(callback); // finding the player that wants to send a message
		player.getRoom().sendMessageToAllPlayersExceptSender("<USRMSG "+ player.getNick()+": "+msg+">", player); // and send the MSG to everyone
	}
	
}
