# Sample Output Reference

This document shows representative sample data from the Teams and Players nodes to help users understand the expected output format.

---

## Teams Node Output (Sample - 3 rows)

### Basic Team Information (Columns 1-14)

| Team_ID | Team_Name | Team_Code | Team_Country | Team_Founded | Team_National | Team_Logo | Venue_ID | Venue_Name | Venue_Address | Venue_City | Venue_Capacity | Venue_Surface | Venue_Image |
|---------|-----------|-----------|--------------|--------------|---------------|-----------|----------|------------|---------------|------------|----------------|---------------|-------------|
| 33 | Manchester United | MUN | England | 1878 | false | https://media.api-sports.io/football/teams/33.png | 556 | Old Trafford | Sir Matt Busby Way | Manchester | 76212 | grass | https://media.api-sports.io/football/venues/556.png |
| 40 | Liverpool | LIV | England | 1892 | false | https://media.api-sports.io/football/teams/40.png | 550 | Anfield | Anfield Road | Liverpool | 55212 | grass | https://media.api-sports.io/football/venues/550.png |
| 50 | Manchester City | MCI | England | 1880 | false | https://media.api-sports.io/football/teams/50.png | 555 | Etihad Stadium | Rowsley Street | Manchester | 55097 | grass | https://media.api-sports.io/football/venues/555.png |

### Statistics - Form & Fixtures (Columns 15-27)

| Form | Fixtures_Played_Home | Fixtures_Played_Away | Fixtures_Played_Total | Fixtures_Wins_Home | Fixtures_Wins_Away | Fixtures_Wins_Total | Fixtures_Draws_Home | Fixtures_Draws_Away | Fixtures_Draws_Total | Fixtures_Losses_Home | Fixtures_Losses_Away | Fixtures_Losses_Total |
|------|----------------------|----------------------|-----------------------|--------------------|--------------------|--------------------|---------------------|---------------------|---------------------|----------------------|----------------------|----------------------|
| WDWLW | 15 | 15 | 30 | 8 | 6 | 14 | 4 | 3 | 7 | 3 | 6 | 9 |
| WWWDW | 15 | 15 | 30 | 11 | 9 | 20 | 3 | 4 | 7 | 1 | 2 | 3 |
| WWWWW | 15 | 15 | 30 | 13 | 10 | 23 | 2 | 3 | 5 | 0 | 2 | 2 |

### Statistics - Goals For (Columns 28-59)

| Goals_For_Total_Home | Goals_For_Total_Away | Goals_For_Total_Total | Goals_For_Average_Home | Goals_For_Average_Away | Goals_For_Average_Total |
|----------------------|----------------------|-----------------------|------------------------|------------------------|-------------------------|
| 28 | 22 | 50 | 1.87 | 1.47 | 1.67 |
| 42 | 35 | 77 | 2.80 | 2.33 | 2.57 |
| 48 | 40 | 88 | 3.20 | 2.67 | 2.93 |

**Goals For - Minute Distribution (sample for Manchester City)**:

| Period | Total | Percentage |
|--------|-------|------------|
| 0-15 min | 8 | 9.09% |
| 16-30 min | 12 | 13.64% |
| 31-45 min | 15 | 17.05% |
| 46-60 min | 10 | 11.36% |
| 61-75 min | 18 | 20.45% |
| 76-90 min | 22 | 25.00% |
| 91-105 min | 3 | 3.41% |
| 106-120 min | 0 | 0.00% |

### Statistics - Goals Against (Columns 60-91)

| Goals_Against_Total_Home | Goals_Against_Total_Away | Goals_Against_Total_Total | Goals_Against_Average_Home | Goals_Against_Average_Away | Goals_Against_Average_Total |
|--------------------------|--------------------------|---------------------------|----------------------------|----------------------------|----------------------------|
| 18 | 22 | 40 | 1.20 | 1.47 | 1.33 |
| 12 | 16 | 28 | 0.80 | 1.07 | 0.93 |
| 10 | 15 | 25 | 0.67 | 1.00 | 0.83 |

### Statistics - Biggest (Columns 92-102)

| Biggest_Streak_Wins | Biggest_Streak_Draws | Biggest_Streak_Losses | Biggest_Wins_Home | Biggest_Wins_Away | Biggest_Losses_Home | Biggest_Losses_Away | Biggest_Goals_For_Home | Biggest_Goals_For_Away | Biggest_Goals_Against_Home | Biggest_Goals_Against_Away |
|---------------------|----------------------|-----------------------|-------------------|-------------------|---------------------|---------------------|------------------------|------------------------|----------------------------|----------------------------|
| 4 | 2 | 2 | 4-0 | 3-0 | 0-3 | 0-4 | 4 | 3 | 3 | 4 |
| 7 | 1 | 0 | 5-0 | 4-0 | 0-1 | 0-2 | 5 | 4 | 1 | 2 |
| 10 | 1 | 0 | 6-0 | 5-1 | null | 0-2 | 6 | 5 | 0 | 2 |

### Statistics - Clean Sheets & Failed to Score (Columns 103-108)

