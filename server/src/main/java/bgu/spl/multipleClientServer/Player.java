package bgu.spl.multipleClientServer;

import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.tokenizer.StringMessage;

public class Player {

	private String nick;
	/**
	 * the callback that is unique to this player
	 */
	private ProtocolCallback<StringMessage> callback;
	/**
	 * the {@link Room} that the player is in (null if he isn't in a {@link Room})
	 */
	private Room room;
	private boolean isCorrectedOnLastQuestion;
	private String lastBluufingAnswer;
	/**
	 * The last choice that the player selected in a game 
	 */
	private int lastChoice;
	/**
	 * The round score of the player in the current game he is playing (if isn't playing - this field value is meaningless)
	 */
	private int roundScore;
	/**
	 * The total score of the player in the last game played by him (if didn't play a game - this field value is meaningless)
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
