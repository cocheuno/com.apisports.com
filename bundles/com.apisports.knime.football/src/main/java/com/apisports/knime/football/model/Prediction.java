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
 * Model class representing a match prediction.
 */
public class Prediction {
    private final int fixtureId;
    private final String homeTeam;
    private final String awayTeam;
    private final String predictedWinner;
    private final double homeWinProbability;
    private final double drawProbability;
    private final double awayWinProbability;
    private final String advice;
    
    public Prediction(int fixtureId, String homeTeam, String awayTeam, String predictedWinner,
                     double homeWinProbability, double drawProbability, double awayWinProbability,
                     String advice) {
        this.fixtureId = fixtureId;
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.predictedWinner = predictedWinner;
        this.homeWinProbability = homeWinProbability;
        this.drawProbability = drawProbability;
        this.awayWinProbability = awayWinProbability;
        this.advice = advice;
    }
    
    public int getFixtureId() { return fixtureId; }
    public String getHomeTeam() { return homeTeam; }
    public String getAwayTeam() { return awayTeam; }
    public String getPredictedWinner() { return predictedWinner; }
    public double getHomeWinProbability() { return homeWinProbability; }
    public double getDrawProbability() { return drawProbability; }
    public double getAwayWinProbability() { return awayWinProbability; }
    public String getAdvice() { return advice; }
}
