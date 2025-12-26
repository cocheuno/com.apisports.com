# Carovex API-Sports Football Nodes - User Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
3. [Core Workflow](#core-workflow)
4. [Node Reference](#node-reference)
5. [Common Use Cases](#common-use-cases)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)

---

## Introduction

The Carovex API-Sports Football Nodes provide a comprehensive KNIME extension for accessing football (soccer) data from the API-Sports service. These nodes enable you to build data workflows that fetch, analyze, and visualize football data including:

- Teams, players, and coaches
- Fixtures (matches) and results
- League standings and statistics
- Player injuries, transfers, and trophies
- Betting odds and match predictions

### Requirements

- **KNIME Analytics Platform** (4.0 or higher)
- **API-Sports API Key** - Get one at [https://api-sports.io](https://api-sports.io)
- **Active Internet Connection** - All nodes make real-time API calls

### API Subscription Tiers

API-Sports offers different subscription tiers (Free, Basic, Pro, Ultra) with varying rate limits and feature access. Check your tier at [api-sports.io](https://api-sports.io) to understand your API call limits.

---

## Getting Started

### Step 1: Obtain an API Key

1. Visit [https://api-sports.io](https://api-sports.io)
2. Create an account or sign in
3. Subscribe to a plan (Free tier available)
4. Copy your API key from the dashboard

### Step 2: Set Up the API Connector

1. In KNIME, find the **API-Sports Connector** node under: **Carovex → API-Sports**
2. Drag it onto your workflow canvas
3. Double-click to configure:
   - **API Key**: Paste your API key
   - **Sport**: Select "Football"
   - **Tier**: Select your subscription tier (free, basic, pro, ultra)
4. Click **OK** and execute the node

The connector creates a connection that can be reused by all downstream nodes in your workflow.

### Step 3: Load Reference Data

1. Add a **Reference Data Loader** node (under **Carovex → API-Sports → Football**)
2. Connect it to the API Connector's output port
3. Configure options:
   - **Load Teams**: Check this to load team data (recommended but takes longer)
   - **Load Venues**: Future feature (not yet implemented)
   - **Cache TTL**: How long to cache reference data (default: 24 hours)
4. Execute the node

This node loads essential reference data (countries, leagues, teams) that populates dropdown menus in query nodes.

**Performance Note**: Loading teams can take several minutes as it queries the API for each league. By default, only teams from the first 10 leagues are loaded to minimize API calls.

---

## Core Workflow

All workflows follow this basic pattern:

```
┌──────────────────┐
│  API-Sports      │
│  Connector       │
└────────┬─────────┘
         │
         ├──────────────────────┐
         │                      │
         ▼                      ▼
┌──────────────────┐   ┌──────────────────┐
│  Reference Data  │   │  Query Nodes     │
│  Loader          │   │  (Teams,         │
└────────┬─────────┘   │   Players, etc.) │
         │             └──────────────────┘
         │
         ▼
┌──────────────────┐
│  Query Nodes     │
│  (Teams,         │
│   Players, etc.) │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Data Analysis   │
│  & Visualization │
└──────────────────┘
```

**Key Points**:
- The **API Connector** is required for all workflows
- The **Reference Data Loader** populates dropdown menus in query nodes
- Most query nodes require both the API Connection AND Reference Data as inputs
- Some nodes (like **Trophies**) require data tables from other nodes as input

---

## Node Reference

### Connection Nodes

#### API-Sports Connector
**Location**: Carovex → API-Sports
**Type**: Source
**Purpose**: Creates authenticated connection to API-Sports service

**Inputs**: None
**Outputs**: API-Sports Connection

**Configuration**:
- **API Key**: Your API-Sports API key
- **Sport**: Select "Football"
- **Tier**: Your subscription tier (affects rate limiting)

**Usage**: Required as the first node in every workflow. The connection can be split and used by multiple downstream nodes.

---

#### Reference Data Loader
**Location**: Carovex → API-Sports → Football
**Type**: Source
**Purpose**: Loads countries, leagues, seasons, teams, and venues for use in query nodes

**Inputs**:
- Port 0: API-Sports Connection

**Outputs**:
- Port 0: Reference Data (ReferenceDataPortObject)

**Configuration**:
- **Load Teams**: Enable to load team data (recommended, but slower)
- **Load Venues**: Future feature
- **Cache TTL**: How long reference data remains valid (default: 86400 seconds / 24 hours)

**Usage**: Connect to query nodes to populate league/team/season dropdowns. Execute once and reuse the output for multiple query nodes.

**Performance**: Loading teams queries the API for each league and can take several minutes. Only the first 10 leagues are loaded by default.

---

### Team & Organization Nodes

#### Teams
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query team information and optionally team statistics

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Teams table

**Output Columns**: Team_ID, Team_Name, Code, Country, Founded, Venue_Name, Venue_City, Venue_Capacity, Wins, Draws, Losses, Goals_For, Goals_Against

**Configuration**:
- **League**: Select league from dropdown
- **Season**: Select season year
- **Team**: Optional - Select specific team (leave blank for all teams)
- **Team Name Search**: Optional - Search by team name (partial match)
- **Include Team Statistics**: Check to add wins/draws/losses/goals (makes additional API calls)

**Use Cases**:
- Get all teams in a league
- Query statistics for a specific team
- Search for teams by name

---

#### Coaches
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query coach information

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Coaches table

**Output Columns**: Coach_ID, Coach_Name, Firstname, Lastname, Age, Nationality, Team

**Configuration**:
- **Team**: Select team to query coaches for
  - Select "All Teams" to query coaches for all teams in reference data

**Use Cases**:
- Get current coach for a specific team
- Get all coaches across multiple teams
- Use output with Trophies node to see coach achievements

---

#### Venues
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query stadium and venue information

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Venues table

**Configuration**: (Varies by implementation)

**Use Cases**:
- Get stadium capacities
- Find venue locations
- Analyze home/away performance by venue

---

### Match Data Nodes

#### Fixtures
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query match fixtures (games) with flexible query options

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Fixtures table

**Output Columns**: Fixture_ID, Date, Status, League_ID, League_Name, Season, Round, Home_Team_ID, Home_Team, Away_Team_ID, Away_Team, Home_Goals, Away_Goals, Venue

**Configuration**:
- **Query Type**: Select how to query fixtures:
  - **By League/Season**: All fixtures for a league and season
  - **By Date Range**: Fixtures within specific dates
  - **By Team**: All fixtures for a specific team
  - **By Fixture ID**: Details for one specific fixture
  - **Live Fixtures**: Currently in-progress matches
  - **Head to Head**: Past encounters between two teams
- **League**: Select league (for league/season queries)
- **Season**: Select season year
- **Team**: Optional team filter
- **From Date / To Date**: Date range in YYYY-MM-DD format
- **Fixture ID**: Specific fixture ID to query
- **Status Filter**: Filter by status (NS=Not Started, FT=Finished, LIVE=In Progress)
- **Include Additional Data**: Include events, lineups, or statistics

**Use Cases**:
- Get all fixtures for a league season
- Find upcoming matches for a specific team
- Query historical head-to-head records
- Monitor live matches
- Get detailed match data by fixture ID

---

#### Standings
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query league standings/tables

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Standings table

**Output Columns**: Rank, Team_ID, Team_Name, Points, Played, Win, Draw, Lose, Goals_For, Goals_Against, Goal_Difference, Form, Description

**Configuration**:
- **League**: Select league
- **Season**: Select season year

**Use Cases**:
- Get current league table
- Analyze historical standings
- Track team performance over time
- Identify promotion/relegation zones (Description column)

**Note**: This is one of the simplest query nodes - only requires league and season.

---

### Player Data Nodes

#### Players
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query player data and statistics with multiple query modes

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Players table

**Output Columns**: Player_ID, Name, Firstname, Lastname, Nationality, Age, Team, Position, Appearances, Goals, Assists, Yellow_Cards, Red_Cards, Rating

**Configuration**:
- **Query Type**: Select query mode:
  - **Top Scorers**: Leading goal scorers for league/season
  - **Top Assists**: Players with most assists
  - **Top Yellow Cards**: Most yellow carded players
  - **Top Red Cards**: Most red carded players
  - **Search by Name**: Find players by name (partial match)
  - **By Player ID**: Detailed stats for specific player
- **League**: Select league (for top scorers/assists/cards)
- **Season**: Select season year (required for all queries)
- **Player Name**: For name search queries (partial match supported)
- **Player ID**: For player ID queries (numeric)

**Use Cases**:
- Find top scorers in a league
- Search for specific players
- Get detailed player statistics
- Compare player performance across seasons
- Identify disciplinary issues (cards)

---

#### Injuries
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query player injury information

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Injuries table

**Configuration**: (Varies by implementation)

**Use Cases**:
- Track current player injuries
- Analyze injury history
- Plan team selections around availability

---

#### Sidelined
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query player absences (injuries, suspensions, etc.)

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Sidelined table

**Configuration**: (Varies by implementation)

**Use Cases**:
- Track all player absences
- Differentiate between injuries and suspensions
- Analyze team availability

---

#### Transfers
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query player transfer data

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Transfers table

**Configuration**: (Varies by implementation)

**Use Cases**:
- Track transfer market activity
- Analyze player movements between clubs
- Study transfer fees and patterns
- Monitor loan deals

---

#### Trophies
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query trophies won by players or coaches

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data
- Port 2: Players or Coaches table (from Players or Coaches node)

**Outputs**:
- Port 0: Trophies table

**Output Columns**: Person_ID, Person_Name, League, Country, Season, Place

**Configuration**: None - automatically detects whether input is Players or Coaches

**How It Works**:
1. Connect output from **Players** or **Coaches** node to Port 2
2. Node automatically detects input type by checking for Player_ID or Coach_ID column
3. Queries trophies for all individuals in the input table
4. Returns one row per trophy won

**Use Cases**:
- See all trophies won by top scorers
- Compare career achievements of players
- Analyze coach success history
- Filter players by championship wins

**Example Workflow**:
```
API Connector → Reference Data → Players (Top Scorers) → Trophies
                       ↓                                      ↑
                       └──────────────────────────────────────┘
```

---

### Advanced Analysis Nodes

#### Predictions
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query AI-generated match predictions

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Predictions table

**Configuration**: (Varies by implementation)

**Use Cases**:
- Get predicted outcomes for upcoming matches
- Analyze prediction accuracy
- Combine with odds data for betting analysis

---

#### Odds
**Location**: Carovex → API-Sports → Football
**Type**: Manipulator
**Purpose**: Query betting odds from multiple bookmakers

**Inputs**:
- Port 0: API-Sports Connection
- Port 1: Reference Data

**Outputs**:
- Port 0: Odds table

**Output Columns**: Fixture_ID, League, Bookmaker_ID, Bookmaker_Name, Bet_Type, Bet_Value, Odd

**Configuration**:
- **Query Type**: How to query odds:
  - **By Fixture ID**: Odds for specific fixture
  - **By League/Season**: Odds for all fixtures in league
  - **Live Odds**: Odds for currently live fixtures
- **Fixture ID**: For fixture ID queries (numeric)
- **League**: For league/season queries
- **Season**: For league/season queries
- **Bookmaker**: Optional - Filter by bookmaker name (e.g., "Bet365", "Betway")
- **Bet Type**: Optional - Filter by bet type (e.g., "Match Winner", "Goals Over/Under")

**Use Cases**:
- Compare odds across bookmakers
- Track odds movements over time
- Combine with predictions for betting strategy
- Analyze market sentiment

**Note**: Odds data is typically only available for recent and upcoming fixtures. Historical odds availability depends on your API subscription.

---

### Utility Nodes

#### API Statistics
**Location**: Carovex → API-Sports
**Type**: Other
**Purpose**: Display API usage statistics for monitoring API call consumption

**Inputs**:
- Port 0: API-Sports Connection

**Outputs**:
- Port 0: Statistics table (3 rows)

**Output Rows**:
- **API Calls**: Number of actual calls made to the API
- **Cache Hits**: Number of requests served from cache
- **Total Requests**: Sum of API calls and cache hits

**Configuration**: None

**Usage**: Place this node at the END of your workflow after all data-fetching nodes have executed. It will show cumulative statistics for all API calls made through the connection.

**Example Workflow**:
```
API Connector → Reference Data → Teams → Fixtures
       ↓
       └──────────────────────────────────────────→ API Statistics
```

**Why Use This**:
- Monitor API quota usage
- Verify cache effectiveness
- Optimize workflows to minimize API calls
- Stay within your subscription tier limits

---

## Common Use Cases

### Use Case 1: League Overview Dashboard

**Goal**: Get comprehensive data for a specific league season

**Workflow**:
```
API Connector → Reference Data → Teams (with statistics)
                       ↓         → Fixtures
                                 → Standings
                                 → Players (Top Scorers)
```

**Steps**:
1. Set up API Connector with your credentials
2. Load Reference Data with teams enabled
3. Add Teams node, select league/season, enable statistics
4. Add Fixtures node, query by league/season
5. Add Standings node for same league/season
6. Add Players node in "Top Scorers" mode
7. Execute workflow
8. Combine outputs with KNIME Joiner nodes for analysis

**Output**: Complete league dataset including teams, matches, standings, and top performers

---

### Use Case 2: Player Trophy Analysis

**Goal**: Find which top scorers have won the most trophies

**Workflow**:
```
API Connector → Reference Data → Players (Top Scorers) → Trophies
```

**Steps**:
1. Set up API Connector
2. Load Reference Data
3. Add Players node, select "Top Scorers" mode
4. Configure league and season
5. Add Trophies node connected to Players output
6. Connect Reference Data to Trophies node
7. Execute workflow
8. Use GroupBy node to count trophies per player

**Output**: Trophy history for all top scorers in the league

---

### Use Case 3: Team Performance Analysis

**Goal**: Analyze team performance across multiple seasons

**Workflow**:
```
API Connector → Reference Data → Teams (Season 2022)
                                → Teams (Season 2023)
                                → Teams (Season 2024)
                                         ↓
                                   Concatenate
                                         ↓
                                   Line Plot (Team performance over time)
```

**Steps**:
1. Set up API Connector and Reference Data
2. Add three Teams nodes, each for a different season
3. Enable "Include Team Statistics" on all three
4. Select the same team in all three nodes
5. Use Concatenate node to combine outputs
6. Create visualizations showing performance trends

**Output**: Multi-season performance trends for a team

---

### Use Case 4: Match Odds Comparison

**Goal**: Compare betting odds across bookmakers for upcoming fixtures

**Workflow**:
```
API Connector → Reference Data → Fixtures (next round) → Odds
```

**Steps**:
1. Set up API Connector and Reference Data
2. Add Fixtures node, query by league/season and round
3. Filter for upcoming matches (Status = "NS")
4. Add Odds node, query by league/season
5. Join Fixtures and Odds by Fixture_ID
6. Use Pivoting node to compare bookmaker odds side-by-side

**Output**: Odds comparison table for all upcoming matches

---

### Use Case 5: Team Injury Impact Analysis

**Goal**: Correlate team performance with player injuries

**Workflow**:
```
API Connector → Reference Data → Fixtures (team's matches)
                                → Injuries (team's players)
                                         ↓
                                   Join on Date
                                         ↓
                                   Analyze win rate with/without injuries
```

**Steps**:
1. Query fixtures for a specific team
2. Query injuries for the same team
3. Join datasets on date ranges
4. Calculate team performance metrics
5. Compare win rates when key players are injured vs. available

**Output**: Statistical analysis of injury impact on results

---

## Best Practices

### 1. Minimize API Calls

**Why**: API subscriptions have rate limits and call quotas

**How**:
- Execute Reference Data Loader once and reuse its output
- Use caching effectively (default cache TTL is 24 hours)
- Filter data using node parameters rather than fetching everything
- Use API Statistics node to monitor usage

**Example**: Don't execute Reference Data Loader multiple times - instead, split its output to multiple nodes.

---

### 2. Understand Execution Order

**KNIME Execution Model**:
- Nodes execute from left to right, top to bottom
- A node cannot access data from nodes that haven't executed yet
- Trophies node needs Players/Coaches data, so Players must execute first

**Best Practice**: Design workflows with clear data flow from source to analysis

---

### 3. Use Reference Data Effectively

**Why**: Reference Data populates dropdowns and validates inputs

**Tips**:
- Always connect Reference Data to query nodes
- Enable "Load Teams" if you need team dropdowns
- Refresh Reference Data daily to get updated leagues/seasons
- Check cache TTL if dropdowns seem outdated

---

### 4. Handle Large Datasets

**Challenge**: Some queries return thousands of rows (e.g., all fixtures for a season)

**Solutions**:
- Use filters in node configuration (date ranges, specific teams)
- Add KNIME Row Filter nodes after queries
- Query incrementally (by round, by month) rather than all at once
- Consider your API tier limits

**Example**: Instead of querying all league fixtures at once, query by round or month.

---

### 5. Error Handling

**Common Errors**:
- "API returned errors: required - At least one parameter is required"
  - **Cause**: Missing required parameters
  - **Fix**: Ensure all required fields are configured

- "Execute failed: Please select a specific team"
  - **Cause**: Node requires a team selection
  - **Fix**: Select a team from dropdown (or use "All Teams" if supported)

- "API client is not available. Please reset and re-execute the API-Sports Connector node"
  - **Cause**: Connection lost or node out of sync
  - **Fix**: Reset and re-execute API Connector node

- "Invalid API key"
  - **Cause**: API key is incorrect or expired
  - **Fix**: Verify your API key at api-sports.io

---

### 6. Optimize Workflow Performance

**Tips**:
- Place API Statistics node at the very end of the workflow
- Avoid unnecessary "Include Statistics" options if you don't need the data
- Use KNIME's caching - re-executing an unchanged node uses cached results
- Close and re-open KNIME if nodes seem slow (clears memory)

---

### 7. Working with Dates

**Date Formats**: Most nodes use **YYYY-MM-DD** format (e.g., "2024-03-15")

**Tips**:
- Use KNIME String Manipulation nodes to format dates if needed
- Filter fixtures by date range to reduce data volume
- Consider time zones when querying live fixtures

---

### 8. Combine Nodes Effectively

**Powerful Combinations**:
- Teams + Fixtures = Team-specific match analysis
- Players + Trophies = Player achievement profiles
- Fixtures + Odds = Betting analysis
- Standings + Teams = Position correlation with team statistics

**Use KNIME Joiners**: Connect related datasets using ID columns (Team_ID, Player_ID, Fixture_ID)

---

## Troubleshooting

### Problem: Nodes Missing from Node Repository

**Symptoms**: Cannot find Carovex or API-Sports nodes in KNIME

**Solutions**:
1. Verify the plugin is installed in your KNIME installation
2. Check that bundles are in the correct plugins directory
3. Restart KNIME
4. Check KNIME log files for plugin loading errors
5. Verify KNIME version compatibility (4.0+)

---

### Problem: "Connection refused" or "Network error"

**Symptoms**: API calls fail with network-related errors

**Solutions**:
1. Check internet connection
2. Verify firewall isn't blocking api-sports.io
3. Check if API-Sports service is operational
4. Try reducing parallel API calls
5. Check proxy settings if behind corporate firewall

---

### Problem: Empty Output Tables

**Symptoms**: Nodes execute successfully but return zero rows

**Possible Causes**:
1. **No data available**: API has no data for your query parameters
2. **Invalid filters**: Your filters are too restrictive
3. **Wrong season**: Selected season has no data
4. **Subscription limitation**: Your API tier doesn't include this endpoint

**Solutions**:
1. Verify data exists for your query (check on API-Sports website)
2. Broaden your filters (remove optional filters)
3. Try a different season (current or recent)
4. Check your API subscription tier and included endpoints

---

### Problem: Slow Execution

**Symptoms**: Nodes take a very long time to execute

**Causes**:
1. **Reference Data Loader with teams**: Loading teams queries each league
2. **Large date ranges**: Querying many fixtures at once
3. **Rate limiting**: API is throttling your requests
4. **Include Statistics options**: Making additional API calls per row

**Solutions**:
1. Disable "Load Teams" if not needed
2. Query smaller date ranges or specific teams
3. Upgrade API tier for higher rate limits
4. Disable "Include Statistics" options if not needed
5. Use caching (default 24 hours)

---

### Problem: "Rate limit exceeded"

**Symptoms**: API returns 429 error or rate limit messages

**Causes**: You've exceeded your API tier's rate limits

**Solutions**:
1. Wait for rate limit window to reset (usually 1 minute)
2. Reduce concurrent API calls
3. Enable caching to reduce repeated calls
4. Upgrade to higher API tier
5. Space out node executions

---

### Problem: Dropdown Menus Are Empty

**Symptoms**: League/team dropdowns in query nodes are empty

**Causes**:
1. Reference Data not connected
2. Reference Data not executed
3. Cache is stale
4. Load Teams option disabled

**Solutions**:
1. Connect Reference Data Loader output to query node
2. Execute Reference Data Loader before configuring query nodes
3. Re-execute Reference Data Loader to refresh cache
4. Enable "Load Teams" option if team dropdowns are empty

---

### Problem: Trophies Node Error

**Symptoms**: "Input table must contain either 'Player_ID' or 'Coach_ID' column"

**Cause**: Trophies node Port 2 is not connected to Players or Coaches output

**Solution**: Connect output from Players or Coaches node to Trophies node's third input port (Port 2)

---

### Problem: Statistics Always Show Zero

**Symptoms**: API Statistics node shows 0 for all metrics

**Cause**: API Statistics node executed before data-fetching nodes

**Solution**: Place API Statistics node at the END of your workflow and execute the entire workflow sequentially. The statistics accumulate as nodes execute, so Statistics must run last.

**Correct Workflow**:
```
API Connector → Reference Data → Teams → API Statistics
```

**Incorrect Workflow**:
```
API Connector → API Statistics → Reference Data → Teams
              (executes too early)
```

---

### Getting Help

If you encounter issues not covered in this guide:

1. **Check API-Sports Documentation**: [https://www.api-football.com/documentation-v3](https://www.api-football.com/documentation-v3)
2. **Verify API Status**: [https://status.api-sports.io](https://status.api-sports.io)
3. **Review KNIME Logs**: Check KNIME console for error details
4. **Contact Support**: Reach out to your plugin provider

---

## Appendix: Node Quick Reference

| Node | Inputs | Primary Use | Key Configuration |
|------|--------|-------------|-------------------|
| **API-Sports Connector** | None | Authenticate API connection | API Key, Sport, Tier |
| **Reference Data Loader** | Connection | Load leagues/teams | Load Teams option |
| **Teams** | Connection, Reference | Query team data | League, Season, Include Statistics |
| **Players** | Connection, Reference | Query player stats | Query Type, League, Season |
| **Fixtures** | Connection, Reference | Query matches | Query Type, Date Range |
| **Standings** | Connection, Reference | Get league table | League, Season |
| **Coaches** | Connection, Reference | Query coach info | Team selection |
| **Trophies** | Connection, Reference, Players/Coaches | Get trophy history | Auto-detects input type |
| **Injuries** | Connection, Reference | Query injuries | Team/Player filters |
| **Transfers** | Connection, Reference | Query transfers | Team/Player filters |
| **Sidelined** | Connection, Reference | Query absences | Team/Player filters |
| **Predictions** | Connection, Reference | Get match predictions | Fixture selection |
| **Odds** | Connection, Reference | Query betting odds | Query Type, Bookmaker |
| **API Statistics** | Connection | Monitor API usage | None (place at end) |

---

## Version Information

**Document Version**: 1.0
**Last Updated**: 2024
**Compatible KNIME Version**: 4.0+
**API-Sports API Version**: v3

---

*This user guide covers the Carovex API-Sports Football Nodes for KNIME. For technical support or feature requests, contact your plugin provider.*
