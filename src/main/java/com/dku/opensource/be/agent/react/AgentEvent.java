package com.dku.opensource.be.agent.react;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * ReAct 루프가 진행 상황을 외부로 흘려보내기 위한 이벤트.
 * SSE로 직렬화돼 프론트가 "지금 어떤 도구를 쓰는 중"인지 실시간 표시한다.
 *
 * type: thought | tool_start | observation | answer | error
 * (sources / done 은 서비스 레이어에서 별도로 emit)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentEvent(String type, String name, String input, String text) {

    public static AgentEvent thought(String text) {
        return new AgentEvent("thought", null, null, text);
    }

    public static AgentEvent toolStart(String name, String input) {
        return new AgentEvent("tool_start", name, input, null);
    }

    public static AgentEvent observation(String name, String text) {
        return new AgentEvent("observation", name, null, text);
    }

    public static AgentEvent answer(String text) {
        return new AgentEvent("answer", null, null, text);
    }

    public static AgentEvent error(String text) {
        return new AgentEvent("error", null, null, text);
    }
}
