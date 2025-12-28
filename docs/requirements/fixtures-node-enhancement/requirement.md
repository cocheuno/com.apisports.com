# Fixtures Node Enhancement Requirement

## Priority
**HIGH** - Critical functionality is missing despite UI checkboxes existing

## Summary
The Fixtures node currently has 14 output columns with basic fixture information. The node has checkboxes for "Include Events", "Include Lineups", and "Include Statistics" but **these checkboxes don't actually fetch or parse any data**. This requirement is to:

1. Complete the missing base fixture fields (from `/fixtures` endpoint)
2. Implement the events functionality (from `/fixtures/events` endpoint)
3. Implement the lineups functionality (from `/fixtures/lineups` endpoint)
4. Implement the statistics functionality (from `/fixtures/statistics` endpoint)
5. Add support for player statistics (from `/fixtures/players` endpoint)

## Current Implementation Status

### Current Output (14 columns):
1. Fixture_ID
2. Date
3. Status
4. League_ID
5. League_Name
6. Season
7. Round
8. Home_Team_ID
9. Home_Team
10. Away_Team_ID
11. Away_Team
12. Home_Goals
13. Away_Goals
14. Venue

### Existing Settings (Partially Implemented):
- `m_includeEvents` - **CHECKBOX EXISTS BUT DOES NOTHING**
- `m_includeLineups` - **CHECKBOX EXISTS BUT DOES NOTHING**
- `m_includeStatistics` - **CHECKBOX EXISTS BUT DOES NOTHING**

## Required Changes

### Phase 1: Complete Base Fixture Fields (from fixtures-response.json)

Expand the base `/fixtures` endpoint parsing to include ALL available fields:

**Fixture Section (add 8 fields):**
- Referee (String)
- Timezone (String)
- Timestamp (Long)
- Period_First (Long)
- Period_Second (Long)
- Status_Short (String) - currently only using Status (long)
- Status_Elapsed (Int)
- Status_Extra (Int) - for extra time

**Venue Section (add 2 fields):**
- Venue_ID (Int) - currently missing
- Venue_City (String) - currently missing

**League Section (add 4 fields):**
- League_Country (String)
- League_Logo (String)
- League_Flag (String)
- League_Standings (Boolean)

**Teams Section (add 4 fields):**
- Home_Team_Logo (String)
- Away_Team_Logo (String)
- Home_Team_Winner (Boolean)
- Away_Team_Winner (Boolean)

**Score Section (add 8 fields):**
- Halftime_Home (Int)
- Halftime_Away (Int)
- Fulltime_Home (Int)
- Fulltime_Away (Int)
- Extratime_Home (Int) - nullable
- Extratime_Away (Int) - nullable
- Penalty_Home (Int) - nullable
- Penalty_Away (Int) - nullable

**Phase 1 Total: 26 new columns (14 → 40 columns)**

### Phase 2: Implement Events Functionality

When `m_includeEvents` checkbox is checked, fetch data from `/fixtures/events` endpoint for each fixture and create **flattened event columns**.

**Approach**: Since events are arrays and KNIME tables are flat, we need to aggregate event data into summary columns:

**Goals (6 columns):**
- Goals_Home_Scorers (String) - comma-separated: "Player1 (25'), Player2 (67')"
- Goals_Home_Assists (String) - comma-separated assist names
- Goals_Home_Times (String) - comma-separated minutes
- Goals_Away_Scorers (String)
- Goals_Away_Assists (String)
- Goals_Away_Times (String)

**Cards (8 columns):**
- Yellow_Cards_Home (Int) - count
- Yellow_Cards_Home_Players (String) - comma-separated
- Yellow_Cards_Away (Int)
- Yellow_Cards_Away_Players (String)
- Red_Cards_Home (Int)
- Red_Cards_Home_Players (String)
- Red_Cards_Away (Int)
- Red_Cards_Away_Players (String)

**Substitutions (4 columns):**
- Substitutions_Home_Count (Int)
- Substitutions_Home_Details (String) - "PlayerOut→PlayerIn (75'), ..."
- Substitutions_Away_Count (Int)
- Substitutions_Away_Details (String)

**Phase 2 Total: 18 new columns (40 → 58 columns when events enabled)**

### Phase 3: Implement Statistics Functionality

When `m_includeStatistics` checkbox is checked, fetch data from `/fixtures/statistics` endpoint.

**Challenge**: Statistics endpoint returns data **per team**, so we need to fetch for BOTH home and away teams.

**Statistics Fields (32 columns - 16 per team):**

Per Team (Home/Away):
- Shots_On_Goal (Int)
- Shots_Off_Goal (Int)
- Total_Shots (Int)
- Blocked_Shots (Int)
- Shots_Inside_Box (Int)
- Shots_Outside_Box (Int)
- Fouls (Int)
- Corner_Kicks (Int)
- Offsides (Int)
- Ball_Possession (String) - percentage
- Yellow_Cards_Stats (Int) - from statistics endpoint
- Red_Cards_Stats (Int)
- Goalkeeper_Saves (Int)
- Total_Passes (Int)
- Passes_Accurate (Int)
- Passes_Percentage (String)

