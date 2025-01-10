package project.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import project.dto.MonthlyAnalysis;
import project.dto.SpendingTrends;
import project.service.FinancialAnalysisService;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {
    private final  FinancialAnalysisService analysisService;

    @GetMapping("/monthly/{yearMonth}")
    public ResponseEntity<MonthlyAnalysis> getMonthlyAnalysis(
            Authentication authentication,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth){
        MonthlyAnalysis analysis = analysisService.getMonthlyAnalysis(yearMonth);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/trends")
    public ResponseEntity<SpendingTrends> getSpendingTrends(
            Authentication authentication){
        SpendingTrends trends = analysisService.getSpendingTrends();
        return ResponseEntity.ok(trends);
    }

}


