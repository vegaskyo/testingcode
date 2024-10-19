package org.apache.hop.ui.hopgui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test2 {

    public static void main(String[] args) throws IOException {
        Path hopConfigFile = Paths.get("D:\\dev\\tools\\apache-tomcat-9.0.95\\webapps\\hop\\config\\hop-config.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode hopConfig = mapper.readTree(Files.newBufferedReader(hopConfigFile));

        String projectName = "longvh";
        String workflowFileName = "parent.hwf";

        // Get project home
        String projectHome = null;
        ArrayNode projectConfigurations = (ArrayNode) hopConfig.get("projectsConfig").get("projectConfigurations");
        for (JsonNode projectConfig : projectConfigurations) {
            if (projectConfig.get("projectName").asText().equals(projectName)) {
                projectHome = projectConfig.get("projectHome").asText();
                break;
            }
        }

        // Read workflow file
        File workflowFile = new File(projectHome, workflowFileName);
        XmlMapper xmlMapper = new XmlMapper();
        JsonNode workflow = xmlMapper.readTree(new FileReader(workflowFile));

        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            ObjectNode rootObj = traverseWorkflowHierarchy(workflow, projectHome, xmlMapper);
        }
        // Build the hierarchical JSON structure
//        ObjectNode rootObj = traverseWorkflowHierarchy(workflow, projectHome, xmlMapper);
        System.out.println("Time: " + (System.currentTimeMillis() - start));
        // Output the result
//        ObjectMapper jsonMapper = new ObjectMapper();
//        System.out.println(jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootObj));
    }

    private static ObjectNode traverseWorkflowHierarchy(JsonNode workflow, String projectHome, XmlMapper xmlMapper) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode currentObj = mapper.createObjectNode();

        // Extract the the (very first) parent workflow name
        String workflowName = workflow.get("name").asText();
        currentObj.put("name", workflowName);

        // Initialize children array
        ArrayNode children = mapper.createArrayNode();

        // Get actions
        JsonNode actionsNode = workflow.get("actions").get("action");
        if (actionsNode != null) {
            if (actionsNode.isArray()) {
                // Iterate through actions
                for (JsonNode action : actionsNode) {
                    processAction(action, children, projectHome, xmlMapper, mapper);
                }
            } else  {
                // Single action
                processAction(actionsNode, children, projectHome, xmlMapper, mapper);
            }
        }

        // Attach children if any
        if (children.size() > 0) {
            currentObj.set("children", children);
        }

        return currentObj;
    }

    private static void processAction(JsonNode action, ArrayNode children, String projectHome, XmlMapper xmlMapper, ObjectMapper mapper) throws IOException {
        if ("WORKFLOW".equals(action.get("type").asText())) {
            String childWorkflowFileName = action.get("filename").asText(null);
            if (childWorkflowFileName == null) {
                return;
            }
            if (childWorkflowFileName.startsWith("${PROJECT_HOME}")) {
                childWorkflowFileName = childWorkflowFileName.replace("${PROJECT_HOME}", projectHome);
            }
            Path childWorkflowFile = Paths.get(childWorkflowFileName);
            if (Files.exists(childWorkflowFile)) {
                JsonNode childWorkflow = xmlMapper.readTree(new FileReader(childWorkflowFile.toFile()));
                ObjectNode childObj = traverseWorkflowHierarchy(childWorkflow, projectHome, xmlMapper);
                // Append the child object to the children array
                children.add(childObj);
            }
        }
    }
}