Column naming: `Stat_Home_Shots_On_Goal`, `Stat_Away_Shots_On_Goal`, etc.

**Phase 3 Total: 32 new columns (58 → 90 columns when statistics enabled)**

### Phase 4: Implement Lineups Functionality

When `m_includeLineups` checkbox is checked, fetch data from `/fixtures/lineups` endpoint.

**Approach**: Lineups contain arrays of 11 starting players + substitutes. We'll create summary columns:

**Per Team (Home/Away) - 10 columns each = 20 total:**
- Formation (String) - e.g., "4-3-3"
- Starting_XI_Players (String) - comma-separated player names
- Starting_XI_Numbers (String) - comma-separated jersey numbers
- Starting_XI_Positions (String) - comma-separated positions (G/D/M/F)
- Substitutes_Players (String)
- Substitutes_Numbers (String)
- Substitutes_Positions (String)
- Coach_ID (Int)
- Coach_Name (String)
- Coach_Photo (String)

Column naming: `Lineup_Home_Formation`, `Lineup_Away_Formation`, etc.

**Phase 4 Total: 20 new columns (90 → 110 columns when lineups enabled)**

### Phase 5: Add Player Statistics Support (NEW FUNCTIONALITY)

Add a new checkbox: `m_includePlayerStats` to fetch detailed player performance data from `/fixtures/players` endpoint.

**Approach**: Similar to events, player statistics are arrays. We'll create aggregate columns for key metrics:

**Top Performers (12 columns):**
- Top_Rated_Player_Home (String) - name
- Top_Rated_Player_Home_Rating (Double)
- Top_Rated_Player_Away (String)
- Top_Rated_Player_Away_Rating (Double)
- Top_Scorer_Home (String) - player with most goals
- Top_Scorer_Home_Goals (Int)
- Top_Scorer_Away (String)
- Top_Scorer_Away_Goals (Int)
- Top_Assist_Home (String)
- Top_Assist_Home_Assists (Int)
- Top_Assist_Away (String)
- Top_Assist_Away_Assists (Int)

**Team Aggregates (16 columns):**
- Total_Shots_Home_Players (Int) - sum from all players
- Total_Shots_Away_Players (Int)
- Total_Passes_Home_Players (Int)
- Total_Passes_Away_Players (Int)
- Total_Tackles_Home_Players (Int)
- Total_Tackles_Away_Players (Int)
- Total_Dribbles_Home_Players (Int)
- Total_Dribbles_Away_Players (Int)
- Total_Fouls_Committed_Home (Int)
- Total_Fouls_Committed_Away (Int)
- Total_Fouls_Drawn_Home (Int)
- Total_Fouls_Drawn_Away (Int)
- Average_Rating_Home (Double)
- Average_Rating_Away (Double)
- Captain_Home (String)
- Captain_Away (String)

**Phase 5 Total: 28 new columns (110 → 138 columns when player stats enabled)**

## Final Column Count Summary

| Configuration | Column Count |
|--------------|--------------|
| Base (no checkboxes) | 40 |
| + Events | 58 |
| + Statistics | 90 |
| + Lineups | 110 |
| + Player Stats | 138 |
| All enabled | 138 |

## Implementation Approach

### Code Changes Required:

1. **FixturesNodeModel.java - Line 260 (parseFixtureRow method)**
   - Expand base field extraction to include all 26 missing fields
   - Update cells array size from 14 to 40

2. **FixturesNodeModel.java - Line 46 (executeQuery method)**
   - After getting base fixtures, check if any checkboxes are enabled
   - For each enabled option, make additional API calls per fixture:
     ```java
     if (m_includeEvents.getBooleanValue()) {
         JsonNode events = callApi(client, "/fixtures/events",
                                  Map.of("fixture", String.valueOf(fixtureId)), mapper);
         // Parse and add to row
     }
     ```

3. **FixturesNodeModel.java - Line 319 (getOutputSpec method)**
   - Make spec dynamic based on enabled checkboxes
   - Return different specs for different configurations

4. **Add new helper methods:**
   - `parseEvents(JsonNode events)` - aggregate events into summary strings
   - `parseStatistics(JsonNode stats)` - extract statistics for both teams
   - `parseLineups(JsonNode lineups)` - extract lineup summaries
   - `parsePlayerStats(JsonNode players)` - calculate top performers and aggregates

5. **Add new checkbox:**
   - Add `m_includePlayerStats` setting (similar to existing checkboxes)
   - Update dialog to include new checkbox

### API Call Optimization:

