# KNIME Workflow Specification: Teams & Players Analysis

This document provides precise node configurations for implementing the Teams and Players analysis workflow in KNIME Analytics Platform.

## Workflow Metadata

| Property | Value |
|----------|-------|
| Workflow Name | Premier League Teams & Players Analysis |
| Category | Sports Analytics |
| Estimated API Calls | 25-50 (depending on team count) |
| Execution Time | 2-5 minutes (with statistics enabled) |

---

## Node Specifications

### Node 1: API-Sports Connector

**Node ID**: `com.apisports.knime.connector.ApiSportsConnectorNodeFactory`
**Position**: (100, 100)

**Settings**:
```
apiKey: <your-api-key>
sport: FOOTBALL
```

**Connections**:
- Output Port 0 → Node 2 (Reference Data Loader) Input Port 0

---

### Node 2: Reference Data Loader

**Node ID**: `com.apisports.knime.football.nodes.reference.ReferenceDataLoaderNodeFactory`
**Position**: (300, 100)

**Settings**:
```
loadTeams: true
loadVenues: false
cacheTtlSeconds: 86400
```

**Connections**:
- Output Port 0 (Reference Data) → Node 3 (Teams) Input Port 1
- Output Port 0 (Reference Data) → Node 4 (Players - Top Scorers) Input Port 1
- Output Port 0 (Reference Data) → Node 5 (Players - Top Assists) Input Port 1
- Output Port 0 (Reference Data) → Node 6 (Players - Yellow Cards) Input Port 1

---

### Node 3: Teams (All Teams with Statistics)

**Node ID**: `com.apisports.knime.football.nodes.query.teams.TeamsNodeFactory`
**Position**: (500, 50)

**Settings**:
```
leagueId: 39  # Premier League
season: 2024
teamId: -1  # All teams
teamName: ""
includeStatistics: true
```

**Input Ports**:
- Port 0: API-Sports Connection (from Node 1)
- Port 1: Reference Data (from Node 2)

**Output Port 0**: BufferedDataTable (146 columns)

**Expected Output** (20 rows for Premier League):

| Column | Example Value |
|--------|---------------|
| Team_ID | 33 |
| Team_Name | Manchester United |
| Team_Code | MUN |
| Team_Country | England |
| Team_Founded | 1878 |
| Team_National | false |
| Team_Logo | https://media.api-sports.io/... |
| Venue_ID | 556 |
| Venue_Name | Old Trafford |
| Venue_City | Manchester |
| Venue_Capacity | 76212 |
| Form | WDWLW |
| Fixtures_Wins_Total | 15 |
| Goals_For_Total_Total | 45 |
| Clean_Sheet_Total | 8 |
| ... | ... |

**Connections**:
- Output Port 0 → Node 7 (Column Filter) Input Port 0
- Output Port 0 → Node 10 (Joiner) Input Port 0

---

### Node 4: Players (Top Scorers)

**Node ID**: `com.apisports.knime.football.nodes.query.players.PlayersNodeFactory`
**Position**: (500, 150)

**Settings**:
```
queryType: "Top Scorers"
leagueId: 39  # Premier League
season: 2024
playerName: ""
playerId: ""
teamIds: []
```

**Input Ports**:
- Port 0: API-Sports Connection (from Node 1)
- Port 1: Reference Data (from Node 2)
- Port 2: (Optional, not connected)

**Output Port 0**: BufferedDataTable (14 columns)

**Expected Output** (20 rows - top 20 scorers):

| Column | Example Value |
|--------|---------------|
| Player_ID | 1100 |
| Name | E. Haaland |
| Firstname | Erling |
| Lastname | Haaland |
| Nationality | Norway |
| Age | 24 |
| Team | Manchester City |
| Position | Attacker |
| Appearances | 30 |
| Goals | 25 |
| Assists | 5 |
| Yellow_Cards | 3 |
| Red_Cards | 0 |
| Rating | 7.45 |

**Connections**:
- Output Port 0 → Node 8 (Row Filter) Input Port 0
- Output Port 0 → Node 11 (Concatenate) Input Port 0

---

### Node 5: Players (Top Assists)

**Node ID**: `com.apisports.knime.football.nodes.query.players.PlayersNodeFactory`
**Position**: (500, 250)

**Settings**:
```
queryType: "Top Assists"
leagueId: 39  # Premier League
season: 2024
playerName: ""
playerId: ""
teamIds: []
```

**Input Ports**:
- Port 0: API-Sports Connection (from Node 1)
- Port 1: Reference Data (from Node 2)

