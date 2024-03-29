/*
 * Copyright 2023 nigjo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.nigjo.battleship.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.nigjo.battleship.BattleshipGame;
import static de.nigjo.battleship.BattleshipGame.*;
import de.nigjo.battleship.api.StatusDisplayer;
import de.nigjo.battleship.data.BoardData;
import de.nigjo.battleship.data.KeyManager;
import de.nigjo.battleship.data.Savegame;

/**
 *
 * @author nigjo
 */
public class StateObserver implements PropertyChangeListener
{
  private final BattleshipGame game;

  public StateObserver(BattleshipGame game)
  {
    this.game = game;
  }

  @Override
  public void propertyChange(PropertyChangeEvent pce)
  {
    Object stateValue = pce.getNewValue();
    Logger.getLogger(StateObserver.class.getName()).log(Level.FINE,
        "next state: {0}", stateValue);
    if(!(stateValue instanceof String))
    {
      return;
    }

    int playerSelf = game.getDataInt(KEY_PLAYER_NUM, 0);

    switch((String)stateValue)
    {
      case "BattleshipGame.gamestate.init":
        break;
      case STATE_PLACEMENT:
        //Es wird darauf gewartet dass die eigenen Schiffe platziert sind.
        //Der ShipsPlacer muss am Ende "BattleshipGame.storeOwnBoard()" aufrufen.
        game.putData(KEY_PLAYER, PLAYER_SELF);
        break;
      case STATE_WAIT_START:
        //Lokal sind die Schiffe platziert.
        //Pruefen, ob beide Spieler ein "volles" Brett haben.
        Savegame sg = game.getData(Savegame.class);
        boolean player1set = sg.records(1, Savegame.Record.BOARD).findFirst().isPresent();
        boolean player2set = sg.records(2, Savegame.Record.BOARD).findFirst().isPresent();
        if(player1set && player2set)
        {
          if(playerSelf == 1)
          {
            game.putData(KEY_PLAYER, PLAYER_SELF);
            game.updateState(STATE_ATTACK);
          }
          else
          {
            game.putData(KEY_PLAYER, PLAYER_OPPONENT);
            game.updateState(STATE_WAIT_ATTACK);
          }
        }
        else
        {
          Logger.getLogger(StateObserver.class.getName())
              .log(Level.FINE, "missing at least one board");
        }
        break;

      case STATE_ATTACK:
        //Es soll ein Schuss erfolgen.
        //Wird in AttackSelection behandelt.
        game.putData(KEY_PLAYER, PLAYER_SELF);
        break;
      case STATE_WAIT_ATTACK:
        //Warten auf einen Schuss
        StatusDisplayer.getDefault().setText(
            "Warte auf einen Schuß aus dem Gegenergebiet.");
        game.putData(KEY_PLAYER, PLAYER_OPPONENT);
        break;
      case STATE_ATTACKED:
      {
        //TODO: Ergebnis pruefen -> Selber oder nochmal warten.
        game.putData(KEY_PLAYER, PLAYER_SELF);
        checkAttack(playerSelf);

        //TODO: Alle Schiffe getroffen? -> Ende
        break;
      }
      case STATE_WAIT_RESPONSE:
        //Schuss ist erfolgt. Warten auf das Ergebnis
        game.putData(KEY_PLAYER, PLAYER_OPPONENT);
        break;
      case STATE_RESPONSE:
        //TODO: Ergebnis pruefen -> Nochmal Schuss oder warten auf Gegener.
        //TODO: Alle Schiffe getroffen? -> Ende
        boolean hit;
        {
          Savegame savegame = game.getData(Savegame.class);
          Savegame.Record rec = savegame.getLastRecord();
          if(!Savegame.Record.RESULT.equals(rec.getKind()))
          {
            throw new IllegalStateException("last action was no attack");
          }
          String encoded = rec.getPayload();
          KeyManager km = game.getData(KeyManager.KEY_MANAGER_SELF, KeyManager.class);
          String payload = km.decode(encoded);
          String[] split = payload.split(",");
          hit = Boolean.parseBoolean(split[2]);
          game.updateState(hit ? STATE_ATTACK : STATE_WAIT_ATTACK);
        }
        game.putData(KEY_PLAYER, hit ? PLAYER_SELF : PLAYER_OPPONENT);
        break;
      case STATE_FINISHED:
        game.putData(KEY_PLAYER, "none");
        break;
    }
    //repaint();
  }

