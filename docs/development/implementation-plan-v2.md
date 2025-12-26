# Implementation Plan: Football API v2.0
**Date**: 2025-12-19
**Branch**: `claude/restore-progress-setup-wQBIY`

## Requirements Summary

### 1. Reference Data Loader Node
- **Storage**: SQLite database (`football_ref.db`)
- **Data**: Countries, Leagues, Teams, Seasons
- **Country Filter**: By name only (user never sees IDs)
- **Seasons**: Load from `/leagues` endpoint, dynamic (2008-present)
- **Sync**: Button to wipe and reload database

### 2. Universal Query Node
- **Inputs**:
  1. API-Connector port
  2. ReferenceDataPortObject port
- **UI**: Cascading dropdowns (no JSON text box):
  - Country (optional filter)
  - League (filtered by country)
  - Season (multi-select, filtered by league)
  - Team (filtered by league)
  - Endpoint (determines required parameters)
- **Output**: Standard KNIME BufferedDataTable

### 3. Technical Requirements
- **DAO**: ReferenceDAO class with:
  - `getAllLeagues()`
  - `getTeamsByLeague(int leagueId)`
  - `getAllSeasons()`
  - `upsertLeagues(List<League>)`
  - `upsertTeams(List<Team>)`
  - `upsertSeasons(List<Season>)`
- **Security**: Prepared statements (SQL injection prevention)
- **Timeout**: 10 seconds for HTTP calls
- **Validation**: Disable Execute if SQLite empty

---

## Implementation Phases

### Phase 1: SQLite Infrastructure (Files: 4-5)