**Output Port 0**: BufferedDataTable (14 columns)

**Expected Output** (20 rows - top 20 assist providers):

| Column | Example Value |
|--------|---------------|
| Player_ID | 903 |
| Name | K. De Bruyne |
| Team | Manchester City |
| Position | Midfielder |
| Goals | 8 |
| Assists | 15 |
| Rating | 7.82 |

**Connections**:
- Output Port 0 → Node 11 (Concatenate) Input Port 1

---

### Node 6: Players (Top Yellow Cards)

**Node ID**: `com.apisports.knime.football.nodes.query.players.PlayersNodeFactory`
**Position**: (500, 350)

**Settings**:
```
queryType: "Top Yellow Cards"
leagueId: 39  # Premier League
season: 2024
playerName: ""
playerId: ""
teamIds: []
```

**Input Ports**:
- Port 0: API-Sports Connection (from Node 1)
- Port 1: Reference Data (from Node 2)

**Output Port 0**: BufferedDataTable (14 columns)

**Connections**:
- Output Port 0 → Node 11 (Concatenate) Input Port 2

---

### Node 7: Column Filter (Teams Analysis Columns)

**Node ID**: `org.knime.base.node.preproc.filter.column.DataColumnSpecFilterNodeFactory`
**Position**: (700, 50)

**Settings**:
```
Include columns:
  - Team_ID
  - Team_Name
  - Team_Country
  - Venue_Name
  - Venue_Capacity
  - Form
  - Fixtures_Played_Total
  - Fixtures_Wins_Total
  - Fixtures_Wins_Home
  - Fixtures_Wins_Away
  - Fixtures_Draws_Total
  - Fixtures_Losses_Total
  - Goals_For_Total_Total
  - Goals_For_Average_Total
  - Goals_Against_Total_Total
  - Goals_Against_Average_Total
  - Clean_Sheet_Total
  - Failed_To_Score_Total
  - Biggest_Streak_Wins
  - Penalty_Scored_Total
  - Penalty_Scored_Percentage
  - Lineups
```

**Connections**:
- Output Port 0 → Node 12 (Sorter) Input Port 0

---

### Node 8: Row Filter (High Scorers)

**Node ID**: `org.knime.base.node.preproc.filter.row.RowFilterNodeFactory`
**Position**: (700, 150)

**Settings**:
```
filterCriteria: Column "Goals" > 10
columnName: Goals
operator: Greater Than
value: 10
```

**Connections**:
- Output Port 0 → Node 10 (Joiner) Input Port 1

---

### Node 9: Math Formula (Home Advantage Calculation)

**Node ID**: `org.knime.ext.jep.JEPNodeFactory`
**Position**: (900, 50)

**Settings**:
```
expression: $Fixtures_Wins_Home$ - $Fixtures_Wins_Away$
newColumnName: Home_Advantage
columnType: Integer
```

**Input**: From Column Filter output
**Output**: Teams table with Home_Advantage column

---

### Node 10: Joiner (Teams + Top Scorers)

**Node ID**: `org.knime.base.node.preproc.joiner.Joiner3NodeFactory`
**Position**: (900, 150)

**Settings**:
```
leftJoinColumn: Team_Name
rightJoinColumn: Team
joinType: Inner Join
duplicateHandling: Append suffix (_right)
```

**Input Ports**:
- Port 0: Teams table (from Node 3)
- Port 1: Filtered Top Scorers (from Node 8)

**Output**: Combined table with team stats and their top scorers

---

### Node 11: Concatenate (All Player Queries)

**Node ID**: `org.knime.base.node.preproc.append.row.AppendedRowsNodeFactory`
**Position**: (700, 300)

**Settings**:
```
unionMode: Union
skipDuplicates: true
```

**Input Ports**:
- Port 0: Top Scorers (from Node 4)
- Port 1: Top Assists (from Node 5)
- Port 2: Top Yellow Cards (from Node 6)

**Output**: Combined player statistics table

---

### Node 12: Sorter (By Wins)

**Node ID**: `org.knime.base.node.preproc.sorter.SorterNodeFactory`
**Position**: (900, 50)

**Settings**:
```
sortColumn: Fixtures_Wins_Total
sortOrder: Descending
```

---

### Node 13: GroupBy (Goals by Time Period)

**Node ID**: `org.knime.base.node.preproc.groupby.GroupByNodeFactory`
**Position**: (1100, 50)

