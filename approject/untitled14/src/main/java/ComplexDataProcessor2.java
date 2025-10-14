
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComplexDataProcessor2 {

/**
 * Processes a list of data items based on the processing mode, threshold, and optional flags.
 * This method demonstrates complex control flow with multiple branches and loops, making manual
 * test data generation for edge-pair coverage challenging.
 *
 * @param data           The list of data items (each item should be a Map with a "value" key).
 * @param processingMode A string indicating the processing mode ("alpha", "beta", or "gamma").
 * @param threshold      An integer threshold value.
 * @param options        A map of optional flags to influence processing.
 * @return A list of processed results (strings).
 */


        public String processData(List<Map<String, Object>> data, String processingMode, int threshold, Map<String, Boolean> options) {
            StringBuilder output = new StringBuilder();
            output.append("P1\n");
            List<String> results = new ArrayList<>();


            if (data == null || data.isEmpty()) {
                output.append("P2");
                results.add("Warning: Input data is null or empty.");
                return output.toString();
            }
            output.append("P3\n");
            if (processingMode == null || processingMode.isEmpty()) {
                output.append("P4");
                results.add("Error: Processing mode cannot be null or empty.");
                return output.toString();
            }
            output.append("P5\n");
            int i = 0;

            while (i < data.size()) {
                output.append("P6\n");
                Map<String, Object> item = data.get(i);
                if (item == null || !item.containsKey("value")) {
                    output.append("P7\n");
                    results.add("Warning: Skipping invalid data item.");
                    i++;
                    continue;
                }
                output.append("P8\n");
                Object valueObj = item.get("value");
                if (!(valueObj instanceof Integer)) {
                    output.append("P9\n");
                    results.add("Warning: Skipping item with non-integer value.");
                    i++;
                    continue;
                }
                output.append("P10\n");
                int value = (Integer) valueObj;

                switch (processingMode) {
                    case "alpha":
                        output.append("P11\n");
                        if (value > threshold) {
                            output.append("P12\n");
                            if (options != null && options.getOrDefault("doubleHighAlpha", false)) {
                                output.append("P13\n");
                                results.add("Alpha: High value doubled: " + (value * 2));
                            } else {
                                output.append("P14\n");
                                results.add("Alpha: High value: " + value);
                            }
                        }
                        else if (value < threshold) {
                            output.append("P15\n");
                            if (options != null && options.containsKey("addLowAlpha")) {
                                output.append("P16\n");
                                results.add("Alpha: Low value adjusted: " + (value + (options.get("addLowAlpha") ? 10 : 5)));
                            } else {
                                output.append("P17\n");
                                results.add("Alpha: Low value: " + value);
                            }
                        } else {
                            output.append("P18\n");
                            results.add("Alpha: Threshold value: " + value);
                        }
                        output.append("P19\n");
                        break;

                    case "beta":
                        output.append("P20\n");
                        if (item.containsKey("category")) {
                            output.append("P21\n");
                            String category = (String) item.get("category");
                            if ("special".equalsIgnoreCase(category)) {
                                output.append("P22\n");
                                if (options != null && options.getOrDefault("negateSpecialBeta", false)) {
                                    output.append("P23\n");
                                    results.add("Beta: Special category, negated value: " + (-value));
                                } else {
                                    output.append("P24\n");
                                    results.add("Beta: Special category: " + value);
                                }
                            } else if ("normal".equalsIgnoreCase(category)) {
                                output.append("P25\n");
                                results.add("Beta: Normal category: " + value);
                            } else {
                                output.append("P26\n");
                                results.add("Beta: Unknown category.");
                            }
                        } else {
                            output.append("P27\n");
                            results.add("Beta: No category provided.");
                        }
                        output.append("P28\n");
                        break;

                    case "gamma":
                        output.append("P29\n");
                        int j = 0;
                        while (j < 3) {
                            output.append("P30\n");
                            if (value > threshold + j * 5) {
                                output.append("P31\n");
                                if (options != null && options.getOrDefault("flagGamma", false)) {
                                    output.append("P32\n");
                                    results.add("Gamma: Loop " + (j + 1) + ", High value with flag.");
                                } else {
                                    output.append("P33\n");
                                    results.add("Gamma: Loop " + (j + 1) + ", High value.");
                                }
                            } else if (value < threshold - j * 5) {
                                output.append("P34\n");
                                results.add("Gamma: Loop " + (j + 1) + ", Low value.");
                            } else {
                                output.append("P35\n");
                                results.add("Gamma: Loop " + (j + 1) + ", Within range.");
                                break; // Exit inner loop
                            }
                            output.append("P36\n");
                            j++;
                        }
                        output.append("P37\n");
                        break;

                    default:
                        output.append("P38\n");
                        results.add("Error: Invalid processing mode encountered.");
                }
                output.append("P39\n");
                if (options != null && options.getOrDefault("logAll", false)) {
                    output.append("P40\n");
                    results.add("Logged: " + item.toString());
                }
                output.append("P41\n");
                i++;
            }
            output.append("P42");
            return output.toString();
        }
    }
