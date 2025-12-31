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
 * Model class representing a football fixture event (goal, card, etc).
 */
public class FixtureEvent {
    private final int fixtureId;
    private final int minute;
    private final String type;
    private final String team;
    private final String player;
    private final String detail;
    
    public FixtureEvent(int fixtureId, int minute, String type, String team, 
                       String player, String detail) {
        this.fixtureId = fixtureId;
        this.minute = minute;
        this.type = type;
        this.team = team;
        this.player = player;
        this.detail = detail;
    }
    
    public int getFixtureId() { return fixtureId; }
    public int getMinute() { return minute; }
    public String getType() { return type; }
    public String getTeam() { return team; }
    public String getPlayer() { return player; }
    public String getDetail() { return detail; }
}