  private void checkAttack(int playerSelf)
  {
    Savegame savegame = game.getData(Savegame.class);
    Savegame.Record rec = savegame.getLastRecord();
    if(!Savegame.Record.ATTACK.equals(rec.getKind()))
    {
      throw new IllegalStateException("last action was no attack");
    }
    String encoded = rec.getPayload();
    KeyManager km = game.getData(KeyManager.KEY_MANAGER_SELF, KeyManager.class);
    String payload = km.decode(encoded);
    String[] split = payload.split(",");
    BoardData data = game.getData(BoardData.KEY_SELF, BoardData.class);
    int[] pos =
    {
      Integer.parseInt(split[0]), Integer.parseInt(split[1])
    };
    boolean hit = data.shootAt(pos[0], pos[1]);
    String message = "Schuß auf "
        + Character.toString('A' + pos[0]) + (pos[1] + 1)
        + ", " + (hit ? "Treffer" : "Daneben");
    Logger.getLogger(BattleshipGame.class.getName())
        .log(Level.INFO, "{0}", message);

    StatusDisplayer.getDefault().setText(message);
    savegame.addRecord(Savegame.Record.MESSAGE, playerSelf, message);

    KeyManager other = game.getData(KeyManager.KEY_MANAGER_OPPONENT, KeyManager.class);
    String response = payload + "," + hit;
    savegame.addRecord(Savegame.Record.RESULT, 3 - playerSelf, other.encode(response));

    if(hit)
    {
      game.updateState(STATE_WAIT_ATTACK);
    }
    else
    {
      game.updateState(STATE_ATTACK);
    }
  }

  public static void updateState(BattleshipGame game)
  {
    Logger.getLogger(BattleshipGame.class.getName())
        .log(Level.FINER, "updating next state from current game state");
    int selfId = game.getDataInt(KEY_PLAYER_NUM, 0);
    Savegame savegame = game.getData(Savegame.class);
    if(savegame == null)
    {
      Logger.getLogger(StateObserver.class.getName())
          .log(Level.WARNING, "no savegame defined");
      game.updateState("BattleshipGame.gamestate.init");
      return;
    }

    Savegame.Record lastAction = savegame.getLastRecord();
    switch(lastAction.getKind())
    {
      case Savegame.Record.ATTACK:
        if(selfId == lastAction.getPlayerid())
        {
          // Es wurde auf uns geschossen. Treffer pruefen.
          game.updateState(STATE_ATTACKED);
        }
        else
        {
          //Wir haben geschossen. Warten auf Antwort.
          game.updateState(STATE_WAIT_RESPONSE);
        }
        break;
      case Savegame.Record.RESULT:
        if(selfId == lastAction.getPlayerid())
        {
          //Ergebnis unseres Schusses
          game.updateState(STATE_RESPONSE);
        }
        else
        {
          //Wir haben unser Ergebnis gesendet

          //TODO: Wie kann ich erkennen, dass wir dran sind?
          String[] result = savegame
              .getAttack(lastAction,
                  game.getData(KeyManager.KEY_MANAGER_SELF, KeyManager.class));
          boolean lastAttackWasHit = Boolean.parseBoolean(result[2]);
          if(lastAttackWasHit)
          {
            game.updateState(STATE_WAIT_ATTACK);
          }
          else
          {
            game.updateState(STATE_ATTACK);
          }
        }
        break;
      case Savegame.Record.PLAYER:
        if(selfId == lastAction.getPlayerid())
        {
          game.updateState(STATE_PLACEMENT);
        }
        else
        {
          game.updateState(STATE_WAIT_START);
        }
        break;
      case Savegame.Record.BOARD:
        // Player1 wartet auf Player2 oder Player 2 wartet auf den ersten Schuss.
        game.updateState(STATE_WAIT_START);
        break;
      default:
        Logger.getLogger(StateObserver.class.getName())
            .log(Level.INFO, "unknown last record: {0}, player {1}",
                new Object[]
                {
                  lastAction.getKind(),
                  lastAction.getPlayerid()
                });
        game.updateState(STATE_WAIT_START);
        break;
    }
  }

}
