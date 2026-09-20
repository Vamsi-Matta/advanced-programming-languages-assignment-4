import random
from collections import defaultdict

DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
SHIFTS = ["Morning", "Afternoon", "Evening"]
MIN_PER_SHIFT = 2
MAX_PER_SHIFT = 2  # A shift is considered full at two employees for this small application.
MAX_DAYS = 5
RANDOM_SEED = 42

DEMO_EMPLOYEES = ["Aarav", "Maya", "Noah", "Priya", "Liam", "Sofia", "Ethan", "Anika", "Daniel"]


def rotate(items, n):
    n %= len(items)
    return items[n:] + items[:n]


def demo_preferences(names):
    # Demo intentionally creates competing first choices so conflict handling is visible.
    preferences = {}
    for e_idx, name in enumerate(names):
        preferences[name] = {}
        for d_idx, day in enumerate(DAYS):
            if (e_idx + d_idx) % 2 == 0:
                preferences[name][day] = ["Morning", "Afternoon", "Evening"]
            else:
                preferences[name][day] = ["Morning", "Evening", "Afternoon"]
    return preferences


def collect_input():
    print("Employee Schedule Manager")
    print("Enter D for demo data or I for interactive input.")
    mode = input("Mode [D/I]: ").strip().upper() or "D"
    if mode != "I":
        names = DEMO_EMPLOYEES[:]
        return names, demo_preferences(names)

    count = int(input("Number of employees (minimum 9 recommended): "))
    names = []
    preferences = {}
    for i in range(count):
        name = input(f"Employee {i + 1} name: ").strip()
        names.append(name)
        preferences[name] = {}
        for day in DAYS:
            while True:
                raw = input(f"{name} - {day} ranking (Morning,Afternoon,Evening): ")
                ranking = [x.strip().title() for x in raw.split(",")]
                if len(ranking) == 3 and set(ranking) == set(SHIFTS):
                    preferences[name][day] = ranking
                    break
                print("Please enter each shift exactly once, separated by commas.")
    return names, preferences


def build_schedule(names, preferences):
    rng = random.Random(RANDOM_SEED)
    schedule = {day: {shift: [] for shift in SHIFTS} for day in DAYS}
    days_worked = defaultdict(int)
    conflicts = []

    for day_index, day in enumerate(DAYS):
        assigned_today = set()

        # First satisfy each shift with employees who ranked it highest.
        for shift in SHIFTS:
            preferred = [
                name for name in names
                if name not in assigned_today
                and days_worked[name] < MAX_DAYS
                and preferences[name][day][0] == shift
            ]
            preferred.sort(key=lambda n: (days_worked[n], names.index(n)))
            for name in preferred:
                if len(schedule[day][shift]) >= MIN_PER_SHIFT:
                    break
                schedule[day][shift].append(name)
                assigned_today.add(name)
                days_worked[name] += 1

        # Fill shortages. Candidate selection is random among the least-used eligible employees.
        for shift in SHIFTS:
            while len(schedule[day][shift]) < MIN_PER_SHIFT:
                eligible = [
                    name for name in names
                    if name not in assigned_today and days_worked[name] < MAX_DAYS
                ]
                if not eligible:
                    raise RuntimeError(
                        f"Unable to staff {day} {shift}. Add more employees or increase weekly availability."
                    )
                lowest_count = min(days_worked[name] for name in eligible)
                least_used = [name for name in eligible if days_worked[name] == lowest_count]

                # Prefer employees who rank this shift higher; randomize only among equal ranks.
                best_rank = min(preferences[name][day].index(shift) for name in least_used)
                candidates = [
                    name for name in least_used
                    if preferences[name][day].index(shift) == best_rank
                ]
                name = rng.choice(candidates)
                primary = preferences[name][day][0]
                if primary != shift:
                    conflicts.append(
                        f"{day}: {name}'s preferred {primary} shift was unavailable/full; assigned {shift}."
                    )
                schedule[day][shift].append(name)
                assigned_today.add(name)
                days_worked[name] += 1

    return schedule, days_worked, conflicts


def validate(schedule, days_worked):
    for day in DAYS:
        seen = set()
        for shift in SHIFTS:
            workers = schedule[day][shift]
            assert len(workers) >= MIN_PER_SHIFT
            assert len(workers) <= MAX_PER_SHIFT
            for name in workers:
                assert name not in seen, f"{name} assigned more than once on {day}"
                seen.add(name)
    assert all(count <= MAX_DAYS for count in days_worked.values())


def print_schedule(schedule, days_worked, conflicts):
    print("\nFINAL WEEKLY EMPLOYEE SCHEDULE")
    print("=" * 72)
    for day in DAYS:
        print(f"\n{day}")
        print("-" * 72)
        for shift in SHIFTS:
            print(f"{shift:<10}: {', '.join(schedule[day][shift])}")
    print("\nDAYS WORKED")
    print("-" * 72)
    for name in sorted(days_worked):
        print(f"{name:<10}: {days_worked[name]}")
    print(f"\nConflicts resolved: {len(conflicts)}")
    for item in conflicts[:8]:
        print("- " + item)
    if len(conflicts) > 8:
        print(f"- ... and {len(conflicts) - 8} more")
    print("\nValidation passed: one shift/day, max 5 days/week, and 2 employees/shift.")


def main():
    names, preferences = collect_input()
    schedule, days_worked, conflicts = build_schedule(names, preferences)
    validate(schedule, days_worked)
    print_schedule(schedule, days_worked, conflicts)


if __name__ == "__main__":
    main()
