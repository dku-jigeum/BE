package com.dku.opensource.be.api;

import com.dku.opensource.be.agent.AgentService;
import com.dku.opensource.be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * POST /api/agent/analyze — 이슈 AI 분석 (ReAct + EXAONE)
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/analyze")
    public ApiResponse<AgentService.AnalyzeResponse> analyze(
            @AuthenticationPrincipal String userId,
            @RequestBody AnalyzeRequest req) {
        return ApiResponse.success(agentService.analyze(req.issueId(), req.issueType(), userId));
    }

    record AnalyzeRequest(String issueId, String issueType) {}
}
