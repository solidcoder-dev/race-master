# Race Master

## What is Rally Raid?

**Rally Raid** (also called **cross-country rally**) is a motorsport competition that takes place over multiple days, where competitors race across long off-road routes divided into **stages**.  
Each **stage** is a distinct track that must be completed on a given day, often through deserts, mountains, or remote regions.

competitors must:
- **Navigate** through the terrain by passing through a series of **waypoints** (control points), optionally including **split-time checkpoints**, **neutralized zones** (speed-limited or paused areas), and **hidden waypoints** revealed only during the stage,
- Rely primarily on a **roadbook** and **compass** for navigation, as the use of **GPS** is restricted or prohibited except for authorized emergency systems,
- **Minimize total time** or **minimize total distance traveled**, depending on the rally’s specific competition rules,
- Respect specific **categories** (e.g., 4x4, moto, quad, truck), and potentially **subcategories** (e.g., production vs modified vehicles),
- Manage **penalties** for missing waypoints, speeding in restricted zones, mechanical assistance outside allowed areas, failure to respect navigation rules, or abandoning a stage,
- Handle special race circumstances such as **abandonments**, **mechanical breakdowns**, and **refueling stops** if applicable.

Each competitor or team:
- May consist of a **single competitor** or a **team** (driver, co-pilot, mechanic depending on vehicle type),
- Starts each stage either individually (with a **staggered start time**) or grouped depending on race organization,
- Must complete each stage following the mandatory route and checkpoint strategy defined in the official roadbook.

Vehicles are also required to be equipped with **official tracking systems** and **emergency alert devices** to ensure competitor safety. These systems allow the organization to monitor vehicle positions and provide rapid assistance in case of accidents or critical incidents.

Victory is based on the **cumulative performance** across all stages, after applying penalties, and is determined separately for each **category** or **subcategory**.  
Final classifications may take into account **stage times**, **distances**, **penalty adjustments**, and competitor **retirements**.

## First Version

### Core Concepts
- **Roadbook File**: A **KML file** uploaded by admins that defines the stage layout: waypoints, neutral zones, refueling points.
- **competitor Track File**: A **KML file** uploaded by the competitor after completing the stage, showing their route.
- **Waypoint**: A specific coordinate that competitors must pass through.
- **Zone**: Areas like Neutralized Zone or Refueling Zone extracted from the KML.

---

### Initial Processes
- **Roadbook Upload**: Admin uploads the official stage KML (parsed and saved internally).
- **competitor Track Upload**: competitor uploads their recorded KML after the stage.
- **Waypoint Matching**: The system analyzes the competitor’s KML to check:
  - Which waypoints were successfully passed,
  - Which waypoints were missed,
  - (Optional later) Whether speed rules were respected inside zones.

---

### Success Criteria for Stage
- competitor **passed mandatory waypoints**.
- Penalties are applied for **missed waypoints**.

---

# 📦 Minimum Aggregates and Entities for Starting

| Aggregate             | Root Entity         | Contains / Manages                      |
|-----------------------|----------------------|-----------------------------------------|
| Roadbook Management   | Stage Roadbook       | List of official Waypoints, Zones       |
| competitor Tracking  | competitor Stage Track | List of passed Waypoints, Raw Route Data |
| Waypoint (Entity)      | Waypoint             | Coordinates, Radius of validation       |

---

### Initial Application Flow (First Version)

1. Admin uploads a **Stage Roadbook KML**:
   - System parses and extracts:
     - Waypoints (each with coordinate and "acceptance radius" maybe 50-100 meters),
     - Zones (optional, later).
2. competitors complete the stage with their tracking devices.
3. After the stage:
   - competitor uploads their **Track KML**.
4. System parses competitor’s KML:
   - Compare competitor route points with expected waypoints.
   - Mark each waypoint as **passed** or **missed**.
5. Store matching results for each competitor:
   - Passed Waypoints,
   - Missed Waypoints,
   - Potential penalty calculation.

It would help you build the first module fast and clean! 🚀

### Initial API definition

```
POST /auth/login
POST /auth/register


POST /users
GET /users
GET /users/{userId}
PATCH /users/{userId}
DELETE /users/{userId}

POST /rallies/{rallyId}/competitors/
GET /rallies/{rallyId}/competitors/
GET /rallies/{rallyId}/competitors/{competitorId}
PATCH /rallies/{rallyId}/competitors/{competitorId}

POST /rallies
GET /rallies
GET /rallies/{rallyId}
PATCH /rallies/{rallyId}
DELETE /rallies/{rallyId}

POST /rallies/{rallyId}/stages
GET /rallies/{rallyId}/stages
GET /rallies/{rallyId}/stages/{stageId}
PATCH /rallies/{rallyId}/stages/{stageId}
DELETE /rallies/{rallyId}/stages/{stageId}

POST /rallies/{rallyId}/stages/{stageId}/roadbook
GET /rallies/{rallyId}/stages/{stageId}/roadbook

POST /rallies/{rallyId}/competitors/{competitorId}/stages/{stageId}/track
PUT /rallies/{rallyId}/competitors/{competitorId}/stages/{stageId}/track
GET /rallies/{rallyId}/competitors/{competitorId}/stages/{stageId}/track

GET /rallies/{rallyId}/competitors/{competitorId}/stages/{stageId}/waypoints

GET /rallies/{rallyId}/competitors/{competitorId}/stages/{stageId}/score
GET /rallies/{rallyId}/competitors/{competitorId}/score

GET /rallies/{rallyId}/classification
GET /rallies/{rallyId}/stages/{stageId}/classification
```
