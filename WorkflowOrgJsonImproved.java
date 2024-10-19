package org.apache.hop.ui.hopgui;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

public class Test {

    public static void main(String[] args) throws IOException {
        Path hopConfigFile = Paths.get("D:\\dev\\tools\\apache-tomcat-9.0.95\\webapps\\hop\\config\\hop-config.json");
        JSONObject hopConfig = new JSONObject(new String(Files.readAllBytes(hopConfigFile)));

        String projectName = "longvh";
        String workflowFileName = "parent.hwf";
        // Get project home
        String projectHome = null;
        JSONArray projectConfigurations = hopConfig.getJSONObject("projectsConfig").getJSONArray("projectConfigurations");
        for (int i = 0; i < projectConfigurations.length(); i++) {
            JSONObject projectConfig = projectConfigurations.getJSONObject(i);
            if (projectConfig.getString("projectName").equals(projectName)) {
                projectHome = projectConfig.getString("projectHome");
                break;
            }
        }

        // Read workflow file
        File workflowFile = new File(projectHome, workflowFileName);
        JSONObject workflow = XML.toJSONObject(new FileReader(workflowFile));


        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            JSONObject rootObj = traverseWorkflowHierarchy(workflow, projectHome);
        }
        // Build the hierarchical JSON structure
        System.out.println("Time: " + (System.currentTimeMillis() - start));

//        System.out.println(rootObj.toString(4));
    }

    private static JSONObject traverseWorkflowHierarchy(JSONObject workflow, String projectHome) throws IOException {
        // Use LinkedHashMap to preserve key order
        Map<String, Object> currentObjMap = new LinkedHashMap<>();

        // Extract the workflow name
        String workflowName = workflow.getJSONObject("workflow").getString("name");
        currentObjMap.put("name", workflowName);

        // Initialize children array
        JSONArray children = new JSONArray();

        // Get actions
        Object actionsObj = workflow.getJSONObject("workflow").getJSONObject("actions").opt("action");
        if (actionsObj != null) {
            JSONArray actions;
            if (actionsObj instanceof JSONArray) {
                actions = (JSONArray) actionsObj;
            } else {
                actions = new JSONArray();
                actions.put(actionsObj);
            }

            // Iterate through actions
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                if (action.getString("type").equals("WORKFLOW")) {
                    String childWorkflowFileName = action.getString("filename");
                    if (childWorkflowFileName == null) {
                        continue;
                    }
                    if (childWorkflowFileName.startsWith("${PROJECT_HOME}")) {
                        childWorkflowFileName = childWorkflowFileName.replace("${PROJECT_HOME}", projectHome);
                    }
                    Path childWorkflowFile = Paths.get(childWorkflowFileName);
                    if (Files.exists(childWorkflowFile)) {
                        JSONObject childWorkflow = XML.toJSONObject(new FileReader(childWorkflowFile.toFile()));
                        JSONObject childObj = traverseWorkflowHierarchy(childWorkflow, projectHome);
                        // Append the child object to the children array
                        children.put(childObj);
                    }
                }
            }
        }

        // Attach children if any
        if (children.length() > 0) {
            currentObjMap.put("children", children);
        }

        // Create JSONObject from the LinkedHashMap to maintain order
        JSONObject currentObj = new JSONObject(currentObjMap);

        return currentObj;
    }

}
