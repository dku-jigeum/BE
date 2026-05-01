package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegislationApiDto {

    @JsonProperty("NOTICE_NO")
    private String noticeNo;

    @JsonProperty("TITLE")
    private String title;

    @JsonProperty("CONTENT")
    private String content;

    @JsonProperty("END_DT")
    private String endDt;
}
