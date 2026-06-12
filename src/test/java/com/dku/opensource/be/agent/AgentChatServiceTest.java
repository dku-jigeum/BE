package com.dku.opensource.be.agent;

import com.dku.opensource.be.agent.model.ExaoneClient;
import com.dku.opensource.be.agent.tool.AgentContext;
import com.dku.opensource.be.agent.tool.GetIssueDetailTool;
import com.dku.opensource.be.agent.tool.SearchIssuesTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentChatServiceTest {

    @Mock SearchIssuesTool searchIssuesTool;
    @Mock GetIssueDetailTool getIssueDetailTool;
    @Mock ExaoneClient exaoneClient;

    @InjectMocks AgentChatService agentChatService;

    @Test
    void 검색_결과_없으면_폴백_응답을_반환하고_LLM을_호출하지_않는다() {
        when(searchIssuesTool.run(anyString(), any(AgentContext.class))).thenReturn("검색 결과 없음");

        AgentChatService.ChatResponse res = agentChatService.chat("외계인 법안 있어?", "user1");

        assertThat(res.sources()).isEmpty();
        assertThat(res.answer()).contains("찾지 못했어요");
        verify(exaoneClient, never()).complete(anyString(), anyString());
    }

    @Test
    void 검색_결과를_파싱해_상위_2건만_본문을_조회하고_답변을_생성한다() {
        when(searchIssuesTool.run(anyString(), any(AgentContext.class))).thenReturn("""
                RESULT:[bill]|ID:2200063|TITLE:최저임금법 일부개정법률안|DDAY:D-7
                RESULT:[petition]|ID:ABC123|TITLE:최저임금 인상 청원|DDAY:D-3
                RESULT:[legislation]|ID:PRC_001|TITLE:최저임금법 시행령 입법예고|DDAY:D-10""");
        when(getIssueDetailTool.run(anyString(), any(AgentContext.class))).thenReturn("TYPE:bill\nCONTENT:본문");
        when(exaoneClient.complete(anyString(), anyString())).thenReturn("최저임금 관련 법안이 1건 진행 중입니다.");

        AgentChatService.ChatResponse res = agentChatService.chat("최저임금 관련 법안 뭐 있어?", "user1");

        assertThat(res.answer()).isEqualTo("최저임금 관련 법안이 1건 진행 중입니다.");
        assertThat(res.sources()).hasSize(3);
        assertThat(res.sources().get(0).id()).isEqualTo("2200063");
        assertThat(res.sources().get(0).type()).isEqualTo("bill");
        assertThat(res.sources().get(1).dDay()).isEqualTo("D-3");
        // 상위 2건만 상세 조회
        verify(getIssueDetailTool, times(1)).run(eq("bill:2200063"), any(AgentContext.class));
        verify(getIssueDetailTool, times(1)).run(eq("petition:ABC123"), any(AgentContext.class));
        verify(getIssueDetailTool, never()).run(eq("legislation:PRC_001"), any(AgentContext.class));
    }

    @Test
    void LLM_호출이_실패해도_근거_목록과_함께_폴백_문구를_반환한다() {
        when(searchIssuesTool.run(anyString(), any(AgentContext.class)))
                .thenReturn("RESULT:[bill]|ID:2200063|TITLE:최저임금법 일부개정법률안|DDAY:D-7");
        when(getIssueDetailTool.run(anyString(), any(AgentContext.class))).thenReturn("TYPE:bill\nCONTENT:본문");
        when(exaoneClient.complete(anyString(), anyString())).thenThrow(new RuntimeException("LLM 호출 실패"));

        AgentChatService.ChatResponse res = agentChatService.chat("최저임금?", "user1");

        assertThat(res.answer()).contains("실패");
        assertThat(res.sources()).hasSize(1);
    }

    @Test
    void 빈_질문은_예외를_던진다() {
        assertThatThrownBy(() -> agentChatService.chat("  ", "user1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
