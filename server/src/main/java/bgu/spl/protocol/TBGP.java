package bgu.spl.protocol;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import bgu.spl.game.Bluffer;
import bgu.spl.multipleClientServer.DataBase;
import bgu.spl.multipleClientServer.Player;
import bgu.spl.multipleClientServer.Room;
import bgu.spl.tokenizer.StringMessage;

public class TBGP implements AsyncServerProtocol<StringMessage> {
  /**
  * The last response sent by the server
  */
  private String lastResponse=""; 
  private boolean connectionTerminated;
  private boolean shouldClose;

  public void processMessage(StringMessage msg, ProtocolCallback<StringMessage> callback) {
    String message=msg.getMessage();
    String command=null;
    String param="";

    if(this.connectionTerminated)
      return;

    if(message.contains(" ")){
      command=message.substring(0,message.indexOf(" "));
      try{
        param=message.substring(message.indexOf(" ")+1,message.length());
      } 
      catch (IndexOutOfBoundsException e){
        param = "";
      }
    }
    else
    command=message;

    handleCommand(callback, command, param);
  }

  private void handleCommand(ProtocolCallback<StringMessage> callback, String command, String param) {
    DataBase database = DataBase.getInstance();

    switch (command){
      case "NICK":{
        this.nick_handle(param, database, callback);
        break;
      }
      case "JOIN":{
        this.join_handle(param, database, callback);
        break;
      }
      case "MSG":{
        this.msg_handle(param, database, callback);
        break;
      }
      case "STARTGAME":{
        this.startGame_handle(database, callback);
        break;
      }
      case "TXTRESP":{
        this.txtresp_handle(param, database, callback);
        break;
      }
      case "SELECTRESP":{
        this.selectresp_handle(param, database, callback);
        break;
      }
      case "QUIT":{
        this.quit_handle(database, callback);
        break;
      }
      default:
        if (command.length()>1)
          command=command.substring(0, command.length()-1);
        this.sendMessage("<SYSMSG "+command+" UNIDENTIFIED>", callback);
    }
  }

  private void sendMessage(String response, ProtocolCallback<StringMessage> callback){
    try{
      callback.sendMessage(new StringMessage(response));
    }
    catch(IOException e) {}
  }

  @Override
  public boolean isEnd(StringMessage msg) {
    if (this.lastResponse.equals("~")){
      shouldClose = true;
      return true;
    }

    return false;
  }

  @Override
  public boolean shouldClose() {
    return this.shouldClose;
  }

  @Override
  public void connectionTerminated() {
    this.connectionTerminated=true;
  }

  private void nick_handle(String param, DataBase database, ProtocolCallback<StringMessage> callback){
    Player player;

    if (param.isEmpty())
      this.lastResponse = "<SYSMSG NICK REJECTED No parameters were given>";
    else{
      if (!database.getPlayers().containsKey(callback)){  // the database does not contain the requested player (player has no NICK)
        synchronized (database.getNicks()){
          if (!database.getNicks().contains(param)){ // nick is not taken
            database.getNicks().add(param);
            player=new Player(param, callback);
            database.getPlayers().put(callback,player);
            this.lastResponse="<SYSMSG NICK ACCEPTED>";
          }
          else // the database contains the requested nick
            this.lastResponse="<SYSMSG NICK REJECTED This NICK was already taken>";	
        }
      }
      else // the database contains this player => can't change a nick!
        this.lastResponse="<SYSMSG NICK REJECTED Already have a NICK>";
    }
    this.sendMessage(this.lastResponse, callback);
  }

  private void join_handle(String param, DataBase database, ProtocolCallback<StringMessage> callback){
    Player player;
    Room room;

    if (param.isEmpty())
      this.lastResponse = "<SYSMSG JOIN REJECTED No parameters were given>";
    else{
      player = database.getPlayers().get(callback);
      if (player!=null){ // otherwise there is no such player and he can't join a room
        room = player.getRoom();
        if (room!=null){ // if the player is already in a room, need to check if the room is currently playing
          leaveToOtherRoom(param, database, player, room);
        }
        else{ // the player is not in a room so he can join a new one, if a game has not started in it already
          enterRoom(param, database, player);
        }
      }
      else
        this.lastResponse = "<SYSMSG JOIN REJECTED Must choose a NICK before joining a room>"; // player doesn't exist
    }
    this.sendMessage(this.lastResponse, callback);
  }

  private void enterRoom(String param, DataBase database, Player player) {
    if (database.getRooms().containsKey(param)){ // if the requested room exists
      synchronized (database.getRooms().get(param)){ 
        if (database.getRooms().get(param).isPlaying()) // check if the room is in the middle of a game
          this.lastResponse = "<SYSMSG JOIN REJECTED Requested room is in the middle of a game>";
        else{ 
          player.setRoom(database.getRooms().get(param)); // sets player's room to the new room
          player.getRoom().addPlayer(player);
          this.lastResponse = "<SYSMSG JOIN ACCEPTED>";
        }
      }
    }
    else{ // room doesn't exist!
      joinNewRoom(param, database, player);
    }
  }

