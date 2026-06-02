package com.dku.opensource.be.agent.tool;

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
 */
@Component
@RequiredArgsConstructor
public class SimilarIssuesTool implements AgentTool {

    private final EmbeddingService embeddingService;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;

    @Override public String name() { return "similar_issues_tool"; }

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

            // 법안 유사도 검색
            billRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 2)
                    .stream()
                    .filter(b -> !b.getBillNo().equals(ctx.getIssueId()))
                    .limit(2)
                    .forEach(b -> results.add(new SimilarIssue(b.getBillNo(), "bill", b.getTitle(),
                            b.getDeadline() != null ? b.getDeadline().toString() : null)));

            // 청원 유사도 검색
            petitionRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 2)
                    .stream()
                    .filter(p -> !p.getPetitionNo().equals(ctx.getIssueId()))
                    .limit(1)
                    .forEach(p -> results.add(new SimilarIssue(p.getPetitionNo(), "petition", p.getTitle(),
                            p.getDeadline() != null ? p.getDeadline().toString() : null)));

            if (results.isEmpty()) return "유사 이슈 없음";

            StringBuilder sb = new StringBuilder("유사 이슈 검색 결과:\n");
            results.forEach(r -> sb.append("- [").append(r.type()).append("] ").append(r.title())
                    .append(" (").append(r.id()).append(")\n"));
            return sb.toString();

        } catch (Exception e) {
            return "유사 이슈 검색 실패: " + e.getMessage();
        }
    }

    public record SimilarIssue(String id, String type, String title, String deadline) {}
}
