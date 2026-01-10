# Teams and Players Analysis Workflow

## Use Case: Premier League Team and Player Performance Analysis

### Business Scenario

A sports analytics company wants to analyze Premier League team performance and identify key players. The analysis should answer:

1. **Team Performance Questions:**
   - Which teams have the best home vs. away form?
   - What are the goal-scoring patterns by time period?
   - Which teams have the best defensive records (clean sheets)?
   - What formations are most commonly used by top teams?

2. **Player Performance Questions:**
   - Who are the top scorers and assist leaders?
   - Which players have the most disciplinary issues?
   - How do player ratings correlate with team success?

3. **Combined Analysis:**
   - How do top-scoring players' teams perform overall?
   - Is there a correlation between team penalties and league position?

---

## Workflow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        TEAMS & PLAYERS ANALYSIS WORKFLOW                     │
└─────────────────────────────────────────────────────────────────────────────┘

┌────────────────────┐
│  API-Sports        │
│  Connector         │
│  (Auth: API Key)   │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│  Reference Data    │
│  Loader            │
│  (Load Teams: Yes) │
└─────────┬──────────┘
          │
          ├──────────────────────────────────────────────────────────────┐
          │                                                              │
          ▼                                                              ▼
┌────────────────────┐                                    ┌────────────────────┐
│     TEAMS NODE     │                                    │    PLAYERS NODE    │
│  (League: Premier  │                                    │    (Query: Top     │
│   League)          │                                    │     Scorers)       │
│  (Season: 2024)    │                                    │  (League: Premier  │
│  (Include Stats:   │                                    │   League)          │
│   Yes)             │                                    │  (Season: 2024)    │
└─────────┬──────────┘                                    └─────────┬──────────┘
          │                                                         │
          │                                                         │
          ▼                                                         ▼
┌────────────────────┐                                    ┌────────────────────┐
│  Column Filter     │                                    │  Row Filter        │
│  (Select analysis  │                                    │  (Goals > 10)      │
│   columns)         │                                    └─────────┬──────────┘
└─────────┬──────────┘                                              │
          │                                                         │
          │              ┌──────────────────────────────────────────┤
          │              │                                          │
          ▼              ▼                                          ▼
┌────────────────────────────────────┐              ┌────────────────────────┐
│           JOINER NODE              │              │   PLAYERS NODE         │
│  (Join on: Team_Name = Team)       │              │   (Query: Top Assists) │
│  Combines team stats with player   │              └────────────┬───────────┘
│  top scorers                       │                           │
└─────────────────┬──────────────────┘                           │
                  │                                              │
                  │                                              ▼
                  │                                   ┌────────────────────────┐
                  │                                   │   CONCATENATE          │
                  │                                   │   (Combine scorers     │
                  │                                   │    and assisters)      │
                  │                                   └────────────┬───────────┘
                  ▼                                                │
┌─────────────────────────────────────────────────────────────────┐
│                         ANALYSIS OUTPUTS                         │
├─────────────────────────────────────────────────────────────────┤
│  • Team Performance Dashboard (from Teams node)                  │
│  • Player Statistics Report (from Players nodes)                 │
│  • Combined Team-Player Analysis (from Joiner)                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Step-by-Step Implementation Guide

### Step 1: Set Up API Connection

**Node**: API-Sports Connector

| Setting | Value |
|---------|-------|
| API Key | Your API-Sports API key |
| Sport | Football |
| Tier | Your subscription tier (free/basic/pro/ultra) |

**Output**: API-Sports Connection (port object)

---

### Step 2: Load Reference Data

**Node**: Reference Data Loader

| Setting | Value |
|---------|-------|
| Load Teams | Yes (required for team dropdowns) |
| Cache TTL | 86400 (24 hours) |

**Output**: Reference Data (countries, leagues, seasons, teams in SQLite database)

**Performance Note**: Initial load takes several minutes as it queries teams for each league.

---

### Step 3: Query Team Data with Statistics

**Node**: Teams

| Setting | Value |
|---------|-------|
| League | Premier League (ID: 39) |
| Season | 2024 |
| Team | (leave blank for all teams) |
| Team Name Search | (leave blank) |
| Include Team Statistics | Yes (checked) |

**Output Columns (146 total)**:

