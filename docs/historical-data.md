# Historical Data

## Goals

- ensure correct historical representation
- support team and competition changes
- allow future expansion without schema changes

---

## Competition Names

Handled via:

competition_names

Example:

| competition_id | name                  | from | to   |
|----------------|----------------------|------|------|
| 1              | Tri Nations          | 1996 | 2011 |
| 1              | Rugby Championship   | 2012 | NULL |

---

## Team Names

team_names supports:

- rebrands
- sponsorship names
- historical names

---

## Competition Seasons

Each competition has independent seasons:

- URC → Sept → June
- Super Rugby → Feb → July
- Six Nations → Feb → March

---

## Competition Season Teams

Defines which teams participate in each season:

- prevents future teams appearing in past seasons
- supports expansion/relegation

---

## MVP Scope

The system supports full history, but MVP includes:

- current season
- previous season

---

## Design Principle

Entities are stable:

- teams are not duplicated per season
- competitions are not duplicated per rename

History is modeled via:

- relationships
- versioned naming