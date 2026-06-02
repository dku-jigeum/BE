package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.react.ReActLoop;
import com.dku.opensource.be.agent.react.ReActResult;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.agent.tool.SimilarIssuesTool;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 에이전트 분석 서비스.
 * 이슈 정보를 로드해 AgentContext를 구성하고 ReActLoop를 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final ReActLoop reActLoop;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    public AnalyzeResponse analyze(String issueId, String issueType, String userId) {
        AgentContext ctx = buildContext(issueId, issueType, userId);

        String goal = String.format(
                "이슈 '%s'에 대해 3줄 요약, 사용자 영향 분석, 유사 이슈 검색, 캘린더 등록 필요 여부를 순서대로 분석하세요.",
                ctx.getIssueTitle()
        );

        log.info("[Agent] 분석 시작 — issueId={}, type={}, user={}", issueId, issueType, userId);
        ReActResult result = reActLoop.run(goal, ctx);
        log.info("[Agent] 분석 완료 — calendarSuggested={}", result.isCalendarSuggested());

        return buildResponse(result, ctx);
    }

    private AgentContext buildContext(String issueId, String issueType, String userId) {
        return switch (issueType.toLowerCase()) {
            case "bill" -> {
                Bill bill = billRepository.findByBillNo(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("법안 없음: " + issueId));
                yield AgentContext.builder()
                        .issueId(issueId).issueType("bill").userId(userId)
                        .issueTitle(bill.getTitle())
                        .issueContent(bill.getContent())
                        .deadline(bill.getDeadline() != null ? bill.getDeadline().toString() : null)
                        .build();
            }
            case "petition" -> {
                Petition petition = petitionRepository.findByPetitionNo(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("청원 없음: " + issueId));
                yield AgentContext.builder()
                        .issueId(issueId).issueType("petition").userId(userId)
                        .issueTitle(petition.getTitle())
                        .issueContent(petition.getContent())
                        .deadline(petition.getDeadline() != null ? petition.getDeadline().toString() : null)
                        .build();
            }
            case "legislation" -> {
                LegislationNotice notice = legislationNoticeRepository.findByBillId(issueId)
                        .orElseThrow(() -> new IllegalArgumentException("입법예고 없음: " + issueId));
                yield AgentContext.builder()
                        .issueId(issueId).issueType("legislation").userId(userId)
                        .issueTitle(notice.getTitle())
                        .issueContent(null)
                        .deadline(notice.getDeadline() != null ? notice.getDeadline().toString() : null)
                        .build();
            }
            default -> throw new IllegalArgumentException("알 수 없는 issueType: " + issueType);
        };
    }

    private AnalyzeResponse buildResponse(ReActResult result, AgentContext ctx) {
        // summary: 줄바꿈으로 분리해 List<String>
        List<String> summaryLines = result.getSummary() != null
                ? Arrays.stream(result.getSummary().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .limit(3)
                    .collect(Collectors.toList())
                : List.of("요약 생성에 실패했습니다.");

        // similarIssues: raw 텍스트 파싱
        List<SimilarIssueDto> similar = parseSimilarIssues(result.getSimilarIssuesRaw());

        return new AnalyzeResponse(
                summaryLines,
                result.getImpact() != null ? result.getImpact() : "영향 분석 정보 없음.",
                similar,
                result.isCalendarSuggested(),
                result.getDaysLeft()
        );
    }

    private List<SimilarIssueDto> parseSimilarIssues(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("없음")) return List.of();
        return Arrays.stream(raw.split("\n"))
                .filter(line -> line.startsWith("- "))
                .map(line -> {
                    // "- [bill] 법안 제목 (billNo)" 형태 파싱
                    String content = line.substring(2);
                    String type = content.contains("[bill]") ? "bill"
                            : content.contains("[petition]") ? "petition" : "legislation";
                    String title = content.replaceAll("\\[.*?\\]", "")
                            .replaceAll("\\(.*?\\)", "").trim();
                    String id = "";
                    if (content.contains("(") && content.contains(")")) {
                        id = content.substring(content.lastIndexOf("(") + 1, content.lastIndexOf(")"));
                    }
                    return new SimilarIssueDto(id, type, title);
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    public record AnalyzeResponse(
            List<String> summary,
            String impact,
            List<SimilarIssueDto> similarIssues,
            boolean calendarSuggested,
            Integer daysLeft
    ) {}

    public record SimilarIssueDto(String id, String type, String title) {}
}
