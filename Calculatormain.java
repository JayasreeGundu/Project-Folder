import java.util.*;

public class SmartCalculator4 {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        boolean continueCalc = true;

        while (continueCalc) {
            System.out.print("Choose mode (array / linkedlist / queue): ");
            String mode = input.next().toLowerCase();

            if (mode.equals("array")) {
                runCalculatorWithArray(input);
            } else if (mode.equals("linkedlist")) {
                runCalculatorWithLinkedList(input);
            } else if (mode.equals("queue")) {
                runCalculatorWithQueue(input);
            } else {
                System.out.println("Invalid mode. Try again.");
                continue;
            }

            System.out.print("Do you want to perform another calculation? (yes/no): ");
            String choice = input.next();
            if (choice.equalsIgnoreCase("no")) {
                continueCalc = false;
                System.out.println("Calculator exited.");
            }
        }
        input.close();
    }

    private static void runCalculatorWithArray(Scanner input) {
        List<String> tokens = new ArrayList<>();
        System.out.print("Enter full expression (e.g., 5+(6-3)*2): ");
        input.nextLine(); // Consume leftover newline
        String line = input.nextLine();
        tokens = tokenize(line);
        performCalculation(tokens);
    }

    private static void runCalculatorWithLinkedList(Scanner input) {
        List<String> tokens = new LinkedList<>();
        System.out.print("Enter full expression (e.g., 5+(6-3)*2): ");
        input.nextLine(); // Consume leftover newline
        String line = input.nextLine();
        tokens = tokenize(line);
        performCalculation(tokens);
    }

    private static void runCalculatorWithQueue(Scanner input) {
        Queue<String> tokensQueue = new LinkedList<>();
        List<String> tokensList = new ArrayList<>();

        System.out.println("Queue Mode Activated.");
        System.out.print("Enter numbers separated by commas (e.g., 2,5,6,8,3,2): ");
        input.nextLine(); // Consume leftover newline
        String numberLine = input.nextLine();
        String[] numbers = numberLine.split(",");

        for (String num : numbers) {
            num = num.trim();
            if (!num.isEmpty()) {
                tokensQueue.offer(num);
                tokensList.add(num);
            }
        }

        int totalInputs = tokensQueue.size();

        System.out.print("Enter rotation size: ");
        int rotationSize = input.nextInt();

        int rotations = totalInputs / rotationSize;
        System.out.println("Total inputs: " + totalInputs);
        System.out.println("Rotation size: " + rotationSize);
        System.out.println("Rotations to perform: " + rotations);

        for (int i = 0; i < rotations; i++) {
            String rotated = tokensQueue.poll();
            if (rotated != null) {
                tokensQueue.offer(rotated);
            }
        }

        System.out.println("Final queue after rotation: " + tokensQueue);

        tokensList = new ArrayList<>(tokensQueue);

        System.out.print("Do you want to perform arithmetic operations on these values? (yes/no): ");
        input.nextLine(); // consume leftover newline
        String choice = input.nextLine();
        if (choice.equalsIgnoreCase("yes")) {
            performCalculation(tokensList);
        }
    }

    private static void performCalculation(List<String> tokens) {
        String expression = String.join(" ", tokens);
        try {
            double result = evaluateExpression(expression);
            System.out.println("Expression: " + expression + " = " + result);

            List<Double> numbers = extractNumbers(tokens);
            Collections.sort(numbers);
            System.out.println("Sorted numbers: " + numbers);

            Set<Double> unique = new LinkedHashSet<>(numbers);
            System.out.println("Sorted unique numbers: " + unique);

            List<Double> even = new ArrayList<>();
            List<Double> odd = new ArrayList<>();
            for (double n : unique) {
                if (((int) n) % 2 == 0) even.add(n);
                else odd.add(n);
            }
            System.out.println("Even values: " + even);
            System.out.println("Odd values: " + odd);
        } catch (Exception e) {
            System.out.println("Invalid expression: " + e.getMessage());
        }
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        for (char ch : expression.toCharArray()) {
            if (Character.isDigit(ch) || ch == '.') {
                token.append(ch);
            } else if ("+-*/()".indexOf(ch) != -1) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
                tokens.add(String.valueOf(ch));
            } else if (ch == ' ') {
                continue;
            } else {
                throw new IllegalArgumentException("Invalid character in expression: " + ch);
            }
        }
        if (token.length() > 0) {
            tokens.add(token.toString());
        }
        return tokens;
    }

    private static List<Double> extractNumbers(List<String> tokens) {
        List<Double> numbers = new ArrayList<>();
        for (String token : tokens) {
            try {
                numbers.add(Double.parseDouble(token));
            } catch (NumberFormatException ignored) {
            }
        }
        return numbers;
    }

    private static double evaluateExpression(String expr) {
        return evaluateRPN(toRPN(expr));
    }

    private static List<String> toRPN(String expr) {
        List<String> tokens = Arrays.asList(expr.split(" "));
        Stack<String> ops = new Stack<>();
        List<String> output = new ArrayList<>();
        Map<String, Integer> precedence = new HashMap<>();
        precedence.put("+", 1);
        precedence.put("-", 1);
        precedence.put("*", 2);
        precedence.put("/", 2);
        for (String token : tokens) {
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                output.add(token);
            } else if ("+-*/".contains(token)) {
                while (!ops.isEmpty() && precedence.containsKey(ops.peek()) &&
                        precedence.get(ops.peek()) >= precedence.get(token)) {
                    output.add(ops.pop());
                }
                ops.push(token);
            } else if (token.equals("(")) {
                ops.push(token);
            } else if (token.equals(")")) {
                while (!ops.peek().equals("(")) {
                    output.add(ops.pop());
                }
                ops.pop();
            }
        }
        while (!ops.isEmpty()) {
            output.add(ops.pop());
        }
        return output;
    }

    private static double evaluateRPN(List<String> rpn) {
        Stack<Double> stack = new Stack<>();
        for (String token : rpn) {
            if (token.matches("-?\\d+(\\.\\d+)?")) {
                stack.push(Double.parseDouble(token));
            } else {
                double b = stack.pop();
                double a = stack.pop();
                switch (token) {
                    case "+" -> stack.push(a + b);
                    case "-" -> stack.push(a - b);
                    case "*" -> stack.push(a * b);
                    case "/" -> {
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        stack.push(a / b);
                    }
                }
            }
        }
        return stack.pop();
    }
}