  private void leaveToOtherRoom(String param, DataBase database, Player player, Room room) {
    if (room.isPlaying())
      this.lastResponse = "<SYSMSG JOIN REJECTED Can't leave in the middle of a game>";
    else{ // his room isn't playing right now so he can leave
      if (database.getRooms().containsKey(param)){ // if the room exists
        synchronized (database.getRooms().get(param)){
          if (database.getRooms().get(param).isPlaying()) // check if this room in the middle of a game
            this.lastResponse = "<SYSMSG JOIN REJECTED Requested room is in the middle of a game>";
          else{ // the room exist but isn't playing!
            if (room.getfName().equals(param)) // requested room is current room
              this.lastResponse = "<SYSMSG JOIN REJECTED You are already in this room!>";
            else{ // player asks to move to a different room
              joinExistingRoom(param, database, player, room);
            }
          }
        }
      }
      else{ // room doesn't exist!
        room.removePlayer(player); // removes the player from the room object
        joinNewRoom(param, database, player);
      }
    }
  }

  private void joinNewRoom(String param, DataBase database, Player player) {
    Room newRoom = new Room(param);

    database.getRooms().put(param, newRoom); // adds the new room to the database
    database.getRoomsPlaying().put(param, false); // adds the room to the playing rooms list
    player.setRoom(newRoom); // adds the room to the player's object
    player.getRoom().addPlayer(player); // adds the player to the room's list of players
    this.lastResponse = "<SYSMSG JOIN ACCEPTED>";
  }

  private void joinExistingRoom(String param, DataBase database, Player player, Room room) {
    room.removePlayer(player); // removes the player from his room
    player.setRoom(database.getRooms().get(param)); // sets player's room to the new room
    player.getRoom().addPlayer(player); // adds the player to the room's list of players
    this.lastResponse = "<SYSMSG JOIN ACCEPTED>";
  }

  private void msg_handle(String param, DataBase database, ProtocolCallback<StringMessage> callback){
    Player player;
    Room room;

    if (param.isEmpty())
      this.lastResponse = "<SYSMSG MSG REJECTED No parameters were given>";
    else{
      player = database.getPlayers().get(callback);
      if (player!=null){
        room = player.getRoom();
        if (room!=null){
          if (!room.isPlaying()){
            this.lastResponse = "<SYSMSG MSG ACCEPTED>";
          }
          else 
            this.lastResponse = "<SYSMSG MSG REJECTED Can't send a message while playing>";
        }
        else // player is not in a room, can't send a message
          this.lastResponse = "<SYSMSG MSG REJECTED Must JOIN a room first>";
      }
      else // player doesn't have a nick
        this.lastResponse = "<SYSMSG MSG REJECTED Must JOIN a room first>";
    }
    this.sendMessage(this.lastResponse, callback);
    if (this.lastResponse.contains("ACCEPTED"))
      database.handleMSG(callback, param);
  }

  private void startGame_handle(DataBase database, ProtocolCallback<StringMessage> callback){
    Player player=database.getPlayers().get(callback);
    JsonReader jreader;

    if (player==null){ // if the user tries to STARTGAME before having a nick
      this.lastResponse = "<SYSMSG STARTGAME REJECTED Must JOIN a room first>";
    }
    else{
      if (player.getRoom()==null) //  if the user tries to STARTGAME before entering a room
        this.lastResponse = "<SYSMSG STARTGAME REJECTED Must JOIN a room first>";
      else{
        synchronized (player.getRoom()){ 
          if (player.getRoom().isPlaying())
            this.lastResponse = "<SYSMSG STARTGAME REJECTED A game has already started in this room>";
          else{ 
            try {
              jreader = new JsonReader(new FileReader("bluffer.json"));
              if ((new JsonParser().parse(jreader).getAsJsonObject()).get("questions").getAsJsonArray().size()<3)
                this.lastResponse = "<SYSMSG STARTGAME REJECTED There are not enough questions in the database to start a game>";
              else{ // means there are enough questions for bluffer
                player.getRoom().setIsPlaying(true);
                this.lastResponse =  "<SYSMSG STARTGAME ACCEPTED>";
              }
            } 
            catch (FileNotFoundException e1) {
              this.lastResponse = "<SYSMSG STARTGAME REJECTED File \""+"bluffer.json\" not found. Cannot load questions.>";
            }	
          }
        }
      }
    }
    this.sendMessage(this.lastResponse, callback);
    if (this.lastResponse.contains("ACCEPTED")){
      database.getPlayers().get(callback).getRoom().setGame(new Bluffer());
      database.setRun(database.getPlayers().get(callback).getRoom().getfName(), true);
      database.getPlayers().get(callback).getRoom().startGame("bluffer", callback);
    }
  }

