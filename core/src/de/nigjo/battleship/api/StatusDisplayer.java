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
package de.nigjo.battleship.api;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.nigjo.battleship.util.Storage;

/**
 *
 * @author nigjo
 */
public interface StatusDisplayer
{
  public static StatusDisplayer getDefault()
  {
    return Storage.getDefault().find(StatusDisplayer.class)
        .orElseGet(() -> s
        -> Logger.getLogger(StatusDisplayer.class.getName()).log(Level.INFO, "{0}", s));
  }

  public void setText(String message);

}
