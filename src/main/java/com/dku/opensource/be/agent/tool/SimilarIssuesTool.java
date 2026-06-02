package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import com.dku.opensource.be.recommendation.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * similar_issues_tool — pgvector 코사인 유사도로 유사 이슈 3건 검색.
 * 결과마다 EXAONE으로 유사 이유 한 줄을 생성해 함께 반환한다.
 */
@Component
@RequiredArgsConstructor
public class SimilarIssuesTool implements AgentTool {

    private final ExaoneClient exaoneClient;
    private final EmbeddingService embeddingService;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;

    @Override public String name() { return "find_similar_events"; }

    @Override public String description() {
        return "현재 이슈와 유사한 법안·청원을 pgvector 코사인 유사도로 검색해 3건 반환합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        String title = ctx.getIssueTitle();
        if (title == null || title.isBlank()) return "유사 이슈 없음";

        try {
            String queryVector = embeddingService.embed(title);

            List<SimilarIssue> results = new ArrayList<>();

            billRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 4)
                    .stream()
                    .filter(b -> !b.getBillNo().equals(ctx.getIssueId()))
                    .limit(2)
                    .forEach(b -> results.add(new SimilarIssue(b.getBillNo(), "bill", b.getTitle(),
                            b.getDeadline() != null ? b.getDeadline().toString() : null)));

            petitionRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 3)
                    .stream()
                    .filter(p -> !p.getPetitionNo().equals(ctx.getIssueId()))
                    .limit(1)
                    .forEach(p -> results.add(new SimilarIssue(p.getPetitionNo(), "petition", p.getTitle(),
                            p.getDeadline() != null ? p.getDeadline().toString() : null)));

            if (results.isEmpty()) return "유사 이슈 없음";

            // 각 유사 이슈에 대해 EXAONE으로 한 줄 이유 생성
            StringBuilder sb = new StringBuilder();
            for (SimilarIssue r : results) {
                String reason = generateReason(ctx.getIssueTitle(), r.title());
                String dDay = calcDDay(r.deadline());
                sb.append(String.format("SIMILAR:[%s]|ID:%s|TITLE:%s|DDAY:%s|REASON:%s%n",
                        r.type(), r.id(), r.title(), dDay, reason));
            }
            return sb.toString().trim();

        } catch (Exception e) {
            return "유사 이슈 검색 실패: " + e.getMessage();
        }
    }

    private String generateReason(String currentTitle, String similarTitle) {
        try {
            String prompt = String.format(
                    "현재 이슈: %s\n유사 이슈: %s\n\n두 이슈가 왜 유사한지 15자 이내 한 문장으로만 답하세요.",
                    currentTitle, similarTitle);
            return exaoneClient.complete(
                    "두 이슈의 유사 이유를 15자 이내로 설명하는 전문가입니다. 설명 없이 이유 문장만 출력하세요.",
                    prompt).replaceAll("[\\n\\r]", " ").trim();
        } catch (Exception e) {
            return "관련 키워드가 유사해요";
        }
    }

    private String calcDDay(String deadline) {
        if (deadline == null || deadline.isBlank()) return "상시";
        try {
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), java.time.LocalDate.parse(deadline));
            return days >= 0 ? "D-" + days : "마감";
        } catch (Exception e) {
            return "미상";
        }
    }

    public record SimilarIssue(String id, String type, String title, String deadline) {}
}
