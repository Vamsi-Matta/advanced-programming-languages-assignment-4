import java.util.*;

public class ScheduleManager {
    static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
    static final List<String> SHIFTS = List.of("Morning", "Afternoon", "Evening");
    static final int MIN_PER_SHIFT = 2;
    static final int MAX_PER_SHIFT = 2;
    static final int MAX_DAYS = 5;
    static final long RANDOM_SEED = 42L;
    static final List<String> DEMO_EMPLOYEES = List.of("Aarav", "Maya", "Noah", "Priya", "Liam", "Sofia", "Ethan", "Anika", "Daniel");

    static class InputData {
        List<String> names;
        Map<String, Map<String, List<String>>> preferences;
        InputData(List<String> names, Map<String, Map<String, List<String>>> preferences) {
            this.names = names;
            this.preferences = preferences;
        }
    }

    static List<String> rotate(List<String> items, int n) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) result.add(items.get((i + n) % items.size()));
        return result;
    }

    static Map<String, Map<String, List<String>>> demoPreferences(List<String> names) {
        // Demo intentionally creates competing first choices so conflict handling is visible.
        Map<String, Map<String, List<String>>> preferences = new LinkedHashMap<>();
        for (int e = 0; e < names.size(); e++) {
            Map<String, List<String>> perDay = new LinkedHashMap<>();
            for (int d = 0; d < DAYS.size(); d++) {
                if ((e + d) % 2 == 0) perDay.put(DAYS.get(d), List.of("Morning", "Afternoon", "Evening"));
                else perDay.put(DAYS.get(d), List.of("Morning", "Evening", "Afternoon"));
            }
            preferences.put(names.get(e), perDay);
        }
        return preferences;
    }


    static InputData collectInput(Scanner scanner) {
        System.out.println("Employee Schedule Manager");
        System.out.println("Enter D for demo data or I for interactive input.");
        System.out.print("Mode [D/I]: ");
        String mode = scanner.nextLine().trim().toUpperCase();
        if (!mode.equals("I")) {
            List<String> names = new ArrayList<>(DEMO_EMPLOYEES);
            return new InputData(names, demoPreferences(names));
        }

        System.out.print("Number of employees (minimum 9 recommended): ");
        int count = Integer.parseInt(scanner.nextLine().trim());
        List<String> names = new ArrayList<>();
        Map<String, Map<String, List<String>>> preferences = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            System.out.print("Employee " + (i + 1) + " name: ");
            String name = scanner.nextLine().trim();
            names.add(name);
            Map<String, List<String>> perDay = new LinkedHashMap<>();
            for (String day : DAYS) {
                while (true) {
                    System.out.print(name + " - " + day + " ranking (Morning,Afternoon,Evening): ");
                    String[] parts = scanner.nextLine().split(",");
                    List<String> ranking = new ArrayList<>();
                    for (String part : parts) {
                        String s = part.trim().toLowerCase();
                        ranking.add(s.substring(0, 1).toUpperCase() + s.substring(1));
                    }
                    if (ranking.size() == 3 && new HashSet<>(ranking).equals(new HashSet<>(SHIFTS))) {
                        perDay.put(day, ranking);
                        break;
                    }
                    System.out.println("Please enter each shift exactly once, separated by commas.");
                }
            }
            preferences.put(name, perDay);
        }
        return new InputData(names, preferences);
    }

    static class Result {
        Map<String, Map<String, List<String>>> schedule = new LinkedHashMap<>();
        Map<String, Integer> daysWorked = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();
    }

    static Result buildSchedule(List<String> names, Map<String, Map<String, List<String>>> preferences) {
        Random random = new Random(RANDOM_SEED);
        Result result = new Result();
        for (String name : names) result.daysWorked.put(name, 0);
        for (String day : DAYS) {
            Map<String, List<String>> daySchedule = new LinkedHashMap<>();
            for (String shift : SHIFTS) daySchedule.put(shift, new ArrayList<>());
            result.schedule.put(day, daySchedule);
            Set<String> assignedToday = new HashSet<>();

            for (String shift : SHIFTS) {
                List<String> preferred = new ArrayList<>();
                for (String name : names) {
                    if (!assignedToday.contains(name)
                            && result.daysWorked.get(name) < MAX_DAYS
                            && preferences.get(name).get(day).get(0).equals(shift)) preferred.add(name);
                }
                preferred.sort(Comparator.comparingInt((String n) -> result.daysWorked.get(n)).thenComparingInt(names::indexOf));
                for (String name : preferred) {
                    if (daySchedule.get(shift).size() >= MIN_PER_SHIFT) break;
                    daySchedule.get(shift).add(name);
                    assignedToday.add(name);
                    result.daysWorked.put(name, result.daysWorked.get(name) + 1);
                }
            }

            for (String shift : SHIFTS) {
                while (daySchedule.get(shift).size() < MIN_PER_SHIFT) {
                    List<String> eligible = new ArrayList<>();
                    for (String name : names) {
                        if (!assignedToday.contains(name) && result.daysWorked.get(name) < MAX_DAYS) eligible.add(name);
                    }
                    if (eligible.isEmpty()) throw new IllegalStateException("Unable to staff " + day + " " + shift + ". Add more employees.");
                    int lowest = eligible.stream().mapToInt(result.daysWorked::get).min().orElseThrow();
                    List<String> leastUsed = new ArrayList<>();
                    for (String name : eligible) if (result.daysWorked.get(name) == lowest) leastUsed.add(name);
                    int bestRank = leastUsed.stream().mapToInt(n -> preferences.get(n).get(day).indexOf(shift)).min().orElseThrow();
                    List<String> candidates = new ArrayList<>();
                    for (String name : leastUsed) if (preferences.get(name).get(day).indexOf(shift) == bestRank) candidates.add(name);
                    String name = candidates.get(random.nextInt(candidates.size()));
                    String primary = preferences.get(name).get(day).get(0);
                    if (!primary.equals(shift)) result.conflicts.add(day + ": " + name + "'s preferred " + primary + " shift was unavailable/full; assigned " + shift + ".");
                    daySchedule.get(shift).add(name);
                    assignedToday.add(name);
                    result.daysWorked.put(name, result.daysWorked.get(name) + 1);
                }
            }
        }
        return result;
    }

    static void validate(Result result) {
        for (String day : DAYS) {
            Set<String> seen = new HashSet<>();
            for (String shift : SHIFTS) {
                List<String> workers = result.schedule.get(day).get(shift);
                if (workers.size() < MIN_PER_SHIFT || workers.size() > MAX_PER_SHIFT) throw new IllegalStateException("Invalid staffing level");
                for (String name : workers) if (!seen.add(name)) throw new IllegalStateException(name + " assigned more than once on " + day);
            }
        }
        for (int count : result.daysWorked.values()) if (count > MAX_DAYS) throw new IllegalStateException("Employee exceeds five days");
    }

    static void printSchedule(Result result) {
        System.out.println("\nFINAL WEEKLY EMPLOYEE SCHEDULE");
        System.out.println("========================================================================");
        for (String day : DAYS) {
            System.out.println("\n" + day);
            System.out.println("------------------------------------------------------------------------");
            for (String shift : SHIFTS) System.out.printf("%-10s: %s%n", shift, String.join(", ", result.schedule.get(day).get(shift)));
        }
        System.out.println("\nDAYS WORKED");
        System.out.println("------------------------------------------------------------------------");
        result.daysWorked.keySet().stream().sorted().forEach(name -> System.out.printf("%-10s: %d%n", name, result.daysWorked.get(name)));
        System.out.println("\nConflicts resolved: " + result.conflicts.size());
        for (int i = 0; i < Math.min(8, result.conflicts.size()); i++) System.out.println("- " + result.conflicts.get(i));
        if (result.conflicts.size() > 8) System.out.println("- ... and " + (result.conflicts.size() - 8) + " more");
        System.out.println("\nValidation passed: one shift/day, max 5 days/week, and 2 employees/shift.");
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        InputData input = collectInput(scanner);
        Result result = buildSchedule(input.names, input.preferences);
        validate(result);
        printSchedule(result);
    }
}
