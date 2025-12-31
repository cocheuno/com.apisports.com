/*
 * Copyright 2025 Carone Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.apisports.knime.football.model;

/**
 * Model class representing player statistics.
 */
public class PlayerStats {
    private final String playerName;
    private final String team;
    private final int appearances;
    private final int goals;
    private final int assists;
    private final int yellowCards;
    private final int redCards;
    
    public PlayerStats(String playerName, String team, int appearances, int goals, 
                      int assists, int yellowCards, int redCards) {
        this.playerName = playerName;
        this.team = team;
        this.appearances = appearances;
        this.goals = goals;
        this.assists = assists;
        this.yellowCards = yellowCards;
        this.redCards = redCards;
    }
    
    public String getPlayerName() { return playerName; }
    public String getTeam() { return team; }
    public int getAppearances() { return appearances; }
    public int getGoals() { return goals; }
    public int getAssists() { return assists; }
    public int getYellowCards() { return yellowCards; }
    public int getRedCards() { return redCards; }
}
