package com.prashanthganojiatwork.reconciler.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashanthganojiatwork.reconciler.model.PersonRecord;
import com.prashanthganojiatwork.reconciler.model.ReconciliationRequest;
import com.prashanthganojiatwork.reconciler.model.ReconciliationResponse;
import com.prashanthganojiatwork.reconciler.model.ThresholdConfig;
import com.prashanthganojiatwork.reconciler.orchestrator.ReconciliationOrchestrator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web controller rendering Thymeleaf templates for the reconciliation UI.
 * Provides input form, processes form submissions, and displays results.
 */
@Controller
@RequestMapping("/ui")
public class ReconcilerWebController {

    private final ReconciliationOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReconciliationResponse> resultStore = new ConcurrentHashMap<>();

    public ReconcilerWebController(ReconciliationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Renders the input form page.
     */
    @GetMapping("/input")
    public String showInputForm(Model model) {
        return "input";
    }

    /**
     * Renders the sample datasets showcase page.
     */
    @GetMapping("/datasets")
    public String showDatasets(Model model) {
        return "datasets";
    }

    /**
     * Handles form submission: parses JSON from textareas, builds a ReconciliationRequest,
     * calls the orchestrator, stores results, and redirects to the results page.
     * On parse error, re-renders the input form with an error message.
     */
    @PostMapping("/reconcile")
    public String reconcile(
            @RequestParam("sourceAJson") String sourceAJson,
            @RequestParam("sourceBJson") String sourceBJson,
            @RequestParam(value = "matchThreshold", required = false, defaultValue = "0.7") Double matchThreshold,
            @RequestParam(value = "reviewBandLowerBound", required = false, defaultValue = "0.4") Double reviewBandLowerBound,
            Model model) {

        // Parse Source A JSON
        List<PersonRecord> sourceA;
        try {
            sourceA = objectMapper.readValue(sourceAJson, new TypeReference<List<PersonRecord>>() {});
        } catch (JsonProcessingException e) {
            model.addAttribute("error", "Invalid JSON in Source A: " + e.getOriginalMessage());
            model.addAttribute("sourceAJson", sourceAJson);
            model.addAttribute("sourceBJson", sourceBJson);
            model.addAttribute("matchThreshold", matchThreshold);
            model.addAttribute("reviewBandLowerBound", reviewBandLowerBound);
            return "input";
        }

        // Parse Source B JSON
        List<PersonRecord> sourceB;
        try {
            sourceB = objectMapper.readValue(sourceBJson, new TypeReference<List<PersonRecord>>() {});
        } catch (JsonProcessingException e) {
            model.addAttribute("error", "Invalid JSON in Source B: " + e.getOriginalMessage());
            model.addAttribute("sourceAJson", sourceAJson);
            model.addAttribute("sourceBJson", sourceBJson);
            model.addAttribute("matchThreshold", matchThreshold);
            model.addAttribute("reviewBandLowerBound", reviewBandLowerBound);
            return "input";
        }

        // Validate non-empty sources
        if (sourceA.isEmpty()) {
            model.addAttribute("error", "Source A must contain at least one record.");
            model.addAttribute("sourceAJson", sourceAJson);
            model.addAttribute("sourceBJson", sourceBJson);
            model.addAttribute("matchThreshold", matchThreshold);
            model.addAttribute("reviewBandLowerBound", reviewBandLowerBound);
            return "input";
        }
        if (sourceB.isEmpty()) {
            model.addAttribute("error", "Source B must contain at least one record.");
            model.addAttribute("sourceAJson", sourceAJson);
            model.addAttribute("sourceBJson", sourceBJson);
            model.addAttribute("matchThreshold", matchThreshold);
            model.addAttribute("reviewBandLowerBound", reviewBandLowerBound);
            return "input";
        }

        // Build threshold config
        ThresholdConfig thresholds = new ThresholdConfig(matchThreshold, reviewBandLowerBound);

        // Build request and call orchestrator
        ReconciliationRequest request = new ReconciliationRequest(sourceA, sourceB, thresholds);
        ReconciliationResponse response = orchestrator.reconcile(request);

        // Store result for retrieval
        resultStore.put(response.jobId(), response);

        return "redirect:/ui/results/" + response.jobId();
    }

    /**
     * Renders the results page for a given job ID.
     * If the jobId is not found in the store, redirects to input with an error message.
     */
    @GetMapping("/results/{jobId}")
    public String showResults(@PathVariable String jobId, Model model, RedirectAttributes redirectAttributes) {
        ReconciliationResponse response = resultStore.get(jobId);
        if (response == null) {
            redirectAttributes.addFlashAttribute("error", "No results found for job ID: " + jobId + ". The results may have expired.");
            return "redirect:/ui/input";
        }
        model.addAttribute("response", response);
        model.addAttribute("jobId", jobId);
        return "results";
    }

    /**
     * Renders the match detail page for a specific match candidate within a job's results.
     * If the jobId is not found, redirects to input. If the index is out of range, redirects to results.
     */
    @GetMapping("/results/{jobId}/match/{index}")
    public String showMatchDetail(@PathVariable String jobId, @PathVariable int index,
                                  Model model, RedirectAttributes redirectAttributes) {
        ReconciliationResponse response = resultStore.get(jobId);
        if (response == null) {
            redirectAttributes.addFlashAttribute("error", "No results found for job ID: " + jobId + ". The results may have expired.");
            return "redirect:/ui/input";
        }

        if (response.matches() == null || index < 0 || index >= response.matches().size()) {
            redirectAttributes.addFlashAttribute("error", "Invalid match index: " + index);
            return "redirect:/ui/results/" + jobId;
        }

        model.addAttribute("match", response.matches().get(index));
        model.addAttribute("jobId", jobId);
        model.addAttribute("matchIndex", index);
        return "match-detail";
    }
}
