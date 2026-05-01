package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetitionApiDto {

    @JsonProperty("id")
    private String petitionNo;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @JsonProperty("expireAt")
    private String expireAt;

    @JsonProperty("agreeCount")
    private int agreeCount;
}
