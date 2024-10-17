package org.apache.hop.ui.hopgui;


import org.apache.hop.core.Const;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class GetWorkflowHierarchyApi extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String projectName = req.getParameter("projectName");
        String workflowFileName = req.getParameter("workflowFileName");

        if (projectName == null || projectName.isEmpty() || workflowFileName == null || workflowFileName.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing project name or workflow file name");
            return;
        }
        String hopConfigFolder = Const.HOP_CONFIG_FOLDER;
        if (hopConfigFolder == null || hopConfigFolder.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "HOP_CONFIG_FOLDER environment variable is not set");
            return;
        }
        // Read hop-config.json file
        File hopConfigFile = new File(hopConfigFolder, "hop-config.json");
        if (!hopConfigFile.exists()) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "hop-config.json file not found");
            return;
        }
        JSONObject hopConfig = new JSONObject(new FileReader(hopConfigFile));

        // Get project home
        String projectHome = null;
        JSONArray projectConfigs = hopConfig.getJSONObject("projectsConfig").getJSONArray("projectConfigurations");
        for (int i = 0; i < projectConfigs.length(); i++) {
            JSONObject projectConfig = projectConfigs.getJSONObject(i);
            if (projectConfig.getString("projectName").equals(projectName)) {
                projectHome = projectConfig.getString("projectHome");
                break;
            }
        }

        // Read workflow file
        File workflowFile = new File(projectHome, workflowFileName);
        JSONObject workflow = XML.toJSONObject(new FileReader(workflowFile));

        // Recursively traverse workflow hierarchy
        JSONArray childWorkflows = new JSONArray();
        traverseWorkflowHierarchy(workflow, childWorkflows, projectHome);

        // Return output as JSON
        resp.setContentType("application/json");
        resp.getWriter().write(childWorkflows.toString());
    }

    private void traverseWorkflowHierarchy(JSONObject workflow, JSONArray childWorkflows, String projectHome) throws FileNotFoundException {
        JSONArray actions = workflow.getJSONObject("workflow").getJSONArray("actions");
        for (int i = 0; i < actions.length(); i++) {
            JSONObject action = actions.getJSONObject(i);
            if (action.getString("type").equals("WORKFLOW")) {
                String childWorkflowFileName = action.getString("filename");
                File childWorkflowFile = new File(projectHome, childWorkflowFileName);
                if (childWorkflowFile.exists()) {
                    JSONObject childWorkflow = XML.toJSONObject(new FileReader(childWorkflowFile));
                    childWorkflows.put(childWorkflow);
                    traverseWorkflowHierarchy(childWorkflow, childWorkflows, projectHome);
                }
            }
        }
    }
}
