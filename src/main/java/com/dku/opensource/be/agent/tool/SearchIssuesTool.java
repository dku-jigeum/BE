package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import com.dku.opensource.be.recommendation.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * search_issues — 자연어 질문을 임베딩해 pgvector 코사인 유사도로
 * 법안·청원·입법예고를 검색한다. (챗봇 Q&A용 — 이슈 미특정 질문 처리)
 *
 * 출력 형식 (줄 단위):
 *   RESULT:[bill]|ID:...|TITLE:...|DDAY:D-7
 */
@Component
@RequiredArgsConstructor
public class SearchIssuesTool implements AgentTool {

    private final EmbeddingService embeddingService;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override public String name() { return "search_issues"; }

    @Override public String description() {
        return "자연어 질문과 관련된 법안·청원·입법예고를 pgvector 코사인 유사도로 검색합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        if (input == null || input.isBlank()) return "검색 결과 없음";

        try {
            String queryVector = embeddingService.embed(input);
            StringBuilder sb = new StringBuilder();

            billRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 3)
                    .forEach(b -> appendResult(sb, "bill", b.getBillNo(), b.getTitle(),
                            b.getDeadline() != null ? b.getDeadline().toString() : null));

            petitionRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 2)
                    .forEach(p -> appendResult(sb, "petition", p.getPetitionNo(), p.getTitle(),
                            p.getDeadline() != null ? p.getDeadline().toString() : null));

            legislationNoticeRepository.findByEmbeddingSimilarityAfterDeadline(queryVector, 2)
                    .forEach(n -> appendResult(sb, "legislation", n.getBillId(), n.getTitle(),
                            n.getDeadline() != null ? n.getDeadline().toString() : null));

            return sb.isEmpty() ? "검색 결과 없음" : sb.toString().trim();

        } catch (Exception e) {
            return "검색 실패: " + e.getMessage();
        }
    }

    private void appendResult(StringBuilder sb, String type, String id, String title, String deadline) {
        sb.append(String.format("RESULT:[%s]|ID:%s|TITLE:%s|DDAY:%s%n",
                type, id, title, calcDDay(deadline)));
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
}
