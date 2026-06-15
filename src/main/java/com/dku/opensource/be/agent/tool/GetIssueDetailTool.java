package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.legislation.LegislationNotice;
import com.dku.opensource.be.domain.legislation.LegislationNoticeRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * get_issue_detail — "type:id" 입력으로 특정 이슈의 제목·본문·마감일을 조회한다.
 * 챗봇 Q&A에서 search_issues 결과 중 답변 근거로 쓸 본문을 가져올 때 사용.
 *
 * 입력 예: "bill:2200063", "petition:4F91926BDD", "legislation:PRC_..."
 */
@Component
@RequiredArgsConstructor
public class GetIssueDetailTool implements AgentTool {

    private static final int CONTENT_MAX_LENGTH = 800;

    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;
    private final LegislationNoticeRepository legislationNoticeRepository;

    @Override public String name() { return "get_issue_detail"; }

    @Override public String description() {
        return "이슈 식별자(type:id)로 제목·본문·마감일을 조회합니다.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        if (input == null || !input.contains(":")) {
            return "잘못된 입력 형식 (type:id 필요): " + input;
        }
        String[] parts = input.split(":", 2);
        String type = parts[0].trim().toLowerCase();
        String id = parts[1].trim();

        try {
            return switch (type) {
                case "bill" -> {
                    Bill b = billRepository.findByBillNo(id)
                            .orElseThrow(() -> new IllegalArgumentException("법안 없음: " + id));
                    yield format("bill", id, b.getTitle(), b.getContent(),
                            b.getDeadline() != null ? b.getDeadline().toString() : null);
                }
                case "petition" -> {
                    Petition p = petitionRepository.findByPetitionNo(id)
                            .orElseThrow(() -> new IllegalArgumentException("청원 없음: " + id));
                    yield format("petition", id, p.getTitle(), p.getContent(),
                            p.getDeadline() != null ? p.getDeadline().toString() : null);
                }
                case "legislation" -> {
                    LegislationNotice n = legislationNoticeRepository.findByBillId(id)
                            .orElseThrow(() -> new IllegalArgumentException("입법예고 없음: " + id));
                    yield format("legislation", id, n.getTitle(), n.getContent(),
                            n.getDeadline() != null ? n.getDeadline().toString() : null);
                }
                default -> "알 수 없는 타입: " + type;
            };
        } catch (Exception e) {
            return "상세 조회 실패: " + e.getMessage();
        }
    }

    private String format(String type, String id, String title, String content, String deadline) {
        String body = (content != null && !content.isBlank())
                ? (content.length() > CONTENT_MAX_LENGTH
                        ? content.substring(0, CONTENT_MAX_LENGTH) + "..." : content)
                : "(본문 없음)";
        return String.format("TYPE:%s%nID:%s%nTITLE:%s%nDEADLINE:%s%nCONTENT:%s",
                type, id, title, deadline != null ? deadline : "상시", body);
    }
}