| Clean_Sheet_Home | Clean_Sheet_Away | Clean_Sheet_Total | Failed_To_Score_Home | Failed_To_Score_Away | Failed_To_Score_Total |
|------------------|------------------|-------------------|----------------------|----------------------|----------------------|
| 5 | 3 | 8 | 2 | 4 | 6 |
| 8 | 6 | 14 | 1 | 2 | 3 |
| 10 | 7 | 17 | 0 | 1 | 1 |

### Statistics - Penalties (Columns 109-113)

| Penalty_Scored_Total | Penalty_Scored_Percentage | Penalty_Missed_Total | Penalty_Missed_Percentage | Penalty_Total |
|----------------------|---------------------------|----------------------|---------------------------|---------------|
| 5 | 83.33% | 1 | 16.67% | 6 |
| 8 | 88.89% | 1 | 11.11% | 9 |
| 7 | 100.00% | 0 | 0.00% | 7 |

### Statistics - Lineups (Column 114)

| Lineups |
|---------|
| 4-2-3-1:18, 4-3-3:8, 3-4-3:4 |
| 4-3-3:25, 4-2-3-1:5 |
| 4-3-3:20, 3-2-4-1:8, 4-2-3-1:2 |

### Statistics - Cards Yellow (Columns 115-130)

**Sample for Manchester United**:

| Period | Total | Percentage |
|--------|-------|------------|
| 0-15 min | 3 | 5.26% |
| 16-30 min | 6 | 10.53% |
| 31-45 min | 12 | 21.05% |
| 46-60 min | 8 | 14.04% |
| 61-75 min | 10 | 17.54% |
| 76-90 min | 15 | 26.32% |
| 91-105 min | 3 | 5.26% |
| 106-120 min | 0 | 0.00% |

### Statistics - Cards Red (Columns 131-146)

**Sample for Manchester United**:

| Period | Total | Percentage |
|--------|-------|------------|
| 0-15 min | 0 | 0.00% |
| 16-30 min | 0 | 0.00% |
| 31-45 min | 0 | 0.00% |
| 46-60 min | 1 | 33.33% |
| 61-75 min | 1 | 33.33% |
| 76-90 min | 1 | 33.33% |
| 91-105 min | 0 | 0.00% |
| 106-120 min | 0 | 0.00% |

---

## Players Node Output - Top Scorers (Sample - 5 rows)

| Player_ID | Name | Firstname | Lastname | Nationality | Age | Team | Position | Appearances | Goals | Assists | Yellow_Cards | Red_Cards | Rating |
|-----------|------|-----------|----------|-------------|-----|------|----------|-------------|-------|---------|--------------|-----------|--------|
| 1100 | E. Haaland | Erling | Haaland | Norway | 24 | Manchester City | Attacker | 30 | 25 | 5 | 3 | 0 | 7.45 |
| 184 | M. Salah | Mohamed | Salah | Egypt | 32 | Liverpool | Attacker | 32 | 20 | 12 | 1 | 0 | 7.62 |
| 909 | C. Palmer | Cole | Palmer | England | 22 | Chelsea | Midfielder | 28 | 18 | 8 | 2 | 0 | 7.38 |
| 186 | D. Nunez | Darwin | Nunez | Uruguay | 25 | Liverpool | Attacker | 30 | 15 | 6 | 4 | 1 | 6.95 |
| 1467 | A. Isak | Alexander | Isak | Sweden | 24 | Newcastle United | Attacker | 26 | 14 | 3 | 2 | 0 | 7.12 |

---

## Players Node Output - Top Assists (Sample - 5 rows)

| Player_ID | Name | Firstname | Lastname | Nationality | Age | Team | Position | Appearances | Goals | Assists | Yellow_Cards | Red_Cards | Rating |
|-----------|------|-----------|----------|-------------|-----|------|----------|-------------|-------|---------|--------------|-----------|--------|
| 903 | K. De Bruyne | Kevin | De Bruyne | Belgium | 33 | Manchester City | Midfielder | 18 | 3 | 12 | 1 | 0 | 7.82 |
| 184 | M. Salah | Mohamed | Salah | Egypt | 32 | Liverpool | Attacker | 32 | 20 | 12 | 1 | 0 | 7.62 |
| 306 | B. Saka | Bukayo | Saka | England | 22 | Arsenal | Attacker | 30 | 12 | 10 | 3 | 0 | 7.55 |
| 1485 | P. Foden | Phil | Foden | England | 24 | Manchester City | Midfielder | 28 | 10 | 9 | 2 | 0 | 7.48 |
| 909 | C. Palmer | Cole | Palmer | England | 22 | Chelsea | Midfielder | 28 | 18 | 8 | 2 | 0 | 7.38 |

---

## Players Node Output - Top Yellow Cards (Sample - 5 rows)