**Settings**:
```
groupColumns: [] (no grouping - aggregate all)
aggregations:
  - Goals_For_Minute_0_15_Total: Sum
  - Goals_For_Minute_16_30_Total: Sum
  - Goals_For_Minute_31_45_Total: Sum
  - Goals_For_Minute_46_60_Total: Sum
  - Goals_For_Minute_61_75_Total: Sum
  - Goals_For_Minute_76_90_Total: Sum
  - Goals_For_Minute_91_105_Total: Sum
  - Goals_For_Minute_106_120_Total: Sum
```

**Output**: Aggregated goals by time period across all teams

---

### Node 14: Bar Chart (Home vs Away Wins)

**Node ID**: `org.knime.js.base.node.viz.plotter.bar.BarChartNodeFactory`
**Position**: (1100, 150)

**Settings**:
```
categoryColumn: Team_Name
valueColumns: [Fixtures_Wins_Home, Fixtures_Wins_Away]
chartTitle: Home vs Away Performance
orientation: Vertical
```

---

### Node 15: Scatter Plot (Goals vs Wins)

**Node ID**: `org.knime.js.base.node.viz.plotter.scatter.ScatterPlotNodeFactory`
**Position**: (1100, 250)

**Settings**:
```
xColumn: Goals_For_Total_Total
yColumn: Fixtures_Wins_Total
colorColumn: Team_Name
chartTitle: Goals Scored vs Wins
```

---

## Workflow Connections Summary

```
Node 1 (Connector)
    ├──► Node 2 (Reference Data Loader)
    │        ├──► Node 3 (Teams)
    │        │        ├──► Node 7 (Column Filter) ──► Node 9 (Math Formula) ──► Node 12 (Sorter)
    │        │        └──► Node 10 (Joiner)
    │        │                 ▲
    │        ├──► Node 4 (Top Scorers)
    │        │        ├──► Node 8 (Row Filter) ──► Node 10 (Joiner)
    │        │        └──► Node 11 (Concatenate)
    │        │                 ▲
    │        ├──► Node 5 (Top Assists) ──────────► Node 11 (Concatenate)
    │        │                                            ▲
    │        └──► Node 6 (Yellow Cards) ────────► Node 11 (Concatenate)
    │
    └──► (Connection passed through Reference Data Loader to all query nodes)
```

---

## Execution Order

1. **Node 1**: API-Sports Connector (immediate)
2. **Node 2**: Reference Data Loader (2-5 minutes with teams)
3. **Nodes 3-6**: Teams and Players queries (parallel, ~30 seconds each)
4. **Nodes 7-8**: Filters (immediate)
5. **Nodes 9-11**: Transformations (immediate)
6. **Nodes 12-15**: Analysis and Visualization (immediate)

---

## Expected API Call Count

| Node | API Calls | Notes |
|------|-----------|-------|
| Reference Data Loader | ~10 | Countries, leagues, seasons, teams |
| Teams (with stats) | 21 | 1 for teams list + 20 for individual team statistics |
| Players (Top Scorers) | 1 | Single endpoint |
| Players (Top Assists) | 1 | Single endpoint |
| Players (Yellow Cards) | 1 | Single endpoint |
| **Total** | ~34 | Within free tier limits |

---

## Output Files

After execution, export the following tables:

1. **team_performance.csv**: Filtered Teams output with key statistics
2. **top_scorers.csv**: Top Scorers output
3. **all_players.csv**: Concatenated player statistics
4. **team_player_combined.csv**: Joined Teams + Top Scorers

---

## Validation Checklist

- [ ] Teams output has 20 rows (Premier League)
- [ ] Teams output has 146 columns (with statistics enabled)
- [ ] Top Scorers output has 20 rows
- [ ] All Player_ID values are non-zero
- [ ] Join produces results (team names match)
- [ ] No rate limit errors during execution
- [ ] Statistics columns are populated (not all missing)

---

## Troubleshooting

### Issue: Reference Data Loader hangs
**Solution**: Check API key validity, verify internet connection

### Issue: Teams statistics all missing
**Solution**: Verify "Include Statistics" checkbox is enabled

### Issue: Joiner produces empty output
**Solution**: Check Team_Name (Teams) vs Team (Players) - may need String Manipulation to trim whitespace

### Issue: Concatenate has type conflicts
**Solution**: All Players queries use same schema, no conflict expected

---

## Extension Points

1. **Add Standings**: Include Standings node for league position context
2. **Add Fixtures**: Include Fixtures node for match-level analysis
3. **Multi-Season**: Loop over seasons 2022, 2023, 2024 for trend analysis
4. **Team Comparison**: Filter Teams by specific team_ids for head-to-head
5. **Player Details**: Use optional input port on Players node for detailed stats
