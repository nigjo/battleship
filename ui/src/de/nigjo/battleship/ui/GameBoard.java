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
package de.nigjo.battleship.ui;

import java.util.List;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import de.nigjo.battleship.BattleshipGame;
import de.nigjo.battleship.data.BoardData;
import de.nigjo.battleship.util.Storage;

/**
 *
 * @author nigjo
 */
public class GameBoard extends JPanel
{
  public GameBoard(BattleshipGame game)
  {
    super(new BorderLayout());
    setBackground(Color.ORANGE);

    /*
    +-------------------------------+
    | <New> <Load> <Refresh>        |
    +---------------+---------------+
    | "Player 1"    | "Player 2"    |
    +---------------+---------------+
    |I |            |I |            |
    |c |    Board   |c |    Board   |
    |o |            |o |            |
    |n |      1     |n |      2     |
    |s |            |s |            |
    +--+------------+--+------------+
     */
    JToolBar toolbar = new JToolBar("Options", JToolBar.HORIZONTAL);
    toolbar.setFloatable(false);

    List<Action> actions = ActionsManager.getDefault().getActions("Options");
    actions.forEach(toolbar::add);

    add(toolbar, BorderLayout.PAGE_START);

    JPanel players = new JPanel(new GridLayout(1, 2, 16, 4));
    players.add(createPlayerSide(game, BoardData.KEY_SELF));
    players.add(createPlayerSide(game, BoardData.KEY_OPPONENT));
    add(players, BorderLayout.CENTER);

    Storage.getDefault().get(BattleshipGame.class)
        .addPropertyChangeListener(BattleshipGame.KEY_PLAYER, pce -> repaint());
  }

  private static JPanel createPlayerSide(BattleshipGame game, String boardkey)
  {
    JPanel playerBoard = new JPanel(new BorderLayout(2, 4));

    BoardData playerData = game.getData(boardkey, BoardData.class);

    playerBoard.add(
        new JLabel(playerData.isOpponent() ? "Gegnergebiet" : "Eigene Schiffe"),
        BorderLayout.PAGE_START);
    JToolBar playerActions = new JToolBar("Actions", JToolBar.VERTICAL);
    playerBoard.add(playerActions, BorderLayout.LINE_START);
    OceanBoard ocean = new OceanBoard(playerData);
    ocean.setName(playerData.isOpponent() ? "opponent" : "self");
    playerBoard.add(ocean, BorderLayout.CENTER);

    game.addPropertyChangeListener(boardkey,
        pce -> ocean.updateBoard((BoardData)pce.getNewValue())
    );

    return playerBoard;
  }
}
