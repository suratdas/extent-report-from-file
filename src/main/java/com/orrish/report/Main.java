package com.orrish.report;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.ExtentSparkReporterConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        ExtentReports extent = new ExtentReports();
        ExtentSparkReporter spark = new ExtentSparkReporter("spark.html");
        spark.config().setTimelineEnabled(false);
        ExtentSparkReporterConfig extentSparkReporterConfig = ExtentSparkReporterConfig.builder()
                .timeStampFormat("HH:mm:ss")
                .build();
        spark.config(extentSparkReporterConfig);
        extent.attachReporter(spark);
        ExtentTest suiteNode = null, testNode = null;
        List<String> lines = Files.readAllLines(Path.of(args[0]));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("REPORT:")) {
                if (line.contains("SUITE_START")) {
                    String suiteName = line.split("SUITE_START:")[1];
                    if (suiteNode == null || !suiteNode.getModel().getName().equals(suiteName)) {
                        suiteNode = extent.createTest(suiteName);
                    }
                } else if (line.contains("SUITE_END")) {
                } else if (line.contains("TEST_START")) {
                    String testName = line.split("TEST_START:")[1];
                    testNode = (suiteNode == null) ? extent.createTest(testName) : suiteNode.createNode(testName);
                } else if (line.contains("TEST_END")) {
                    if (line.trim().endsWith("END")) continue;
                    Status status = Status.valueOf(line.split("TEST_END ")[1].trim());
                    if (status == Status.PASS) continue;
                    StringBuilder stepMessage = new StringBuilder();
                    i++;
                    do {
                        stepMessage.append(lines.get(i)).append(System.lineSeparator());
                        i++;
                    } while (!lines.get(i).contains("REPORT:"));
                    testNode.log(status, MarkupHelper.createCodeBlock(stepMessage.toString()));
                } else if (line.contains("STEP_MESSAGE_WITH_JSON")) {
                    if (line.trim().endsWith("END")) continue;
                    Status status = Status.valueOf(line.split("STEP_MESSAGE_WITH_JSON")[1].trim());
                    StringBuilder stepMessage = new StringBuilder();
                    i++;
                    do {
                        stepMessage.append(lines.get(i)).append(System.lineSeparator());
                        i++;
                    } while (!lines.get(i).contains("JSON:"));
                    String jsonString = lines.get(i).replace("JSON:", "");
                    String stepMessageString = String.valueOf(stepMessage).replaceFirst("MESSAGE: ", " ");
                    testNode.log(status, stepMessageString);
                    testNode.log(status, MarkupHelper.createCodeBlock(jsonString, CodeLanguage.JSON));
                } else if (line.contains("STEP_MESSAGE_WITH_SCREENSHOT")) {
                    if (line.trim().endsWith("END")) continue;
                    Status status = Status.valueOf(line.split("STEP_MESSAGE_WITH_SCREENSHOT")[1].trim());
                    StringBuilder stepMessage = new StringBuilder();
                    i++;
                    do {
                        stepMessage.append(lines.get(i)).append(System.lineSeparator());
                        i++;
                    } while (!lines.get(i).contains("PATH:"));
                    String screenshotPath = lines.get(i).replace("PATH:", "");
                    String stepMessageString = String.valueOf(stepMessage).replaceFirst("MESSAGE: ", " ");
                    testNode.log(status, stepMessageString, MediaEntityBuilder.createScreenCaptureFromPath(screenshotPath).build());
                } else if (line.contains("STEP_MESSAGE")) {
                    Status status = Status.valueOf(line.split("STEP_MESSAGE")[1].trim());
                    StringBuilder stepMessage = new StringBuilder();
                    i++;
                    do {
                        line = lines.get(i);
                        stepMessage.append(line).append(System.lineSeparator());
                        i++;
                    } while (!lines.get(i).startsWith("REPORT:"));
                    String stepMessageString = String.valueOf(stepMessage).replaceFirst("MESSAGE: ", " ").trim();
                    if (stepMessageString.contains(System.lineSeparator())) {
                        testNode.log(status, MarkupHelper.createCodeBlock(stepMessageString));
                    } else {
                        testNode.log(status, stepMessageString);
                    }
                }
            }
        }
        extent.flush();
    }
}
