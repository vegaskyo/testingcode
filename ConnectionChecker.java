package dev.zhun.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConnectionChecker {
    public static void main(String[] args) {
        String inputFilePath = "text.txt";  // Replace with your input file path
        String outputFilePath = "output.txt";  // Replace with your desired output file path

        try {
            // Read the entire file content into a string
            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
            reader.close();

            String content = contentBuilder.toString();

            // Split the content into blocks based on "ok:"
            String[] blocks = content.split("(?m)^ok:");

            ArrayList<String> results = new ArrayList<>();

            ObjectMapper mapper = new ObjectMapper();

            for (String block : blocks) {
                if (block.trim().isEmpty()) continue;  // Skip empty blocks

                // Extract connectionName
                String connectionName = extractConnectionName(block);

                // Extract JSON string
                String jsonString = extractJsonString(block);

                if (jsonString == null) continue;  // Skip if JSON not found

                // Parse the JSON
                JsonNode rootNode = mapper.readTree(jsonString);
                JsonNode msgNode = rootNode.get("msg");

                if (msgNode != null && msgNode.isArray()) {
                    for (JsonNode itemNode : msgNode) {
                        String itemValue = itemNode.get("item").asText();

                        JsonNode stdoutLinesNode = itemNode.get("stdout_lines");
                        if (stdoutLinesNode != null && stdoutLinesNode.isArray()) {
                            int size = stdoutLinesNode.size();
                            if (size > 0) {
                                String lastLine = stdoutLinesNode.get(size - 1).asText();
                                if (lastLine.contains("* * *")) {
                                    results.add(connectionName + ":" + itemValue);
                                }
                            }
                        }
                    }
                }
            }

            // Write the results to the output file
            FileWriter writer = new FileWriter(outputFilePath);
            for (String result : results) {
                writer.write(result + "\n");
            }
            writer.close();

            System.out.println("Processing complete. Results written to " + outputFilePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to extract connectionName from the block
    private static String extractConnectionName(String block) {
        String connectionName = null;
        int startIndex = block.indexOf('[');
        int endIndex = block.indexOf(']');
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            connectionName = block.substring(startIndex + 1, endIndex).trim();
        }
        return connectionName;
    }

    // Method to extract JSON string from the block
    private static String extractJsonString(String block) {
        int jsonStartIndex = block.indexOf("=>");
        if (jsonStartIndex != -1) {
            String jsonString = block.substring(jsonStartIndex + 2).trim();
            return jsonString;
        }
        return null;
    }
}
