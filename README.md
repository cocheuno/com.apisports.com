# Carovex API-Sports KNIME Extension

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![KNIME](https://img.shields.io/badge/KNIME-5.5.0+-yellow.svg)](https://www.knime.com/)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)

KNIME extension for querying football data from [API-Sports.io](https://api-sports.io).

This extension provides a comprehensive set of KNIME nodes for accessing football (soccer) data including teams, players, fixtures, standings, predictions, odds, and more. Perfect for sports analytics, data science projects, and building automated football data pipelines.

## Features

- **14+ Query Nodes** - Access teams, players, fixtures, standings, predictions, odds, and more
- **Smart Caching** - Reduces API calls with configurable TTL caching
- **Rate Limiting** - Automatic rate limiting based on your subscription tier
- **Reference Data** - SQLite-backed reference data for fast dropdown population
- **Flow Variables** - Full support for KNIME flow variables for dynamic workflows

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

3. **Configure Connection** - Enter your API key and select subscription tier

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
```

### Bundle Overview

| Bundle | Purpose |
|--------|---------|
| `com.apisports.knime.core` | HTTP client, caching, rate limiting, API utilities |
| `com.apisports.knime.port` | Custom KNIME port objects for passing connections |
| `com.apisports.knime.connector` | API-Sports Connector and API Statistics nodes |
| `com.apisports.knime.football` | All football query nodes |

## Available Nodes

### Connection Nodes

| Node | Description |
|------|-------------|
| **API-Sports Connector** | Authenticates and creates connection to API-Sports |
| **API Statistics** | Displays API usage metrics (calls, cache hits) |

### Reference Data

| Node | Description |
|------|-------------|
| **Reference Data Loader** | Loads and caches countries, leagues, seasons, teams, venues |

### Team & Organization Nodes

| Node | Description |
|------|-------------|
| **Teams** | Query team information with optional statistics |
| **Coaches** | Query coach information by team |
| **Venues** | Query stadium and venue information |

### Match Data Nodes

| Node | Description |
|------|-------------|
| **Fixtures** | Query matches (by league, date, team, live, head-to-head) |
| **Fixtures Selector** | Lightweight fixture browser for selection |
| **Standings** | Query league tables and rankings |

### Player Data Nodes

| Node | Description |
|------|-------------|
| **Players** | Query players (top scorers, assists, cards, search) |
| **Players Selector** | Lightweight player browser for selection |
| **Player Stats** | Detailed match-by-match statistics |
| **Injuries** | Query player injury information |
| **Sidelined** | Query player absences (injuries, suspensions) |
| **Transfers** | Query player transfer data |
| **Trophies** | Query achievements for players or coaches |

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
- **Subscription Tier** - Free, Basic, Pro, or Ultra (affects rate limiting)

**Output:**
- API Connection port object (connect to other nodes)

---

### Reference Data Loader

Loads reference data and caches it in a local SQLite database for fast access.

**Configuration:**
- **Load Teams** - Whether to load team data (can take several minutes)
- **Load Venues** - Whether to load venue data
- **Cache TTL** - How long to cache data (default: 24 hours)

**Output:**
- Reference Data port object (provides dropdowns in query nodes)

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

---

### Players

Query player data with multiple query modes.

**Query Modes:**
- **Top Scorers** - League top scorers
- **Top Assists** - League top assists
- **Top Yellow Cards** - Most yellow cards
- **Top Red Cards** - Most red cards
- **Search by Name** - Find players by name
- **By Player ID** - Specific player details

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

### Pattern 4: API Monitoring
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

The extension automatically handles rate limiting based on your configured tier.

## Caching

The extension includes intelligent caching to reduce API calls:

- **Reference Data** - Cached in SQLite (default: 24 hours)
- **API Responses** - In-memory cache with configurable TTL
- **Cache Hits** - Viewable in API Statistics node

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `APISPORTS_CACHE_DIR` | Cache directory location | `~/.apisports/` |

### SQLite Database

Reference data is stored in `~/.apisports/football_ref.db` with tables:
- `countries` - Country reference data
- `leagues` - League information
- `seasons` - Available seasons
- `teams` - Team information
- `team_leagues` - Team-league relationships

## Documentation

- [User Guide](docs/user/football-nodes-guide.md) - Complete node reference
- [Architecture](docs/architecture/descriptor-schema.md) - Technical design
- [Development](docs/development/v2-implementation-summary.md) - Implementation details

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

1. Clone the repository
2. Import into Eclipse with Tycho support
3. Set target platform to KNIME 5.5.0+
4. Run `mvn clean verify` to build

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Third-Party Licenses

This project uses the following open source libraries:

- [Jackson](https://github.com/FasterXML/jackson) - Apache 2.0
- [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - Apache 2.0
- [SnakeYAML](https://github.com/snakeyaml/snakeyaml) - Apache 2.0

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
