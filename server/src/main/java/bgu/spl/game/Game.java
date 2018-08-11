package bgu.spl.game;

import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;
/**
 * This interface represents a game on our server. Every game that we would like to add,
 * will have to implement this interface.
 */
public interface Game {
	/**
	 * This command will start a specific game (by it's name) in the room
	 * 
	 * @param name the name of the game we would like to start
	 * @param callback representing the player who sent the message
	 */
	public void startGame(String name, ProtocolCallback<StringMessage> callback);
	/**
	 * handles a TXTRESP message from player
	 * @param bluff the player's answer to a question
	 * @param callback representing the player who sent the message
	 */
	public void TXTRESP(String bluff, ProtocolCallback<StringMessage> callback);
	/**
	 * selecting respond to a msg ans return message to the user by his callback function 
	 * @param msg
	 * @param callback representing the player who sent the message
	 */
	public void SELECTRESP(String msg, ProtocolCallback<StringMessage> callback);
	
	/**
	 * will create a new instance of the game in the room who wants to play it
	 */
	public Game create();
}