| Player_ID | Name | Firstname | Lastname | Nationality | Age | Team | Position | Appearances | Goals | Assists | Yellow_Cards | Red_Cards | Rating |
|-----------|------|-----------|----------|-------------|-----|------|----------|-------------|-------|---------|--------------|-----------|--------|
| 2931 | J. Schlupp | Jeffrey | Schlupp | Ghana | 31 | Crystal Palace | Midfielder | 28 | 1 | 2 | 11 | 0 | 6.72 |
| 560 | D. Rice | Declan | Rice | England | 25 | Arsenal | Midfielder | 32 | 4 | 5 | 10 | 1 | 7.15 |
| 892 | M. Guendouzi | Matteo | Guendouzi | France | 24 | Aston Villa | Midfielder | 30 | 2 | 4 | 10 | 0 | 6.88 |
| 1234 | B. Guimaraes | Bruno | Guimaraes | Brazil | 26 | Newcastle United | Midfielder | 31 | 5 | 4 | 9 | 0 | 7.22 |
| 745 | J. Stones | John | Stones | England | 30 | Manchester City | Defender | 25 | 2 | 1 | 9 | 0 | 7.08 |

---

## Players Node Output - Players by Team (Sample - Manchester City - 5 rows)

| Player_ID | Name | Firstname | Lastname | Nationality | Age | Team | Position | Appearances | Goals | Assists | Yellow_Cards | Red_Cards | Rating |
|-----------|------|-----------|----------|-------------|-----|------|----------|-------------|-------|---------|--------------|-----------|--------|
| 1100 | E. Haaland | Erling | Haaland | Norway | 24 | Manchester City | Attacker | 30 | 25 | 5 | 3 | 0 | 7.45 |
| 903 | K. De Bruyne | Kevin | De Bruyne | Belgium | 33 | Manchester City | Midfielder | 18 | 3 | 12 | 1 | 0 | 7.82 |
| 1485 | P. Foden | Phil | Foden | England | 24 | Manchester City | Midfielder | 28 | 10 | 9 | 2 | 0 | 7.48 |
| 617 | R. Dias | Ruben | Dias | Portugal | 27 | Manchester City | Defender | 32 | 2 | 0 | 4 | 0 | 7.18 |
| 623 | Ederson | Ederson | Moraes | Brazil | 30 | Manchester City | Goalkeeper | 30 | 0 | 1 | 1 | 0 | 6.95 |

---

## Joined Output: Teams + Top Scorers (Sample)

| Team_ID | Team_Name | Fixtures_Wins_Total | Goals_For_Total_Total | Clean_Sheet_Total | Player_ID | Name | Goals | Assists | Rating |
|---------|-----------|--------------------|-----------------------|-------------------|-----------|------|-------|---------|--------|
| 50 | Manchester City | 23 | 88 | 17 | 1100 | E. Haaland | 25 | 5 | 7.45 |
| 40 | Liverpool | 20 | 77 | 14 | 184 | M. Salah | 20 | 12 | 7.62 |
| 49 | Chelsea | 15 | 62 | 10 | 909 | C. Palmer | 18 | 8 | 7.38 |
| 40 | Liverpool | 20 | 77 | 14 | 186 | D. Nunez | 15 | 6 | 6.95 |
| 34 | Newcastle United | 16 | 58 | 9 | 1467 | A. Isak | 14 | 3 | 7.12 |

---

## Analysis Output Examples

### Goal Scoring Pattern Analysis (Aggregated)

| Time_Period | Total_Goals | Percentage |
|-------------|-------------|------------|
| 76-90 min | 245 | 22.5% |
| 61-75 min | 198 | 18.2% |
| 31-45 min | 165 | 15.1% |
| 46-60 min | 155 | 14.2% |
| 16-30 min | 142 | 13.0% |
| 0-15 min | 110 | 10.1% |
| 91-105 min | 68 | 6.2% |
| 106-120 min | 7 | 0.6% |

**Insight**: Most goals are scored in the final 15 minutes (76-90), likely due to fatigue and tactical changes.

### Home Advantage Analysis

| Team_Name | Wins_Home | Wins_Away | Home_Advantage |
|-----------|-----------|-----------|----------------|
| Liverpool | 11 | 9 | +2 |
| Manchester City | 13 | 10 | +3 |
| Arsenal | 12 | 8 | +4 |
| Chelsea | 9 | 6 | +3 |
| Manchester United | 8 | 6 | +2 |

**Insight**: Arsenal has the strongest home advantage (+4 wins difference).

### Defensive Strength Ranking

| Team_Name | Clean_Sheets | Goals_Against | Defensive_Rating |
|-----------|--------------|---------------|------------------|
| Manchester City | 17 | 25 | A+ |
| Liverpool | 14 | 28 | A |
| Arsenal | 13 | 32 | A- |
| Newcastle United | 9 | 42 | B |
| Manchester United | 8 | 40 | B |

---

## Data Quality Notes

1. **Missing Values**: Statistics columns may contain missing values for:
   - Teams with few matches played
   - Historical seasons with incomplete data
   - Newly promoted teams

2. **String vs Numeric**: Some values are returned as strings (Age, Goals, Assists in Players node) for flexibility. Convert to numbers for calculations.

3. **Percentage Format**: Percentage columns include the "%" symbol (e.g., "25.00%"). Remove symbol for numeric operations.

4. **Null Handling**: KNIME displays missing cells as "?" - use Missing Value node to handle before analysis.

5. **API Timing**: Statistics reflect data at query time. Re-execute for latest data.