  private void txtresp_handle(String param, DataBase database, ProtocolCallback<StringMessage> callback){
    Player player;
    Room room;

    if (param.isEmpty())
      this.lastResponse = "<SYSMSG TXTRESP REJECTED No parameters were given>";
    else{
      player = database.getPlayers().get(callback);
      if (player == null) // player is null means player doesn't have a nick
        this.lastResponse = "<SYSMSG TXTRESP REJECTED Must choose a NICK first>";
      else{ // player has a nick
        room = player.getRoom();
        if (room == null) 
          this.lastResponse = "<SYSMSG TXTRESP REJECTED Must JOIN a room first>";
        else{ 
          if (!room.isPlaying()) 
            this.lastResponse = "<SYSMSG TXTRESP REJECTED Cannot send a TXTRESP if not in the middle of a game>";
          else{ 
            handleTxtRespInPlayingRoom(param, player, room);
          }
        }
      }
    }

    this.sendMessage(this.lastResponse, callback);
    if (this.lastResponse.contains("ACCEPTED"))
      database.getPlayers().get(callback).getRoom().getGame().TXTRESP(param, callback);
  }

  private void handleTxtRespInPlayingRoom(String param, Player player, Room room) {
    if (room.getGameState()!=1) //wrong timing
      this.lastResponse = "<SYSMSG TXTRESP REJECTED>";
    else{ // timing is right
      if (room.isCorrect(param) || room.isBluff(param)){
        this.lastResponse = "<SYSMSG TXTRESP REJECTED This TXTRESP was already given by another player>";
      }
      else{ // player's TXTRESP is not the correct answer or a different player's TXTRESP
        if(room.hasBluffed(player)){ // player has already sent TXTRESP
          this.lastResponse = "<SYSMSG TXTRESP REJECTED You have already sent TXTRESP!>";
        }
        else
          this.lastResponse = "<SYSMSG TXTRESP ACCEPTED>";
      }
    }
  }

  private void selectresp_handle(String param, DataBase database, ProtocolCallback<StringMessage> callback){
    Player player;
    Room room;

    if (param.isEmpty())
      this.lastResponse = "<SYSMSG SELECTRESP REJECTED No parameters were given>";
    else{
      player = database.getPlayers().get(callback);
      if (player == null) // player has no NICK
        this.lastResponse = "<SYSMSG SELECTRESP REJECTED Must choose a NICK first>";
      else{ // player has a NICK
        room = player.getRoom();
        if (room == null) // player is not in a room
          this.lastResponse = "<SYSMSG SELECTRESP REJECTED Must JOIN a room first>";
        else{ 
          if (!room.isPlaying()) 
            this.lastResponse = "<SYSMSG SELECTRESP REJECTED Cannot send a SELECTRESP if not in the middle of a game>";
          else{
            param = handleSelectRspInPlayingRoom(param, player, room);
          }
        }
      }
    }

    this.sendMessage(this.lastResponse, callback);
    if (this.lastResponse.contains("ACCEPTED"))
      database.getPlayers().get(callback).getRoom().getGame().SELECTRESP(param, callback);
  }

  private String handleSelectRspInPlayingRoom(String param, Player player, Room room) {
    if (room.getGameState()!=2) // not the right time
      this.lastResponse = "<SYSMSG SELECTRESP REJECTED>";
    else{ // timing is right
      try{
        if (param.length()<1)
          this.lastResponse = "<SYSMSG SELECTRESP REJECTED Invalid answer selection. Please enter a number between 0 and "+(room.getPlayersList().size()+">");
        else{
          param=param.substring(0, param.length()-1);
          if (Integer.parseInt(param)>room.getPlayersList().size() || Integer.parseInt(param)<0) // answer is not in range
            this.lastResponse = "<SYSMSG SELECTRESP REJECTED Invalid answer selection. Please enter a number between 0 and "+(room.getPlayersList().size()+">");
          else{
            if(room.hasSelected(player)){ // player has already selected an answer
              this.lastResponse = "<SYSMSG SELECTRESP REJECTED You have already selected an answer>";
            }
            else 
              this.lastResponse = "<SYSMSG SELECTRESP ACCEPTED>";
          }
        }
      } 
      catch (NumberFormatException e){ 
        this.lastResponse = "<SYSMSG SELECTRESP REJECTED Please enter a number between 0 and "+(room.getPlayersList().size()+">");
      }
    }

    return param;
  }

  private void quit_handle(DataBase database, ProtocolCallback<StringMessage> callback){
    if (database.getPlayers().get(callback)==null){ 
      this.lastResponse = "~";
    }
    else{
      if (database.getPlayers().get(callback).getRoom()==null){ 
        this.lastResponse = "~";
        database.getNicks().remove(database.getPlayers().get(callback).getNick()); // removes the player's NICK from the list
        database.getPlayers().remove(callback); // removes the player from the database list
      }
      else{
        if (!database.getPlayers().get(callback).getRoom().isPlaying()){ 
          this.lastResponse = "~";
          database.getPlayers().get(callback).getRoom().removePlayer(database.getPlayers().get(callback)); 
          database.getNicks().remove(database.getPlayers().get(callback).getNick()); // removes the player's NICK from the list
          database.getPlayers().remove(callback); // removes the player from the database list
        }
        else
          this.lastResponse = "<SYSMSG QUIT REJECTED Cannot leave in the middle of a game>";
      }
    }

    this.sendMessage(this.lastResponse, callback);
    if (this.lastResponse.contains("~")){
      this.shouldClose = true;
    }
  }
	
}
