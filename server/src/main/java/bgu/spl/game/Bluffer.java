package bgu.spl.game;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import bgu.spl.protocol.ProtocolCallback;
import bgu.spl.multipleClientServer.DataBase;
import bgu.spl.multipleClientServer.Player;
import bgu.spl.tokenizer.StringMessage;

public class Bluffer implements Game {

	/**
	 * The game round
	 */
	private int round;
	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>> bluffesForQuestion;
	/** 
	 * Field that saves for each question in the game, the players that already answered it 
	 */
	private ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>> peopleChoosed; 
	private ConcurrentHashMap<Integer,String> questions; 
	private ConcurrentHashMap<Integer,String> answers; 
	/**
	 * Field that represents the bluffs+real answers shuffled in a random order with lower-case characters.
	 */
	private final CopyOnWriteArrayList<String> shuffeledAnswers;
	private final Object lockTXTRESP;
	private final Object lockSELECTRESP;
	private ConcurrentLinkedQueue<Player> players;
	private DataBase database=DataBase.getInstance();
	
	public Bluffer(){
		this.bluffesForQuestion=new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();
		this.peopleChoosed= new ConcurrentHashMap<Integer, ConcurrentLinkedQueue<Player>>();
		this.questions= new ConcurrentHashMap<Integer,String>();
		this.answers=new ConcurrentHashMap<Integer,String>(); 
		this.shuffeledAnswers=new CopyOnWriteArrayList<String>();
		this.lockTXTRESP=new Object();
		this.lockSELECTRESP=new Object();
		this.players = new ConcurrentLinkedQueue<Player>();
	}
	
	public void startGame(String name, ProtocolCallback<StringMessage> callback) {
		HashMap<String,String> questions= new HashMap<String,String>();
		JsonReader jreader=null;
		JsonParser jparser = new JsonParser();
		int i;
		
		database.getPlayers().get(callback).getRoom().setGameState(0);
		database.setRun(database.getPlayers().get(callback).getRoom().getfName(),true);
		
		try {
			jreader = new JsonReader(new FileReader("bluffer.json"));
		} catch (FileNotFoundException e1) {
			return;
		}
		
        initQuestions(questions, jreader, jparser);
        i = (int)(Math.random()*questions.size());
        this.players = database.getPlayers().get(callback).getRoom().getPlayersList(); // put all the players in the room in the game players list
		clearPreviousGameData();

		for (String q: questions.keySet()){
			while (this.questions.get(i)!=null)
				i = (int)(Math.random()*questions.size());
			this.questions.put(i, q); // load question
			this.answers.put(i, questions.get(q)); // load answer
			i = (int)(Math.random()*questions.size());
		}
		if (!this.askQuestion())
			throw new IllegalStateException(" error. couldn't ask a question although the game is just starting!"); 
		database.getPlayers().get(callback).getRoom().setGameState(1);
	}

	private void clearPreviousGameData() {
		this.round = 0;
		this.bluffesForQuestion.clear(); // clear all fields from previous game
		this.peopleChoosed.clear();
		this.shuffeledAnswers.clear();
		this.questions.clear(); 
		this.answers.clear();
	}

	private void initQuestions(HashMap<String, String> questions, JsonReader jreader, JsonParser jparser) {
		JsonElement element;
		JsonObject jobject;
		JsonArray jQuestions;
		String question;
		String answer;
		
		element = jparser.parse(jreader);
        if (element.isJsonObject()){
        	jobject = element.getAsJsonObject();
        	jQuestions = jobject.get("questions").getAsJsonArray();
        	for (JsonElement jQuestion : jQuestions){
        		question = jQuestion.getAsJsonObject().get("questionText").getAsString();
        		answer = jQuestion.getAsJsonObject().get("realAnswer").getAsString();
        		questions.put(question, answer);
        	}
        }
	}
	
	private boolean askQuestion(){
		this.round++;
		if (this.round==4){
			this.round = 0;
			this.players.element().getRoom().setIsPlaying(false);
			database.setRun(this.players.element().getRoom().getfName(), false);
			return false;
		}
		else{
			this.players.element().getRoom().sendMessageToAllPlayers("<    ASKTXT "+this.questions.get(this.round-1)+"    >");
			return true;
		}
	}

	public void TXTRESP(String bluff, ProtocolCallback<StringMessage> callback) {
		Player player = database.getPlayers().get(callback);
		
		player.setLastBluufingAnswer(bluff);
		if (this.TXTRESP(player))
			player.getRoom().setGameState(2);
	}
	
