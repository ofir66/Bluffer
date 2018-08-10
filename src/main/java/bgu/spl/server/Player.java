package bgu.spl.server;

import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;
/**
 * A class represents a player in the server, contains the needed information for us to handle a player
 * 
 *
 */
public class Player {
	/**
	 * the nick of the player
	 */
	private String nick;
	/**
	 * the callback unique to this player
	 */
	private ProtocolCallback<StringMessage> callback;
	/**
	 * the {@link Room} that the player is in (null if he isn't in a {@link Room})
	 */
	private Room room;
	/**
	 * Field that indicates if the player was right in his last question at a game (false at initialize)
	 */
	private boolean isCorrectedOnLastQuestion;
	/**
	 * Field that save the last bluff from the player (null at initialize)
	 */
	private String lastBluufingAnswer;
	/**
	 * The last choice the player selected in a game 
	 */
	private int lastChoice;
	/**
	 * The round score of a player in a current round in a game
	 */
	private int roundScore;
	/**
	 * The total score of a player in a current game
	 */
	private int totalScore;
	
	
	public Player (String nick, ProtocolCallback<StringMessage> callback){
		this.nick=nick;
		this.callback=callback;
		this.isCorrectedOnLastQuestion=false;
	}
	
	
	public final String getNick() {
		return nick;
	}

	public void setNick(String fNick) {
		this.nick = fNick;
	}

	public final ProtocolCallback<StringMessage> getCallback() {
		return callback;
	}

	public void setCallback(ProtocolCallback<StringMessage> fCallback) {
		this.callback = fCallback;
	}

	public final Room getRoom() {
		return room;
	}

	public void setRoom(Room fRoom) {
		this.room = fRoom;
	}

	public final boolean isIsCorrectedOnLastQuestion() {
		return isCorrectedOnLastQuestion;
	}

	public void setIsCorrectedOnLastQuestion(boolean fIsCorrectedOnLastQuestion) {
		this.isCorrectedOnLastQuestion = fIsCorrectedOnLastQuestion;
	}

	public final String getLastBluufingAnswer() {
		return lastBluufingAnswer;
	}

	public void setLastBluufingAnswer(String fLastBluufingAnswer) {
		this.lastBluufingAnswer = fLastBluufingAnswer;
	}

	public final int getLastChoice() {
		return lastChoice;
	}

	public void setLastChoice(int fLastChoice) {
		this.lastChoice = fLastChoice;
	}

	public final int getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(int fTotalScore) {
		this.totalScore = fTotalScore;
	}
	
	public final int getRoundScore() {
		return roundScore;
	}

	public void setRoundScore(int fRoundScore) {
		this.roundScore = fRoundScore;
	}

	
 }
