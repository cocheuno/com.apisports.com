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

import java.time.LocalDateTime;

/**
 * Model class representing a football fixture (match).
 */
public class Fixture {
    private final int id;
    private final String homeTeam;
    private final String awayTeam;
    private final LocalDateTime date;
    private final String status;
    private final Integer homeScore;
    private final Integer awayScore;
    
    public Fixture(int id, String homeTeam, String awayTeam, LocalDateTime date, 
                   String status, Integer homeScore, Integer awayScore) {
        this.id = id;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.date = date;
        this.status = status;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
    
    public int getId() { return id; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }
    public Integer getHomeScore() { return homeScore; }
    public Integer getAwayScore() { return awayScore; }
}
