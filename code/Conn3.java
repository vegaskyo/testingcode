import java.io.*;  
import java.util.regex.*;  
import com.google.gson.*;  
  
public class FailedConnectionExtractor {  
    public static void main(String[] args) {  
        String inputFilePath = "input.txt";    // Replace with your input file path  
        String outputFilePath = "output.txt";  // Replace with your desired output file path  
  
        try {  
            // Read the entire file content  
            String content = new String(Files.readAllBytes(Paths.get(inputFilePath)));  
  
            // Split the content into lines  
            String[] lines = content.split("\n");  
  
            StringBuilder results = new StringBuilder();  
            Gson gson = new Gson();  
  
            for (String line : lines) {  
                if (line.startsWith("failed:")) {  
                    // Extract connectionName  
                    String connectionName = extractBetween(line, "\\[", "\\]");  
                    if (connectionName == null) continue;  
  
                    // Extract JSON string  
                    int jsonStartIndex = line.indexOf("=>");  
                    if (jsonStartIndex == -1) continue;  
                    String jsonString = line.substring(jsonStartIndex + 2).trim();  
  
                    // Parse the JSON  
                    JsonObject rootObject = JsonParser.parseString(jsonString).getAsJsonObject();  
                    String itemValue = rootObject.get("item").getAsString();  
  
                    // Extract port from cmd  
                    String cmd = rootObject.get("cmd").getAsString();  
                    String port = extractBetween(cmd, "-p ", "\n");  
                    if (port == null) continue;  
  
                    // Append result  
                    results.append(connectionName).append(":").append(itemValue).append(":").append(port).append("\n");  
                }  
            }  
  
            // Write the results to the output file  
            Files.write(Paths.get(outputFilePath), results.toString().getBytes());  
  
            System.out.println("Processing complete. Results written to " + outputFilePath);  
  
        } catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
  
    // Method to extract text between two patterns using regular expressions  
    private static String extractBetween(String text, String startPattern, String endPattern) {  
        Pattern pattern = Pattern.compile(Pattern.quote(startPattern) + "(.*?)" + Pattern.quote(endPattern));  
        Matcher matcher = pattern.matcher(text);  
        if (matcher.find()) {  
            return matcher.group(1).trim();  
        }  
        return null;  
    }  
}  