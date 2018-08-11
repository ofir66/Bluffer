package bgu.spl.multipleClientServer;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import bgu.spl.game.Bluffer;
import bgu.spl.game.Game;
import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;
/**
 * A class that saves information about the players, rooms, and games
 *
 */
public class DataBase {
	
	// fields
	/**
	 * Maps each player in our server to it's callback
	 */
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, Player> players;
	/**
	 * Maps each {@link Room Room} by it's name
	 */
	private ConcurrentHashMap<String, Room> rooms; 
	/**
	 * Maps a boolean value to each room (by name) by if there is a game being played on in it right now (true) or not (false)
	 */
	private ConcurrentHashMap <String, Boolean> roomsPlaying;
	/**
	 * List of all the nicks currently occupied
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
	/**
	 * The method uses synchronized on nicks so no one would be able to change it while we're taking a look.
	 * 
	 * @return all nicks that are already taken on our service
	 */
	public ConcurrentLinkedQueue<String> getNicks() {
		synchronized(nicks){
			return nicks;
		}
	}

	/**
	 * 
	 * @return the players on the server mapped by their callback
	 */
	public ConcurrentHashMap<ProtocolCallback<StringMessage>,Player> getPlayers() {
		return players;
	}

	/**
	 * 
	 * @return the rooms on the server mapped by their names
	 */
	public ConcurrentHashMap<String, Room> getRooms() {
		return rooms;
	}


	public ConcurrentHashMap<String, Boolean> getRoomsPlaying() {
		return roomsPlaying;
	}
	
	
	/**
	 * Changes a room state from playing/not playing to the other way around
	 * 
	 * @param room the name of the room we would like to change it's status
	 * @param bol the new status of the room: true if there is a game being played in it, false otherwise
	 */
	public void setRun(String room,boolean bol){
		roomsPlaying.remove(room);
		roomsPlaying.put(room, bol);
	}
	
	/**
	 * The method sends a MSG from the player (represented by his callback) to all players in his room
	 * @param callback the callback representing the client
	 * @param msg the message to send
	 */
	public void handleMSG(ProtocolCallback<StringMessage> callback, String msg){
		Player player=this.players.get(callback); // finding the player that wants to send a message
		player.getRoom().sendMessageToAllPlayersExceptSender("<USRMSG "+ player.getNick()+": "+msg+">", player); // and send the MSG to everyone
	}
	
}
