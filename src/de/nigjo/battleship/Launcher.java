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
package de.nigjo.battleship;


import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import de.nigjo.battleship.data.BoardData;
import de.nigjo.battleship.ui.GameBoard;
import de.nigjo.battleship.ui.StatusLine;
import de.nigjo.battleship.util.Storage;

/**
 *
 * @author nigjo
 */
public class Launcher
{

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {

    BoardData own = BoardData.generateRandom(10,
        System.currentTimeMillis(), BoardData.GAME_SIMPLE);
    Storage.getDefault().put("board.own", own);
    BoardData opponent = BoardData.generateRandom(10,
        System.currentTimeMillis() + 1l, BoardData.GAME_SIMPLE);
    opponent.setOpponent(true);
    Storage.getDefault().put("board.opponent", opponent);

    SwingUtilities.invokeLater(Launcher::createUI);
  }

  private static void createUI()
  {
    try
    {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(ReflectiveOperationException | UnsupportedLookAndFeelException ex)
    {
    }

    JFrame frame = new JFrame("Schiffe versenken");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    BoardData own = Storage.getDefault().get("board.own", BoardData.class);
    BoardData opponent = Storage.getDefault().get("board.opponent", BoardData.class);

    frame.getContentPane().add(new GameBoard(own, opponent));
    frame.getContentPane().add(StatusLine.getDefault(), BorderLayout.PAGE_END);

    JMenuBar menu = new JMenuBar();
    frame.setJMenuBar(menu);

    frame.setLocationByPlatform(true);
    frame.pack();
    frame.setVisible(true);

    StatusLine.getDefault().setText("Willkommen zu Schiffe versenken");

  }
}
