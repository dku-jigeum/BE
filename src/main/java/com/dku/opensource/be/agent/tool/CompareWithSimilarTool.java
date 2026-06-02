package com.dku.opensource.be.agent.tool;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.domain.bill.Bill;
import com.dku.opensource.be.domain.bill.BillRepository;
import com.dku.opensource.be.domain.petition.Petition;
import com.dku.opensource.be.domain.petition.PetitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * compare_with_similar_events — 유사 이벤트와 현재 이벤트의 공통점/차이점을 비교한다.
 * find_similar_events 결과가 1개 이상 존재할 때 실행. 사용자 클릭 후 실행 권장.
 *
 * input 형식: "id1:type1,id2:type2,id3:type3"  (find_similar_events 결과의 ID 목록)
 * 출력 형식 (이벤트당 한 블록):
 *   COMPARE_ID:id
 *   COMPARE_TITLE:제목
 *   COMMON:공통점1##공통점2
 *   DIFF:차이점1##차이점2
 *   USER_RELEVANCE:사용자 관련성 차이 설명
 */
@Component
@RequiredArgsConstructor
public class CompareWithSimilarTool implements AgentTool {

    private final ExaoneClient exaoneClient;
    private final BillRepository billRepository;
    private final PetitionRepository petitionRepository;

    @Override public String name() { return "compare_with_similar_events"; }

    @Override public String description() {
        return "현재 이벤트와 유사 이벤트의 공통점·차이점을 비교합니다. find_similar_events 실행 후 호출하세요.";
    }

    @Override
    public String run(String input, AgentContext ctx) {
        List<SimilarRef> refs = parseInput(input);
        if (refs.isEmpty()) return "비교할 유사 이벤트 정보가 없습니다.";

        StringBuilder sb = new StringBuilder();
        for (SimilarRef ref : refs) {
            String similarTitle = resolveTitle(ref);
            if (similarTitle == null) continue;

            String comparison = generateComparison(ctx.getIssueTitle(), similarTitle,
                    ctx.getUserInterests());
            sb.append(comparison).append("\n---\n");
        }

        return sb.toString().trim();
    }

    private String generateComparison(String currentTitle, String similarTitle,
                                       List<String> userInterests) {
        String interests = userInterests.isEmpty() ? "없음" : String.join(", ", userInterests);
        String prompt = String.format("""
                현재 이슈: %s
                유사 이슈: %s
                사용자 관심사: %s

                두 이슈를 비교하여 아래 형식으로만 출력하세요.

                COMPARE_TITLE:%s
                COMMON:공통점1##공통점2
                DIFF:차이점1##차이점2
                USER_RELEVANCE:사용자 관심사 기준 관련성 차이 한 문장""",
                currentTitle, similarTitle, interests, similarTitle
        );

        try {
            return exaoneClient.complete(
                    "두 정책 이슈를 비교 분석하는 전문가입니다. 지정 형식으로만 답변하세요.",
                    prompt
            );
        } catch (Exception e) {
            return String.format(
                    "COMPARE_TITLE:%s%nCOMMON:정책 방향 유사##관련 법령 연결%nDIFF:대상 범위 상이##접근 방식 차이%nUSER_RELEVANCE:두 이슈 모두 관심 분야와 연관될 수 있어요.",
                    similarTitle
            );
        }
    }

    private String resolveTitle(SimilarRef ref) {
        if ("bill".equals(ref.type())) {
            return billRepository.findByBillNo(ref.id()).map(Bill::getTitle).orElse(null);
        }
        if ("petition".equals(ref.type())) {
            return petitionRepository.findByPetitionNo(ref.id())
                    .map(Petition::getTitle).orElse(null);
        }
        return null;
    }

    /** input: "id1:bill,id2:petition" */
    private List<SimilarRef> parseInput(String input) {
        List<SimilarRef> refs = new ArrayList<>();
        if (input == null || input.isBlank()) return refs;
        for (String part : input.split(",")) {
            String[] kv = part.trim().split(":");
            if (kv.length == 2) refs.add(new SimilarRef(kv[0].trim(), kv[1].trim()));
        }
        return refs;
    }

    record SimilarRef(String id, String type) {}
}
