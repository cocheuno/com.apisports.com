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
 * Model class representing a football league.
 */
public class League {
    private final int id;
    private final String name;
    private final String country;
    private final String type;
    private final int season;
    
    public League(int id, String name, String country, String type, int season) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.type = type;
        this.season = season;
    }
    
    public int getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getType() { return type; }
    public int getSeason() { return season; }
}