	/**
	 * The method gets a player that sent TXTRESP (in TXTRESP phase of the game). if everyone has already sent their TXTRESP in that phase,
	 * the method will update the fShuffledAnswers field.
	 * @param player the player that sent a TXTRESP
	 * @return true if everyone sent their TXTRESP for current question. false otherwise
	 */
	private boolean TXTRESP(Player player){
		ConcurrentLinkedQueue<Player> playersBluffed;
		CopyOnWriteArrayList<String> bluffers=new CopyOnWriteArrayList<String>();
		
		synchronized(this.lockTXTRESP){
			playersBluffed=this.bluffesForQuestion.get(this.round); // we find the players answered so far
			if (playersBluffed==null){ // if no one chose before
				playersBluffed=new ConcurrentLinkedQueue<Player>();
				this.bluffesForQuestion.put(this.round, playersBluffed);
			}
			if (playersBluffed.contains(player))
				return false; // in case a player tries to send another TXTRESP when he already did
			playersBluffed.add(player);
			
			if (playersBluffed.size()==this.players.size()){ // if everyone sent their bluffing
				createAskChoices(player, playersBluffed, bluffers);
				return true;
			}
			else{
				return false;
			}
		}
	}

	private void createAskChoices(Player player, ConcurrentLinkedQueue<Player> playersBluffed,
			CopyOnWriteArrayList<String> bluffers) {
		
		Iterator<Player> it;
		Player iPlayer;
		String ASKCHOICES;
		
		it= playersBluffed.iterator();
		while (it.hasNext()){
			iPlayer=it.next();
			if (!this.isListContainsString(bluffers, iPlayer.getLastBluufingAnswer()))
				bluffers.add(iPlayer.getLastBluufingAnswer()); // we will not allow duplicates in our answers (it is written in the forum that we don't have to show duplicates)
		}
		ASKCHOICES=this.shuffleAnswers(bluffers); // creating the ASKCHOICES
		ASKCHOICES="<    "+ASKCHOICES+"    >";
		player.getRoom().sendMessageToAllPlayers(ASKCHOICES); // and sending to all players
	}
	
	private boolean isListContainsString(CopyOnWriteArrayList<String> list, String str){
		for (int i=0; i<list.size(); ++i){
			if (list.get(i).equals(str))
				return true;
		}
		
		return false;
	}
	
	/** Auxiliary method. 
	 * The method gets answers array as input, and shuffles them in random order+switches them to lower-case
	 * @param answers the answers to shuffle and switch to lower-case
	 * @return the choices the players now need to choose 
	 */
	private String shuffleAnswers(CopyOnWriteArrayList<String> answers){
		String ASKCHOICES="ASKCHOICES";
		
		for (int i=0; i<answers.size(); ++i){ // first we will switch to lowercase
			answers.set(i, answers.get(i).toLowerCase());
		}
		answers.add(this.answers.get(this.round-1).toLowerCase()); // then we will add the real answer
		Collections.shuffle(answers); // then shuffle
		for (int i=0; i<answers.size(); ++i){ // now we want to create the ASKCHOICES
			ASKCHOICES=ASKCHOICES+" "+i+"."+answers.get(i);
		}
		this.shuffeledAnswers.clear(); // now we also want to save the shuffle answers. we will need it later
		for(int i=0; i<answers.size(); ++i){
			this.shuffeledAnswers.add(answers.get(i));
		}
		
		return ASKCHOICES;
	}

	public void SELECTRESP(String msg, ProtocolCallback<StringMessage> callback) {
		Player player = database.getPlayers().get(callback);
		
		player.setLastChoice(Integer.parseInt(msg)); // don't need to check for exception as this method only called after checking it in the TBGP
		if (this.SELECTRESP(player)){
			if (!this.askQuestion()){ // if question wasn't asked -> the game is over.
				this.summary();
				player.getRoom().setGameState(0);
			}
			else
				player.getRoom().setGameState(1); // else - we update state to question mode
		}
	}
	
	/** Auxiliary method 
	 * @param answer the answer the player chose to the last question in the game
	 * @return true if the answer to the last question is correct. false otherwise.
	 */
	public boolean isCorrect(String answer){
		return this.answers.get(this.round-1).toLowerCase().equals(answer.toLowerCase());
	}
	
	/** Auxiliary method 
	 * @param answer the answer the player chose to the last question in the game
	 * @return true if the answer to the last question is correct. false otherwise.
	 */
	public boolean isBluff(String answer){
		Iterator<Player> it;
		
		if ( (bluffesForQuestion!=null) && (bluffesForQuestion.get(round)!=null) &&
				(!bluffesForQuestion.get(round).isEmpty()) 
			){
					it= bluffesForQuestion.get(round).iterator();
					while (it.hasNext()){
						if (it.next().getLastBluufingAnswer().toLowerCase().equals(answer.toLowerCase()))
							return true;
					}
		}
		
		return false;
	}
	
