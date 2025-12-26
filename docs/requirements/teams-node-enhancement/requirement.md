# Teams Node Enhancement - Complete API Field Coverage

## Overview
Ensure the Teams node captures ALL fields returned by the API-Sports `/teams` and `/teams/statistics` endpoints. Currently, some fields from the API response are not included in the node's output table.

## Priority
- [x] High

## Affected Nodes
- Teams node (`com.apisports.knime.football.nodes.query.teams`)

## Problem Statement
The Teams node currently outputs a subset of fields available in the API response. When comparing the actual JSON responses from the API with the node's output columns, there are missing fields that users may need for analysis.

## Requirements

### Functional Requirements
1. Review the JSON responses in this directory:
   - `teams-response.json` - Response from `/teams` endpoint
   - `teams-statistics-response.json` - Response from `/teams` with statistics enabled

2. Compare with current Teams node output columns (from TeamsNodeFactory.xml):
   - Current columns: Team_ID, Team_Name, Code, Country, Founded, Venue_Name, Venue_City, Venue_Capacity, Wins, Draws, Losses, Goals_For, Goals_Against

3. Add ALL missing fields from the API response to the output table

4. Maintain backward compatibility - existing columns should remain in the same order

### Technical Requirements
- Update `TeamsNodeModel.java` to parse all fields from JSON
- Add new columns to output table spec
- Update `TeamsNodeFactory.xml` to document new columns
- Handle null/missing values gracefully (use appropriate KNIME missing value cells)

## API Responses
See JSON files in this directory:
- `teams-response.json` - Full API response without statistics
- `teams-statistics-response.json` - Full API response with statistics checkbox enabled

## Expected Fields (to be verified from JSON)
Based on API documentation, the response may include:
- Team info: id, name, code, country, founded, national, logo
- Venue info: venue.id, venue.name, venue.address, venue.city, venue.capacity, venue.surface, venue.image
- Statistics (when enabled): fixtures.played, wins, draws, losses, goals.for, goals.against, etc.

**Important**: Use the actual JSON files to determine the complete field list, not this example.

## Output Columns
After implementation, the Teams node should output columns for every field in the API response. Group related fields logically:

**Team Information:**
- Team_ID
- Team_Name
- Team_Code
- Team_Country
- Team_Founded
- Team_National (boolean)
- Team_Logo

**Venue Information:**
- Venue_ID
- Venue_Name
- Venue_Address
- Venue_City
- Venue_Capacity
- Venue_Surface
- Venue_Image

**Statistics (when checkbox enabled):**
- (All statistics fields from JSON)

## Implementation Notes
1. Read both JSON files in this directory
2. Parse the complete structure to identify all available fields
3. Update TeamsNodeModel.java:
   - Modify `parseTeamResponse()` method to extract all fields
   - Update `createOutputSpec()` to include all columns
4. Handle nested JSON structures (e.g., venue.*, statistics.*)
5. Use appropriate KNIME cell types (IntCell, StringCell, BooleanCell, etc.)

## Test Cases
1. **Test Case 1**: Query without statistics
   - Input: Execute Teams node with statistics unchecked
   - Expected: All team and venue fields populated
   - Verify: Statistics columns are empty/missing

2. **Test Case 2**: Query with statistics
   - Input: Execute Teams node with "Include Team Statistics" checked
   - Expected: All team, venue, AND statistics fields populated
   - Verify: Every field from `teams-statistics-response.json` has a corresponding column

3. **Test Case 3**: Null value handling
   - Input: Query a team with missing optional fields
   - Expected: Missing value cells (not errors) for null fields
   - Verify: Node executes successfully

## Acceptance Criteria
- [ ] Every field in `teams-response.json` has a corresponding output column
- [ ] Every field in `teams-statistics-response.json` (when statistics enabled) has a corresponding output column
- [ ] No fields are missing from the output table
- [ ] Column names are clear and follow existing naming conventions
- [ ] Null values are handled properly (no execution errors)
- [ ] TeamsNodeFactory.xml updated to document all new columns
- [ ] Backward compatibility maintained (existing workflows don't break)

## Questions
- Should we flatten all nested structures (e.g., venue.name → Venue_Name)?
  - **Recommendation**: Yes, maintain current flat table structure for KNIME compatibility

- What should we do with array fields if any exist?
  - **Recommendation**: Convert to comma-separated strings or create separate rows

---

## Status
- [ ] Not Started
- [ ] In Progress
- [ ] Completed

**Implementation**: Claude will read the JSON files and implement all changes

**Created**: 2025-12-26