#### 1.1 Add SQLite Dependency
**File**: `pom.xml` (project root)
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.43.0.0</version>
</dependency>
```

#### 1.2 Create Season Model
**File**: `bundles/com.apisports.knime.port/src/main/java/com/apisports/knime/port/ReferenceData.java`
- Add inner class `Season` with fields:
  - `int leagueId`
  - `int year`
  - `String startDate`
  - `String endDate`
  - `boolean current`

#### 1.3 Create ReferenceDAO
**File**: `bundles/com.apisports.knime.core/src/main/java/com/apisports/knime/core/dao/ReferenceDAO.java`
- Database schema:
  ```sql
  CREATE TABLE countries (name TEXT PRIMARY KEY, code TEXT, flag TEXT);
  CREATE TABLE leagues (id INTEGER PRIMARY KEY, name TEXT, type TEXT, country_name TEXT, logo TEXT);
  CREATE TABLE seasons (league_id INTEGER, year INTEGER, start_date TEXT, end_date TEXT, is_current INTEGER, PRIMARY KEY(league_id, year));
  CREATE TABLE teams (id INTEGER PRIMARY KEY, name TEXT, code TEXT, country TEXT, logo TEXT);
  CREATE TABLE team_leagues (team_id INTEGER, league_id INTEGER, PRIMARY KEY(team_id, league_id));
  ```
- Methods:
  - `void initializeDatabase(String dbPath)`
  - `void clearAll()`
  - `List<Country> getAllCountries()`
  - `List<League> getAllLeagues()`
  - `List<League> getLeaguesByCountry(String countryName)`
  - `List<Season> getSeasonsByLeague(int leagueId)`
  - `List<Team> getTeamsByLeague(int leagueId)`
  - `void upsertCountries(List<Country> countries)`
  - `void upsertLeagues(List<League> leagues)`
  - `void upsertSeasons(List<Season> seasons)`
  - `void upsertTeams(List<Team> teams)`

---

### Phase 2: Reference Data Loader Node (Files: 3-4)

#### 2.1 Update ReferenceDataLoaderNodeModel
**File**: `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/referencedata/ReferenceDataLoaderNodeModel.java`

**Changes**:
1. Add settings:
   - `SettingsModelStringArray m_countryFilter` (list of country names)
   - `SettingsModelString m_dbPath` (default: `${user.home}/.apisports/football_ref.db`)

2. Modify `execute()`:
   - Create ReferenceDAO instance
   - Initialize database at `m_dbPath`
   - If "Sync" requested: call `dao.clearAll()`
   - Load countries → filter by `m_countryFilter` if set
   - Load leagues (all) → extract seasons for each league
   - For filtered leagues: load teams
   - Save all to SQLite via DAO
   - Return `ReferenceDataPortObject` with DB path

3. Add `loadSeasons()`:
   ```java
   private List<Season> loadSeasons(ApiSportsHttpClient client, ObjectMapper mapper, List<League> leagues) {
       // For each league, GET /leagues?id={leagueId}
       // Extract seasons array: [{"year": 2024, "start": "...", "end": "...", "current": true}, ...]
       // Create Season objects
   }
   ```

#### 2.2 Update ReferenceDataLoaderNodeDialog
**File**: `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/referencedata/ReferenceDataLoaderNodeDialog.java`

**Changes**:
1. Add country filter (multi-select list):
   ```java
   addDialogComponent(new DialogComponentStringListSelection(
       m_countryFilter,
       "Filter by Countries (leave empty for all):",
       Arrays.asList("France", "England", "Spain", "Germany", "Italy", ...)
   ));
   ```
2. Add DB path setting (file chooser)
3. Add "Clear & Reload" checkbox for sync behavior

#### 2.3 Update ReferenceDataPortObject
**File**: `bundles/com.apisports.knime.port/src/main/java/com/apisports/knime/port/ReferenceDataPortObject.java`

**Changes**:
1. Replace in-memory lists with:
   - `String dbFilePath`
   - `transient ReferenceDAO dao` (lazy-loaded)
2. Update serialization to save only `dbFilePath`
3. Add getters that query DAO:
   ```java
   public List<League> getLeagues() {
       return getDAO().getAllLeagues();
   }
   ```

---

### Phase 3: Universal Node (Files: 2-3)

#### 3.1 Update UniversalNodeModel
**File**: `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/universal/UniversalNodeModel.java`

**Changes**:
1. Update constructor:
   ```java
   super(
       new PortType[]{
           ApiSportsConnectionPortObject.TYPE,
           ReferenceDataPortObject.TYPE  // NEW
       },
       new PortType[]{BufferedDataTable.TYPE}
   );
   ```

2. Add settings:
   - `SettingsModelString m_selectedCountry` (optional)
   - `SettingsModelInteger m_selectedLeagueId`
   - `SettingsModelIntegerArray m_selectedSeasons` (multi-select)
   - `SettingsModelInteger m_selectedTeamId` (optional)
   - `SettingsModelString m_selectedEndpoint`

3. Update `execute()`:
   ```java
   ReferenceDataPortObject refData = (ReferenceDataPortObject) inObjects[1];

   // Build parameters from selections
   Map<String, String> params = new HashMap<>();
   if (m_selectedLeagueId.getIntValue() > 0) {
       params.put("league", String.valueOf(m_selectedLeagueId.getIntValue()));
   }
   if (m_selectedSeasons.getIntArrayValue().length > 0) {
       params.put("season", String.valueOf(m_selectedSeasons.getIntArrayValue()[0])); // Use first season
   }
   if (m_selectedTeamId.getIntValue() > 0) {
       params.put("team", String.valueOf(m_selectedTeamId.getIntValue()));
   }

   // Get endpoint descriptor
   EndpointDescriptor descriptor = DescriptorRegistry.getInstance()
       .getDescriptor(m_selectedEndpoint.getStringValue());

   // Execute API call
   String response = client.get(descriptor.getPath(), params);

   // Flatten to table
   BufferedDataTable table = flattenJsonToTable(response, exec, descriptor);
   return new PortObject[]{table};
   ```

#### 3.2 Update UniversalNodeDialog
**File**: `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/universal/UniversalNodeDialog.java`

**Changes**:
Replace entire dialog with cascading dropdowns:

```java
// Country dropdown (optional)
DialogComponentStringSelection countryDropdown = new DialogComponentStringSelection(
    m_selectedCountry, "Country (optional):", countryNames
);

// League dropdown (filtered by country)
DialogComponentStringSelection leagueDropdown = new DialogComponentStringSelection(
    m_selectedLeague, "League:", leagueNames
);

// Season dropdown (multi-select, filtered by league)
DialogComponentStringListSelection seasonDropdown = new DialogComponentStringListSelection(
    m_selectedSeasons, "Seasons:", seasonYears
);

// Team dropdown (filtered by league)
DialogComponentStringSelection teamDropdown = new DialogComponentStringSelection(
    m_selectedTeam, "Team (optional):", teamNames
);

