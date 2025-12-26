# Carovex Endpoint Descriptor Schema

## Overview

Endpoint descriptors define API endpoints in a machine-readable format (YAML/JSON) that drives:
- Universal node dynamic UI generation
- Parameter validation
- Caching policies
- JSON-to-table flattening
- Documentation generation

## Schema Definition

```yaml
# Top-level structure
version: "1.0"              # Schema version
sport: "football"           # Sport module identifier
endpoints:                  # Array of endpoint definitions
  - <endpoint>
  - <endpoint>
  ...
```

## Endpoint Structure

```yaml
id: string                  # Unique endpoint identifier (e.g., "fixtures_by_league")
path: string                # API path (e.g., "/fixtures")
category: string            # Grouping category (e.g., "Fixtures", "Teams", "Players")
subcategory: string?        # Optional subcategory
description: string         # Human-readable description
keywords: string[]?         # Search keywords for discovery

# HTTP method (default: GET)
method: "GET" | "POST"      # Only GET for v1

# Parameters
params:
  - name: string            # Parameter name
    type: "integer" | "string" | "boolean" | "date" | "enum"
    required: boolean       # Is this parameter required?
    description: string     # Help text for UI
    default: any?           # Default value (optional)

    # For integer types
    min: number?            # Minimum value
    max: number?            # Maximum value

    # For enum types
    enum: string[]?         # Allowed values
    enumLabels: string[]?   # Display labels for enum values

    # For string types
    pattern: regex?         # Validation regex
    minLength: number?
    maxLength: number?

    # For date types
    format: string?         # Date format (e.g., "YYYY-MM-DD")

# Validation rules
validation:
  requiredParams: string[]?           # List of required param names
  requiresAtLeastOneOf: string[]?     # At least one of these params must be present
  requiresOneOfGroups: string[][]?    # Exactly one param from each group must be present
  mutuallyExclusive: string[][]?      # Params that cannot be used together
  conditionalRequired:                # If param X is set, then param Y is required
    - when: string                    # Param name to check
      equals: any                     # Value to match
      then: string[]                  # Params that become required

# Paging behavior
paging:
  supported: boolean                  # Does endpoint support paging?
  paramName: string?                  # Page param name (default: "page")
  defaultPageSize: number?            # Typical page size (for UX estimation)
  maxPages: number?                   # Safety cap (default: 25)

# Caching policy
caching:
  policy: "static" | "reference" | "hourly" | "live" | "none"
  ttl: number?                        # Time-to-live in seconds (overrides policy default)
  description: string?                # Explain why this policy

# Response structure and flattening
response:
  rootPath: string                    # JSONPath to data array (e.g., "response")
  type: "array" | "object"            # Is response an array or single object?

  # Column mapping rules
  flatten:
    prefix: string?                   # Column name prefix (e.g., "fixture_")

    # How to handle nested objects
    nestedObjects:
      - path: string                  # JSONPath (e.g., "teams.home")
        strategy: "flatten" | "json"  # flatten: create columns, json: single JSON column
        prefix: string?               # Prefix for flattened columns

    # How to handle nested arrays
    nestedArrays:
      - path: string                  # JSONPath (e.g., "events")
        strategy: "stringify" | "explode" | "ignore"
        prefix: string?               # Prefix if exploded

    # Special column handling
    renameColumns:                    # Map API field names to display names
      - from: string
        to: string

    excludeColumns: string[]?         # Columns to skip

# Metadata
metadata:
  apiTier: "free" | "basic" | "pro" | "ultra"?  # Minimum tier required
  rateLimit: string?                  # e.g., "10/min" for documentation
  quotaWeight: number?                # Relative cost (1 = normal, 2 = expensive)

# Example usage (for documentation)
examples:
  - title: string
    params:
      paramName: value
    description: string?
```

## Policy Default TTLs

| Policy | TTL (seconds) | Use Case |
|--------|---------------|----------|
| `static` | 2592000 (30 days) | Countries, timezones, rarely changing reference |
| `reference` | 86400 (1 day) | Leagues, teams, venues, seasons |
| `hourly` | 3600 (1 hour) | Standings, statistics (updated ~daily) |
| `live` | 300 (5 min) | Fixtures, odds, live scores |
| `none` | 0 | Always fetch fresh |

## Example Descriptor

```yaml
id: fixtures_by_league
path: /fixtures
category: Fixtures
description: Get fixtures for a specific league and season
keywords:
  - matches
  - games
  - schedule

params:
  - name: league
    type: integer
    required: true
    description: League ID (e.g., 39 for Premier League)
    min: 1

  - name: season
    type: integer
    required: true
    description: Season year (e.g., 2024)
    min: 2000
    max: 2030

  - name: team
    type: integer
    required: false
    description: Filter by team ID
    min: 1

  - name: date
    type: string
    required: false
    description: Filter by date (YYYY-MM-DD)
    pattern: "^\\d{4}-\\d{2}-\\d{2}$"

  - name: status
    type: enum
    required: false
    description: Match status filter
    enum: ["TBD", "NS", "1H", "HT", "2H", "ET", "P", "FT", "AET", "PEN", "BT", "SUSP", "INT", "PST", "CANC", "ABD", "AWD", "WO"]
    enumLabels: ["Time to be defined", "Not Started", "First Half", "Halftime", "Second Half", "Extra Time", "Penalty", "Match Finished", "After Extra Time", "Penalties Finished", "Break Time", "Match Suspended", "Match Interrupted", "Match Postponed", "Match Cancelled", "Match Abandoned", "Match Awarded", "Match Walkover"]

validation:
  requiredParams: [league, season]

paging:
  supported: false

caching:
  policy: live
  ttl: 300
  description: Fixtures change frequently (lineups, status updates)

response:
  rootPath: response
  type: array
  flatten:
    prefix: fixture_
    nestedObjects:
      - path: fixture
        strategy: flatten
        prefix: ""
      - path: league
        strategy: flatten
        prefix: league_
      - path: teams.home
        strategy: flatten
        prefix: home_
      - path: teams.away
        strategy: flatten
        prefix: away_
      - path: goals
        strategy: flatten
        prefix: goals_
      - path: score
        strategy: json
    nestedArrays:
      - path: events
        strategy: stringify
      - path: lineups
        strategy: stringify

metadata:
  apiTier: free
  rateLimit: 10/min
  quotaWeight: 1

examples:
  - title: Get all Premier League fixtures for 2024 season
    params:
      league: 39
      season: 2024
    description: Returns all fixtures for Premier League 2024

  - title: Get Liverpool fixtures
    params:
      league: 39
      season: 2024
      team: 40
    description: Returns only Liverpool fixtures
```

## Validation During Load

The descriptor loader MUST validate:
1. Required fields present (id, path, category, params, response)
2. No duplicate endpoint IDs
3. Parameter types valid
4. Validation rules reference valid param names
5. Response flatten paths don't conflict
6. TTL values are positive integers
7. Enum arrays have matching enumLabels length (if provided)

## Future Extensions

- `POST` method support with request body schema
- Query parameter templates (e.g., `season=${flow.season}`)
- Response transformation functions (date parsing, unit conversions)
- Multi-output port support (primary table + metadata table)