**Team Information (7 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Team_ID | Integer | Unique team identifier |
| Team_Name | String | Full team name |
| Team_Code | String | 3-letter team code |
| Team_Country | String | Country name |
| Team_Founded | Integer | Year founded |
| Team_National | Boolean | Is national team |
| Team_Logo | String | URL to team logo |

**Venue Information (7 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Venue_ID | Integer | Venue identifier |
| Venue_Name | String | Stadium name |
| Venue_Address | String | Street address |
| Venue_City | String | City |
| Venue_Capacity | Integer | Seating capacity |
| Venue_Surface | String | Pitch surface type |
| Venue_Image | String | URL to venue image |

**Statistics - Form (1 column)**:
| Column | Type | Description |
|--------|------|-------------|
| Form | String | Recent form (e.g., "WWDLW") |

**Statistics - Fixtures (12 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Fixtures_Played_Home | Integer | Games played at home |
| Fixtures_Played_Away | Integer | Games played away |
| Fixtures_Played_Total | Integer | Total games played |
| Fixtures_Wins_Home | Integer | Home wins |
| Fixtures_Wins_Away | Integer | Away wins |
| Fixtures_Wins_Total | Integer | Total wins |
| Fixtures_Draws_Home | Integer | Home draws |
| Fixtures_Draws_Away | Integer | Away draws |
| Fixtures_Draws_Total | Integer | Total draws |
| Fixtures_Losses_Home | Integer | Home losses |
| Fixtures_Losses_Away | Integer | Away losses |
| Fixtures_Losses_Total | Integer | Total losses |

**Statistics - Goals For (32 columns)**:
| Column Group | Description |
|--------------|-------------|
| Goals_For_Total_* | Goals scored (home/away/total) |
| Goals_For_Average_* | Average goals per game |
| Goals_For_Minute_*_Total | Goals by 15-min periods (0-15, 16-30, etc.) |
| Goals_For_Minute_*_Percentage | Percentage of goals by period |
| Goals_For_Over_Under_* | Over/Under goal statistics |

**Statistics - Goals Against (32 columns)**:
| Column Group | Description |
|--------------|-------------|
| Goals_Against_Total_* | Goals conceded (home/away/total) |
| Goals_Against_Average_* | Average goals conceded per game |
| Goals_Against_Minute_*_Total | Goals conceded by period |
| Goals_Against_Minute_*_Percentage | Percentage by period |
| Goals_Against_Over_Under_* | Over/Under statistics |

**Statistics - Biggest (11 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Biggest_Streak_Wins | Integer | Longest winning streak |
| Biggest_Streak_Draws | Integer | Longest draw streak |
| Biggest_Streak_Losses | Integer | Longest losing streak |
| Biggest_Wins_Home | String | Biggest home win (e.g., "5-0") |
| Biggest_Wins_Away | String | Biggest away win |
| Biggest_Losses_Home | String | Worst home loss |
| Biggest_Losses_Away | String | Worst away loss |
| Biggest_Goals_For_Home | Integer | Most goals scored at home |
| Biggest_Goals_For_Away | Integer | Most goals scored away |
| Biggest_Goals_Against_Home | Integer | Most goals conceded at home |
| Biggest_Goals_Against_Away | Integer | Most goals conceded away |

**Statistics - Clean Sheets & Failed to Score (6 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Clean_Sheet_Home | Integer | Clean sheets at home |
| Clean_Sheet_Away | Integer | Clean sheets away |
| Clean_Sheet_Total | Integer | Total clean sheets |
| Failed_To_Score_Home | Integer | Games without scoring at home |
| Failed_To_Score_Away | Integer | Games without scoring away |
| Failed_To_Score_Total | Integer | Total games without scoring |

**Statistics - Penalties (5 columns)**:
| Column | Type | Description |
|--------|------|-------------|
| Penalty_Scored_Total | Integer | Penalties converted |
| Penalty_Scored_Percentage | String | Conversion rate |
| Penalty_Missed_Total | Integer | Penalties missed |
| Penalty_Missed_Percentage | String | Miss rate |
| Penalty_Total | Integer | Total penalties taken |

**Statistics - Lineups (1 column)**:
| Column | Type | Description |
|--------|------|-------------|
| Lineups | String | Formations used (e.g., "4-3-3:15, 4-4-2:5") |

**Statistics - Cards (32 columns)**:
| Column Group | Description |
|--------------|-------------|
| Cards_Yellow_Minute_*_Total | Yellow cards by 15-min periods |
| Cards_Yellow_Minute_*_Percentage | Yellow card percentage by period |
| Cards_Red_Minute_*_Total | Red cards by 15-min periods |
| Cards_Red_Minute_*_Percentage | Red card percentage by period |

---

### Step 4: Query Top Scorers

**Node**: Players (instance 1)

| Setting | Value |
|---------|-------|
| Query Type | Top Scorers |
| League | Premier League (ID: 39) |
| Season | 2024 |

**Output Columns (14)**:

| Column | Type | Description |
|--------|------|-------------|
| Player_ID | Integer | Unique player identifier |
| Name | String | Display name |
| Firstname | String | First name |
| Lastname | String | Last name |
| Nationality | String | Player nationality |
| Age | String | Player age |
| Team | String | Current team name |
| Position | String | Playing position |
| Appearances | String | Games played |
| Goals | String | Goals scored |
| Assists | String | Assists made |
| Yellow_Cards | String | Yellow cards received |
| Red_Cards | String | Red cards received |
| Rating | String | Average match rating |

---

### Step 5: Query Top Assist Providers

**Node**: Players (instance 2)

| Setting | Value |
|---------|-------|
| Query Type | Top Assists |
| League | Premier League (ID: 39) |
| Season | 2024 |

**Output**: Same 14 columns as Top Scorers query

---

### Step 6: Query Players with Disciplinary Issues

**Node**: Players (instance 3)

| Setting | Value |
|---------|-------|
| Query Type | Top Yellow Cards |
| League | Premier League (ID: 39) |
| Season | 2024 |

**Output**: Same 14 columns, sorted by Yellow_Cards descending

---

### Step 7: Query Team Squad

**Node**: Players (instance 4)

| Setting | Value |
|---------|-------|
| Query Type | Players by Team |
| Team | Manchester City (or selected team) |
| Season | 2024 |

**Output**: All squad players with their statistics

---

### Step 8: Filter and Transform Data

**Column Filter Node** (for Teams output):
- Select relevant columns for analysis
- Remove URL columns if not needed for reporting

**Row Filter Node** (for Players output):
- Filter top scorers with Goals > 10
- Filter by position (e.g., only Forwards)

---

### Step 9: Join Team and Player Data

**Joiner Node**:

| Setting | Value |
|---------|-------|
| Left Table | Teams output |
| Right Table | Top Scorers output |
| Join Column (Left) | Team_Name |
| Join Column (Right) | Team |
| Join Type | Inner Join |

**Result**: Combined table with team statistics and their top scorers

---

### Step 10: Concatenate Player Queries

**Concatenate Node**:

| Setting | Value |
|---------|-------|
| Input Tables | Top Scorers, Top Assists, Top Yellow Cards |
| Options | Union (append rows) |

**Result**: Combined player statistics table

---

## Analysis Examples

### Analysis 1: Home vs Away Performance Comparison

**Columns to Use**:
- Fixtures_Wins_Home vs Fixtures_Wins_Away
- Goals_For_Total_Home vs Goals_For_Total_Away
- Clean_Sheet_Home vs Clean_Sheet_Away

**KNIME Nodes**:
1. **Math Formula**: Calculate home_advantage = Fixtures_Wins_Home - Fixtures_Wins_Away
2. **Sorter**: Sort by home_advantage descending
3. **Bar Chart**: Visualize home vs away performance

**Expected Insight**: Identify teams with strong home advantage

---

### Analysis 2: Goal Scoring Patterns

**Columns to Use**:
- Goals_For_Minute_*_Total columns (8 time periods)
- Goals_For_Minute_*_Percentage columns

**KNIME Nodes**:
1. **Unpivoting**: Transform minute columns into rows
2. **GroupBy**: Aggregate by time period across all teams
3. **Line Chart**: Show when goals are most commonly scored

**Expected Insight**: Determine which 15-minute periods see the most goals (typically 76-90 minute period)

---

### Analysis 3: Defensive Strength Analysis

**Columns to Use**:
- Clean_Sheet_Total
- Goals_Against_Total_Total
- Goals_Against_Average_Total

**KNIME Nodes**:
1. **Sorter**: Sort by Clean_Sheet_Total descending
2. **Scatter Plot**: Plot Clean_Sheet_Total vs Goals_Against_Total_Total
3. **Statistics**: Calculate correlation

**Expected Insight**: Identify the most defensively solid teams

---

### Analysis 4: Top Scorers by Team Success

**Combined Data Required**: Join Teams + Top Scorers

**Analysis Steps**:
1. Join top scorers with their team's statistics
2. Compare Goals vs Team_Wins
3. Calculate goals-per-win ratio

**Expected Insight**: Determine if top scorers come from winning teams or carry struggling teams

---

### Analysis 5: Formation and Results Correlation

**Columns to Use**:
- Lineups (parse the formation:count pairs)
- Fixtures_Wins_Total
- Goals_For_Total_Total

**KNIME Nodes**:
1. **Cell Splitter**: Split Lineups into separate formations
2. **GroupBy**: Aggregate performance by most-used formation
3. **Statistics**: Compare win rates by formation

**Expected Insight**: Determine which formations correlate with success

---

## Advanced Workflow: Player Details Pipeline

This workflow uses the optional input port on the Players node to get detailed statistics for a filtered set of players.

```
┌────────────────────┐
│  Players Node      │
│  (Top Scorers)     │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│  Row Filter        │
│  (Goals > 15)      │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐     ┌──────────────────────┐
│  Column Filter     │     │  Reference Data      │
│  (Keep Player_ID)  │     │  Loader              │
└─────────┬──────────┘     └───────────┬──────────┘
          │                            │
          │         ┌──────────────────┤
          │         │                  │
          ▼         ▼                  ▼
┌─────────────────────────────────────────────────┐
│              Players Node (Details Mode)         │
│  Input Port 3: Table with Player_ID column       │
│  Queries detailed stats for each player          │
└─────────────────────────────────────────────────┘
```

**Use Case**: Get comprehensive statistics for only the highest-scoring players, reducing API calls.

---

## Output Data for Reporting

### Team Performance Report
Export the Teams output with statistics to CSV/Excel for:
- League standings simulation
- Performance dashboards
- Scouting reports

### Player Statistics Report
Export the Players output to CSV/Excel for:
- Top performers list
- Disciplinary tracking
- Transfer market analysis

### Combined Analysis Report
Export the joined Team-Player data for:
- Team depth analysis
- Star player impact assessment
- Recruitment prioritization

---

## Best Practices

1. **Minimize API Calls**
   - Execute Reference Data Loader once
   - Use caching (default 24-hour TTL)
   - Filter data after retrieval rather than making multiple queries

2. **Handle Missing Data**
   - Teams node returns missing cells for unavailable statistics
   - Use KNIME Missing Value node to handle nulls before analysis

3. **Performance Optimization**
   - Disable "Include Statistics" if you only need basic team info
   - Use Column Filter early to reduce data volume
   - Consider querying specific teams rather than entire leagues

4. **Data Validation**
   - Check for empty results (API may have no data for future seasons)
   - Validate join columns match exactly (Team_Name vs Team)
   - Handle percentage strings (e.g., "45%") as strings, convert if needed

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Empty Teams output | Season not started | Select a completed season |
| Statistics columns all missing | "Include Statistics" unchecked | Check the statistics option |
| Join produces no rows | Team names don't match | Check for whitespace, use String Manipulation |
| Rate limit exceeded | Too many API calls | Wait and retry, or upgrade tier |
| Player_ID not found | Player changed teams | Query by season when player was active |

---

## Related Workflows

- **Fixtures Analysis**: Use Fixtures node for match-level data
- **Standings Tracker**: Use Standings node for league tables
- **Injury Impact Analysis**: Combine with Injuries node
- **Transfer Market Analysis**: Combine with Transfers node

---

## Version Information

| Item | Value |
|------|-------|
| Document Version | 1.0 |
| Created | 2026-01-10 |
| KNIME Version | 5.5.0+ |
| API Version | v3 |
| Teams Node Output | 146 columns |
| Players Node Output | 14 columns |