// Endpoint dropdown
DialogComponentStringSelection endpointDropdown = new DialogComponentStringSelection(
    m_selectedEndpoint, "Endpoint:", endpointIds
);

// Add listeners:
// - Country change → reload leagues
// - League change → reload seasons & teams
```

---

### Phase 4: Endpoint Parameter Mapping (Files: 1)

#### 4.1 Create Mapping Table
**File**: `bundles/com.apisports.knime.core/src/main/resources/endpoint-parameter-mapping.yaml`

```yaml
endpoints:
  - id: fixtures_by_league
    path: /fixtures
    requiredParams: [league, season]
    optionalParams: [team, date, status]

  - id: players_by_team
    path: /players
    requiredParams: [team, season]
    optionalParams: [search]

  - id: odds_by_fixture
    path: /odds
    requiredParams: [league, season]
    optionalParams: [bookmaker]

  - id: standings_by_league
    path: /standings
    requiredParams: [league, season]

  - id: injuries_by_league
    path: /injuries
    requiredParams: [league, season]
    optionalParams: [team]
```

Update `EndpointDescriptor` to include this mapping.

---

### Phase 5: Testing & Validation

#### Test Cases:
1. **Reference Loader**:
   - [ ] Loads countries successfully
   - [ ] Filters by France (or other country)
   - [ ] Loads all French leagues
   - [ ] Loads seasons for each league (2008-2025)
   - [ ] Loads teams for each league
   - [ ] Saves to SQLite (`football_ref.db`)
   - [ ] "Sync" clears and reloads

2. **Universal Node**:
   - [ ] Country dropdown filters leagues
   - [ ] League selection loads seasons
   - [ ] League selection loads teams
   - [ ] Multi-season selection works
   - [ ] Endpoint selection shows/hides dropdowns based on requirements
   - [ ] Execute button disabled if no reference data
   - [ ] API call succeeds with correct parameters
   - [ ] Output is valid BufferedDataTable

3. **Use Case Validation**:
   - [ ] Use Case #1: Live Scoreboard (`/fixtures?live=all`)
   - [ ] Use Case #8: Team Form (`/teams/statistics?league=39&season=2025&team=40`)
   - [ ] Use Case #11: Odds Arbitrage (`/odds?fixture={id}`)
   - [ ] (Test representative samples from all 4 categories)

---

## File Inventory

### New Files (7):
1. `bundles/com.apisports.knime.core/src/main/java/com/apisports/knime/core/dao/ReferenceDAO.java`
2. `bundles/com.apisports.knime.core/src/main/resources/endpoint-parameter-mapping.yaml`
3. `docs/implementation-plan-v2.md` (this file)

### Modified Files (6):
1. `pom.xml` (add SQLite dependency)
2. `bundles/com.apisports.knime.port/src/main/java/com/apisports/knime/port/ReferenceData.java` (add Season class)
3. `bundles/com.apisports.knime.port/src/main/java/com/apisports/knime/port/ReferenceDataPortObject.java` (use SQLite)
4. `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/referencedata/ReferenceDataLoaderNodeModel.java`
5. `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/referencedata/ReferenceDataLoaderNodeDialog.java`
6. `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/universal/UniversalNodeModel.java`
7. `bundles/com.apisports.knime.football/src/main/java/com/apisports/knime/football/nodes/universal/UniversalNodeDialog.java`

---

## Estimated Effort
- Phase 1 (SQLite Infrastructure): 2-3 hours
- Phase 2 (Reference Loader): 2-3 hours
- Phase 3 (Universal Node): 3-4 hours
- Phase 4 (Mapping): 1 hour
- Phase 5 (Testing): 2-3 hours
**Total**: 10-14 hours

---

## Success Criteria
✅ User can execute Reference Loader with country filter (e.g., "France")
✅ SQLite database created at `~/.apisports/football_ref.db`
✅ Database contains: countries, leagues (with France filter), teams, seasons (2008-2025)
✅ Universal Node has 2 input ports (API-Connector + Reference)
✅ Universal Node UI shows cascading dropdowns (Country → League → Season → Team → Endpoint)
✅ User selects "Premier League" → "2025" → "Liverpool" → "Team Statistics"
✅ Universal Node executes API call with correct URL: `/teams/statistics?league=39&season=2025&team=40`
✅ Output is standard KNIME BufferedDataTable
✅ All 20 use cases are testable via dropdown combinations
