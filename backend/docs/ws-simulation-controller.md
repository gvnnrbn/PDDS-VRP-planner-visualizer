# SimulationController WebSocket Endpoints

This document describes the real-time WebSocket (STOMP) endpoints exposed by the backend `SimulationController` for simulation management. All endpoints are under the `/app/` prefix and responses are broadcast to `/topic/simulation` as `SimulationResponse` objects.

---

## Endpoints

### 1. `/app/init`

**Purpose:**
Start a new simulation.

**Payload:**
```json
{
  "initialTime": {
    "year": int,
    "month": int,
    "day": int,
    "hour": int,
    "min": int
  }
}
```
- `initialTime`: The simulation's start time (see DTOs below).

**DTOs:**
- `InitMessage`
  - `Time initialTime`
- `Time`
  - `int year`
  - `int month`
  - `int day`
  - `int hour`
  - `int min`

**Behavior:**
- Initializes the simulation state (vehicles, orders, blockages, warehouses, failures, maintenances, current time).
- Starts the simulation thread.

**Emits:**
- On success:
  ```json
  {
    "type": "SIMULATION_STARTED",
    "data": "Simulation initialized"
  }
  ```
- On error:
  ```json
  {
    "type": "SIMULATION_ERROR",
    "data": "Error starting simulation: <error message>"
  }
  ```

---

### 2. `/app/update-failures`

**Purpose:**
Inject or update a failure event for a vehicle.

**Payload:**
```json
{
  "type": "Ti1" | "Ti2" | "Ti3",
  "vehiclePlaque": "string",
  "shiftOccurredOn": "T1" | "T2" | "T3"
}
```
- `type`: Enum, one of `"Ti1"`, `"Ti2"`, `"Ti3"` (see Enums below).
- `vehiclePlaque`: String, the vehicle's unique identifier.
- `shiftOccurredOn`: Enum, one of `"T1"` (00:00-08:00), `"T2"` (08:00-16:00), `"T3"` (16:00-24:00).

**DTOs:**
- `UpdateFailuresMessage`
  - `FailureType type` (`Ti1`, `Ti2`, `Ti3`)
  - `String vehiclePlaque`
  - `Shift shiftOccurredOn` (`T1`, `T2`, `T3`)

**Behavior:**
- Updates the simulation with the new failure.
- If simulation is not running, emits an error.

**Emits:**
- On success:
  ```json
  {
    "type": "STATE_UPDATED",
    "data": "State updated successfully"
  }
  ```
- On error:
  ```json
  {
    "type": "ERROR",
    "data": "Failed to update state: <error message>"
  }
  ```
  or
  ```json
  {
    "type": "ERROR",
    "data": "No active simulation"
  }
  ```

---

### 3. `/app/stop`

**Purpose:**
Stop the currently running simulation.

**Payload:**
None (empty message).

**Behavior:**
- Stops the simulation thread and cleans up state.

**Emits:**
- Always:
  ```json
  {
    "type": "SIMULATION_STOPPED",
    "data": "Simulation stopped"
  }
  ```

---

## DTO ENUMS

### `FailureType` (in `PlannerFailure`)
- `Ti1` (120 min stuck)
- `Ti2` (120 min stuck)
- `Ti3` (240 min stuck)

### `Shift` (in `PlannerFailure`)
- `T1` (00:00-08:00)
- `T2` (08:00-16:00)
- `T3` (16:00-24:00)

---

## Response Format

All responses are sent to `/topic/simulation` as a `SimulationResponse`:
```json
{
  "type": "string", // e.g., "SIMULATION_STARTED", "ERROR", etc.
  "data": "string"  // message or details
}
```

---

## Example Usage

**Start simulation:**
```js
stompClient.send("/app/init", {}, JSON.stringify({
  initialTime: { year: 2025, month: 1, day: 1, hour: 8, min: 0 }
}));
```

**Update failure:**
```js
stompClient.send("/app/update-failures", {}, JSON.stringify({
  type: "Ti1",
  vehiclePlaque: "TA01",
  shiftOccurredOn: "T2"
}));
```

**Stop simulation:**
```js
stompClient.send("/app/stop", {}, {});
```

**Subscribe to responses:**
```js
stompClient.subscribe("/topic/simulation", (msg) => {
  const response = JSON.parse(msg.body);
  // handle response.type and response.data
});
```