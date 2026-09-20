# Advanced Programming Languages - Assignment 4: Implementing Control Structures

This repository contains the same employee scheduling application implemented in two contrasting high-level languages: Python and Java.

## Features
- Employee names and ranked shift preferences for all seven days
- Morning, Afternoon, and Evening shifts
- Maximum one shift per employee per day
- Maximum five workdays per employee per week
- Minimum two employees per shift per day
- Randomized fallback assignment among eligible, least-used employees when preferred staffing is insufficient
- Conflict detection and reassignment to another ranked shift on the same day
- Bonus: ranked shift preferences
- Validation of all core scheduling constraints

## Run Python
```bash
cd src/python
python schedule_manager.py
```
Choose `D` for the reproducible demo or `I` for interactive employee input.

## Run Java
```bash
cd src/java
javac ScheduleManager.java
java ScheduleManager
```
Choose `D` for the reproducible demo or `I` for interactive employee input.

## Repository Structure
- `src/python/schedule_manager.py` - Python implementation
- `src/java/ScheduleManager.java` - Java implementation

The demo uses a fixed random seed so the example can be reproduced while still exercising randomized fallback selection.