package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetitionApiDto {

    @JsonProperty("petitId")
    private String petitId;

    @JsonProperty("petitSj")
    private String title;

    @JsonProperty("petitCn")
    private String content;

    @JsonProperty("agreCo")
    private Integer agreementCount;

    // "2026-05-30 23:59:59" 형태
    @JsonProperty("agreEndDe")
    private String agreementEndDate;
}