	/** 
	 * Symmetric to TXTRESP method, but instead of update the fShuffledAnswers field, it will calculate the players score.
	 */
	private boolean SELECTRESP(Player player){
		ConcurrentLinkedQueue<Player> playersChosed;
		
		synchronized(this.lockSELECTRESP){
			playersChosed=this.peopleChoosed.get(this.round); // we find the players answered so far
			if (playersChosed==null){ // if no one chose before
				playersChosed=new ConcurrentLinkedQueue<Player>();
				playersChosed.add(player);
				this.peopleChoosed.put(this.round, playersChosed);
			}
			else{
				playersChosed.add(player);
				this.peopleChoosed.put(this.round, playersChosed);
			}
			
			if (playersChosed.size()==this.players.size()){ // if now every one has chosen a choice
				player.getRoom().sendMessageToAllPlayers("<    GAMEMSG The correct answer is: "+this.answers.get(this.round-1)+"    >"); // we first send the correct answer to everyone
				calculateRoundScore();
				this.sendResultMessageToAllPlayers(); // update players with their results
				this.peopleChoosed.clear();
				return true;
			}
			else
				return false;
		}
	}

	private void calculateRoundScore() {
		Iterator<Player> i=this.players.iterator();
		Iterator<Player> k;
		Player iPlayer;
		Player kPlayer;

		while (i.hasNext()){ // for each player
			iPlayer=i.next();
			if (iPlayer.getLastChoice()==this.shuffeledAnswers.indexOf(this.answers.get(this.round-1).toLowerCase())){ // if he answered right
				iPlayer.setRoundScore(10);
				iPlayer.setTotalScore(iPlayer.getTotalScore()+10);
				iPlayer.setIsCorrectedOnLastQuestion(true);
			}
			else{
				iPlayer.setRoundScore(0);
				iPlayer.setIsCorrectedOnLastQuestion(false);
			}
		}
		
		i=this.players.iterator();
		while (i.hasNext()){ // for each player i
			iPlayer=i.next();
			k=this.players.iterator();
			while (k.hasNext()){ // for each player k, so that k!=i
				kPlayer=k.next();
				if (iPlayer!=kPlayer){
					if (this.shuffeledAnswers.get(kPlayer.getLastChoice()).equals(iPlayer.getLastBluufingAnswer().toLowerCase())){ // if player i fooled player k
						iPlayer.setRoundScore(iPlayer.getRoundScore()+5);
						iPlayer.setTotalScore(iPlayer.getTotalScore()+5);
					}
				}
			}
		}
	}
	
	private void sendResultMessageToAllPlayers(){
		Iterator<Player> it= this.players.iterator();
		Player iPlayer;
		
		while (it.hasNext()){
			iPlayer=it.next();
			try{
				if (iPlayer.isIsCorrectedOnLastQuestion())
					iPlayer.getCallback().sendMessage(new StringMessage("<GAMEMSG correct! +" +iPlayer.getRoundScore()+"pts>"));
				else
					iPlayer.getCallback().sendMessage(new StringMessage("<GAMEMSG wrong! +" +iPlayer.getRoundScore()+"pts>"));
			}
			catch(IOException e){}
		}
	}
	
	private void summary(){
		String msg = "<GAMEMSG Summary:";
		Iterator<Player> it = this.players.iterator();
		Player player;
		
		while (it.hasNext()){
			player = it.next();
			msg = msg+" "+player.getNick()+": "+player.getTotalScore()+"pts, ";
		}
		msg = msg.substring(0,msg.length()-2)+">"; // for removing the ", " in the end
		this.players.element().getRoom().sendMessageToAllPlayers(msg);
	}

	public Game create() {
		return new Bluffer();
	}
	
	/**
	 * Check if a given player has already given his TXTRESP in this round.
	 * @param player the player we want to check if already gave his TXTRESP
	 * @return true if the player gave his TXTRESP, false otherwise.
	 */
	public boolean hasBluffed(Player player){
		if ( (bluffesForQuestion!=null) && (bluffesForQuestion.get(round)!=null) )
				return bluffesForQuestion.get(round).contains(player);
		
		return false;
	}
	
	/**
	 *  Symmetric to hasBluffed method, just with SELECTRESP instead of TXTRESP
	 */
	public boolean hasSelected(Player player){
		if ( (peopleChoosed!=null) && (peopleChoosed.get(round)!=null) )
				return this.peopleChoosed.get(round).contains(player);
		
		return false;
	}
}
