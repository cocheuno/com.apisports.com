# Carovex API-Sports KNIME Extension

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![KNIME](https://img.shields.io/badge/KNIME-5.5.0+-yellow.svg)](https://www.knime.com/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)

KNIME extension for querying football data from [API-Sports.io](https://api-sports.io).

This extension provides a comprehensive set of KNIME nodes for accessing football (soccer) data including teams, players, fixtures, standings, predictions, odds, and more. Perfect for sports analytics, data science projects, and building automated football data pipelines.

## Features

- **14+ Query Nodes** - Access teams, players, fixtures, standings, predictions, odds, and more
- **Smart Caching** - Two-level caching (memory + disk) with configurable TTL
- **Automatic Rate Limiting** - Token bucket-based rate limiting per API key
- **Independent Reference Data** - Each Reference Data Loader node has its own isolated database
- **Flow Variables** - Full support for KNIME flow variables for dynamic workflows
- **Comprehensive Statistics** - Teams node outputs up to 146 columns including detailed statistics

## Requirements

- KNIME Analytics Platform 5.5.0 or higher
- Java 17 or higher
- API-Sports.io API key ([Get one here](https://api-sports.io/))

## Installation

### From Update Site (Recommended)

1. Open KNIME Analytics Platform
2. Go to **Help > Install New Software...**
3. Add update site: `[Update site URL]`
4. Select "Carovex API-Sports" and install
5. Restart KNIME

### From Source

```bash
git clone https://github.com/cocheuno/com.apisports.com.git
cd com.apisports.com
mvn clean verify
```

Then install the generated update site from `releng/com.apisports.knime.update/target/repository/`

## Quick Start

1. **Get an API Key** - Sign up at [API-Sports.io](https://api-sports.io/) and get your API key

2. **Add the Connector Node** - Drag "API-Sports Connector" onto your workflow

3. **Configure Connection** - Enter your API key and select sport (Football)

4. **Load Reference Data** - Add "Reference Data Loader" to cache leagues, teams, etc.

5. **Query Data** - Add any query node (Teams, Fixtures, Players, etc.)

### Example Workflow

```
┌─────────────────┐    ┌─────────────────────┐    ┌─────────────┐
│  API-Sports     │───▶│  Reference Data     │───▶│   Teams     │
│  Connector      │    │  Loader             │    │             │
└─────────────────┘    └─────────────────────┘    └─────────────┘
                               │
                               ▼
                      ┌─────────────────┐    ┌─────────────┐
                      │    Fixtures     │───▶│  Standings  │
                      │                 │    │             │
                      └─────────────────┘    └─────────────┘
```

### Multi-Country Workflow

You can have multiple Reference Data Loader nodes for different countries:

```
                      ┌─────────────────────┐    ┌─────────────┐
                 ┌───▶│  Reference Data     │───▶│   Teams     │
                 │    │  Loader (England)   │    │  (England)  │
┌────────────────┤    └─────────────────────┘    └─────────────┘
│  API-Sports    │
│  Connector     │    ┌─────────────────────┐    ┌─────────────┐
└────────────────┤───▶│  Reference Data     │───▶│   Teams     │
                      │  Loader (France)    │    │  (France)   │
                      └─────────────────────┘    └─────────────┘
```

Each Reference Data Loader maintains its own isolated SQLite database.

## Architecture

```
com.apisports.com/
├── bundles/
│   ├── com.apisports.knime.core         # Core API client, caching, rate limiting
│   ├── com.apisports.knime.port         # Custom port objects for connections
│   ├── com.apisports.knime.connector    # Connector and statistics nodes
│   └── com.apisports.knime.football     # Football query nodes (14+ nodes)
├── features/
│   ├── com.apisports.knime.core.feature # Core feature bundle
│   └── com.apisports.knime.football.feature # Football feature bundle
├── releng/
│   └── com.apisports.knime.update       # P2 update site
└── docs/                                 # Documentation
    ├── user/                            # User guides
    ├── workflows/                       # Example workflows
    └── requirements/                    # Node specifications
```

### Bundle Overview

| Bundle | Purpose |
|--------|---------|
| `com.apisports.knime.core` | HTTP client, two-level caching, rate limiting, API utilities |
| `com.apisports.knime.port` | Custom KNIME port objects for passing connections and reference data |
| `com.apisports.knime.connector` | API-Sports Connector and API Statistics nodes |
| `com.apisports.knime.football` | All football query nodes (14+ nodes) |

## Available Nodes

### Connection Nodes

| Node | Description |
|------|-------------|
| **API-Sports Connector** | Authenticates and creates connection to API-Sports |
| **API Statistics** | Displays API usage metrics (calls, cache hits) |

### Reference Data

| Node | Description |
|------|-------------|
| **Reference Data Loader** | Loads and caches countries, leagues, seasons, teams (each node has isolated database) |

### Team & Organization Nodes

| Node | Description | Output Columns |
|------|-------------|----------------|
| **Teams** | Query team information with optional statistics | Up to 146 columns |
| **Coaches** | Query coach information by team | - |
| **Venues** | Query stadium and venue information | - |

### Match Data Nodes

| Node | Description |
|------|-------------|
| **Fixtures** | Query matches (by league, date, team, live, head-to-head) |
| **Fixtures Selector** | Lightweight fixture browser for selection |
| **Standings** | Query league tables and rankings |

### Player Data Nodes

| Node | Description | Output Columns |
|------|-------------|----------------|
| **Players** | Query players (top scorers, assists, cards, by team, search) | 14 columns |
| **Players Selector** | Lightweight player browser for selection | - |
| **Player Stats** | Detailed match-by-match statistics | - |
| **Injuries** | Query player injury information | - |
| **Sidelined** | Query player absences (injuries, suspensions) | - |
| **Transfers** | Query player transfer data | - |
| **Trophies** | Query achievements for players or coaches | - |

### Analytics Nodes

| Node | Description |
|------|-------------|
| **Predictions** | AI-generated match predictions |
| **Odds** | Betting odds from multiple bookmakers |

## Node Details

### API-Sports Connector

Creates an authenticated connection to the API-Sports service.

**Configuration:**
- **API Key** (required) - Your API-Sports.io API key
- **Sport** - Currently supports Football

The subscription tier is automatically determined by your API key.

**Output:**
- API Connection port object (connect to other nodes)

---

### Reference Data Loader

Loads reference data and caches it in a local SQLite database for fast access. **Each node instance maintains its own isolated database** to prevent data collision when using multiple loaders.

**Configuration:**
- **Database Path** (optional) - Leave blank for auto-generated unique path, or specify custom path
- **Clear & Reload** - Wipe existing data before loading
- **Country Filter** - Select specific countries to load
- **Season Filter** - Filter by date range or specific seasons
- **Load Teams** - Whether to load team data (can take several minutes)
- **Load Venues** - Whether to load venue data
- **Cache TTL** - How long to cache data (default: 24 hours)

**Output:**
- Reference Data port object (provides dropdowns in query nodes)

**Database Location:**
- Auto-generated: `~/.apisports/football_ref_<instance-id>.db`
- Each node instance gets a unique database file

---

### Teams

Query team information with comprehensive statistics.

**Configuration:**
- **League** - Select from loaded leagues
- **Season** - Select season year
- **Team** - Optional filter for specific team
- **Team Name Search** - Search by team name
- **Include Statistics** - Enable to get full 146-column output

**Output Columns (146 when statistics enabled):**

| Category | Columns | Description |
|----------|---------|-------------|
| Team Info | 7 | ID, Name, Code, Country, Founded, National, Logo |
| Venue Info | 7 | ID, Name, Address, City, Capacity, Surface, Image |
| Form | 1 | Recent form string (e.g., "WWDLW") |
| Fixtures | 12 | Played, Wins, Draws, Losses (home/away/total) |
| Goals For | 32 | Totals, Averages, Minute Distribution, Over/Under |
| Goals Against | 32 | Totals, Averages, Minute Distribution, Over/Under |
| Biggest Stats | 11 | Streaks, Biggest Wins/Losses |
| Clean Sheets | 6 | Home, Away, Total |
| Penalties | 5 | Scored, Missed, Percentages |
| Lineups | 1 | Most used formations |
| Cards | 32 | Yellow/Red cards by minute periods |

---

### Players

Query player data with multiple query modes.

**Query Modes:**
- **Top Scorers** - League top scorers
- **Top Assists** - League top assists
- **Top Yellow Cards** - Most yellow cards
- **Top Red Cards** - Most red cards
- **Players by Team** - All players for selected team(s)
- **Search by Name** - Find players by name
- **By Player ID** - Specific player details

**Output Columns (14):**
- Player_ID, Name, Firstname, Lastname, Nationality, Age
- Team, Position, Appearances, Goals, Assists
- Yellow_Cards, Red_Cards, Rating

**Optional Input Port:**
- Connect a table with Player_ID column to query detailed stats for specific players

---

### Fixtures

Query match data with multiple query modes.

**Query Modes:**
- **By League/Season** - All fixtures for a league
- **By Date Range** - Fixtures within a date range
- **By Team** - All fixtures for a specific team
- **By Fixture ID** - Specific fixture details
- **Live Fixtures** - Currently playing matches
- **Head to Head** - Historical matches between two teams

**Options:**
- Include events (goals, cards, substitutions)
- Include lineups
- Include statistics
- Include player stats

---

### Standings

Query league tables and rankings.

**Output Columns:**
- Rank, Team, Points
- Played, Won, Drawn, Lost
- Goals For, Goals Against, Goal Difference
- Form (last 5 matches)
- Description (Champion, Relegation, etc.)

---

### Odds

Query betting odds from multiple bookmakers.

**Query Modes:**
- **By Fixture ID** - Odds for specific match
- **By League/Season** - All odds for a league
- **Live Odds** - Current live betting odds

**Filters:**
- Bookmaker filter
- Bet type filter (1X2, Over/Under, etc.)

## Example Workflows

Detailed workflow examples are available in `docs/workflows/`:

### Teams & Players Analysis Workflow

A comprehensive workflow demonstrating Teams and Players nodes for Premier League analysis:

- [README](docs/workflows/teams-players-analysis/README.md) - Use case and step-by-step guide
- [Workflow Specification](docs/workflows/teams-players-analysis/workflow-specification.md) - Detailed node configurations
- [Sample Outputs](docs/workflows/teams-players-analysis/sample-outputs.md) - Expected data examples

**Use Cases Covered:**
- Team performance analysis (home vs away)
- Goal scoring pattern analysis
- Defensive strength ranking
- Top scorers correlation with team success
- Formation analysis

## Common Workflow Patterns

### Pattern 1: League Analysis
```
API Connector → Reference Data → Teams → Fixtures → Standings
```

### Pattern 2: Player Scouting
```
API Connector → Reference Data → Players (Top Scorers) → Player Stats → Trophies
```

### Pattern 3: Match Predictions
```
API Connector → Reference Data → Fixtures Selector → [Filter] → Predictions → Odds
```

### Pattern 4: Multi-Country Comparison
```
API Connector ─┬─► Reference Data (England) ─► Teams (England)
               │
               └─► Reference Data (France) ─► Teams (France)
```

### Pattern 5: API Monitoring
Place "API Statistics" at the end of your workflow to monitor API usage:
```
[Your workflow nodes] → API Statistics
```

## API Rate Limits

API-Sports has rate limits based on your subscription:

| Tier | Requests/Day | Requests/Minute |
|------|--------------|-----------------|
| Free | 100 | 30 |
| Basic | 7,500 | 30 |
| Pro | 150,000+ | 450 |
| Ultra | Unlimited | 450 |

The extension automatically handles rate limiting based on API responses.

## Caching

The extension includes intelligent two-level caching to reduce API calls:

- **L1 Cache** - In-memory LRU cache (default: 1000 entries)
- **L2 Cache** - Disk-based cache with configurable TTL
- **Reference Data** - Cached in SQLite (default: 24 hours)
- **Cache Hits** - Viewable in API Statistics node

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `APISPORTS_CACHE_DIR` | Cache directory location | `~/.apisports/` |

### SQLite Databases

Reference data is stored in per-node SQLite databases in `~/.apisports/`:

**Naming Pattern:** `football_ref_<instance-id>.db`

**Database Schema:**
- `countries` - Country reference data
- `leagues` - League information
- `seasons` - Available seasons per league
- `teams` - Team information
- `team_leagues` - Team-league relationships (many-to-many)
- `metadata` - Cache timestamps and configuration hashes

## Documentation

- [User Guide](docs/user/football-nodes-guide.md) - Complete node reference
- [Teams & Players Workflow](docs/workflows/teams-players-analysis/README.md) - Example workflow
- [Architecture](docs/architecture/descriptor-schema.md) - Technical design
- [Development](docs/development/v2-implementation-summary.md) - Implementation details

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

1. Clone the repository
2. Import into Eclipse with Tycho support
3. Set target platform to KNIME 5.5.0+
4. Run `mvn clean verify` to build

### Building

```bash
# Full build
mvn clean verify

# Skip tests
mvn clean verify -DskipTests

# Build update site only
mvn clean verify -pl releng/com.apisports.knime.update -am
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Third-Party Licenses

This project uses the following open source libraries:

| Library | License | Purpose |
|---------|---------|---------|
| [Jackson](https://github.com/FasterXML/jackson) | Apache 2.0 | JSON parsing |
| [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) | Apache 2.0 | SQLite database access |
| [SnakeYAML](https://github.com/snakeyaml/snakeyaml) | Apache 2.0 | YAML configuration |

See [NOTICE](NOTICE) for full attribution.

## Support

- **Issues**: [GitHub Issues](https://github.com/cocheuno/com.apisports.com/issues)
- **Email**: info@caronelabs.com
- **API Documentation**: [API-Sports Docs](https://api-sports.io/documentation/football/v3)

## Acknowledgments

- [API-Sports.io](https://api-sports.io/) for providing the football data API
- [KNIME](https://www.knime.com/) for the analytics platform
- All contributors to this project

---

**Copyright 2025 Carone Labs** - Licensed under Apache 2.0