**Problem**: If all checkboxes are enabled, we'd make 5 API calls per fixture:
1. `/fixtures` (base data)
2. `/fixtures/events`
3. `/fixtures/statistics` (home team)
4. `/fixtures/statistics` (away team)
5. `/fixtures/lineups`
6. `/fixtures/players`

**Solution**:
- Batch process fixtures to minimize API calls
- Show progress: "Processing fixture 1 of 50..."
- Consider caching fixture data when user toggles checkboxes

## Test Cases

### Test 1: Base Fields
- **Given**: Fixtures node with no checkboxes enabled
- **When**: Execute with fixture ID 215662
- **Then**: Output table has 40 columns with all base fields populated

### Test 2: Events
- **Given**: Fixtures node with "Include Events" checked
- **When**: Execute with fixture ID 215662
- **Then**:
  - Output has 58 columns
  - Goals_Home_Scorers shows "F. Andrada (25')"
  - Yellow_Cards_Home shows 5
  - Red_Cards_Home shows 1

### Test 3: Statistics
- **Given**: Fixtures node with "Include Statistics" checked
- **When**: Execute with fixture ID 215662, team 463
- **Then**:
  - Output has 90 columns
  - Stat_Home_Shots_On_Goal shows 3
  - Stat_Home_Ball_Possession shows "32%"

### Test 4: Lineups
- **Given**: Fixtures node with "Include Lineups" checked
- **When**: Execute with fixture ID 592872
- **Then**:
  - Output has 110 columns
  - Lineup_Home_Formation shows "4-3-3"
  - Lineup_Home_Coach_Name shows "Guardiola"
  - Lineup_Home_Starting_XI_Players contains "Ederson, Kyle Walker, John Stones, ..."

### Test 5: Player Stats
- **Given**: Fixtures node with "Include Player Stats" checked
- **When**: Execute with fixture ID 169080
- **Then**:
  - Output has 138 columns
  - Top_Rated_Player_Home shows player name
  - Top_Rated_Player_Home_Rating shows 6.3 or higher
  - Captain_Home is populated

### Test 6: All Checkboxes
- **Given**: All checkboxes enabled
- **When**: Execute with fixture ID 215662
- **Then**: Output has all 138 columns populated

### Test 7: Multiple Fixtures
- **Given**: Query by league (Premier League, 2024)
- **When**: All checkboxes enabled
- **Then**:
  - Multiple rows returned (one per fixture)
  - All 138 columns populated for each row
  - No missing data for completed fixtures

## API Documentation References

- Base Fixtures: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures
- Events: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures-events
- Statistics: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures-statistics
- Lineups: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures-lineups
- Players: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures-players
- Head to Head: https://www.api-football.com/documentation-v3#tag/Fixtures/operation/get-fixtures-headtohead

## JSON Sample Files

All sample JSON files are located in this directory:
- `fixtures-response.json` - Base fixture data
- `fixtures-events-response.json` - Events (goals, cards, substitutions)
- `fixtures-statistics-response.json` - Match statistics
- `fixtures-lineups-response.json` - Starting lineups and formations
- `fixtures-players-response.json` - Player performance statistics
- `fixtures-headtohead-response.json` - Head-to-head matches
- `fixtures-rounds-response.json` - Available rounds for a league/season

## Implementation Priority

1. **Phase 1 (Base Fields)** - IMMEDIATE - Low risk, high value
2. **Phase 2 (Events)** - HIGH - Checkbox already exists, users expect it to work
3. **Phase 3 (Statistics)** - HIGH - Checkbox already exists, users expect it to work
4. **Phase 4 (Lineups)** - HIGH - Checkbox already exists, users expect it to work
5. **Phase 5 (Player Stats)** - MEDIUM - New functionality, can be deferred

## Acceptance Criteria

- [ ] All 26 missing base fields are added to output (14 → 40 columns)
- [ ] "Include Events" checkbox fetches and parses events data (+18 columns)
- [ ] "Include Statistics" checkbox fetches and parses statistics data (+32 columns)
- [ ] "Include Lineups" checkbox fetches and parses lineups data (+20 columns)
- [ ] "Include Player Stats" checkbox (new) fetches and parses player data (+28 columns)
- [ ] All test cases pass
- [ ] Node handles null/missing values gracefully (e.g., extratime, penalties)
- [ ] Progress messages show during multi-API-call execution
- [ ] Documentation updated to explain all output columns

## Notes

- **Breaking Change**: Adding base fields changes output from 14 to 40 columns. Existing workflows will need to update column references.
- **API Rate Limits**: With all checkboxes enabled, each fixture requires up to 6 API calls. Consider warning users about API quota usage.
- **Performance**: For 100 fixtures with all options enabled, this could make 600 API calls. Implement batching and progress reporting.
- **Data Consistency**: Some endpoints (like events, statistics) may not have data for all fixtures. Ensure proper null handling.
