package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PetitionApiDto {

    @JsonProperty("PTT_NO")
    private String petitionNo;

    @JsonProperty("PTT_NM")
    private String title;

    // CITZN_AGM_CNT는 "51,449" 형태 문자열, 의원소개 청원은 null
    @JsonProperty("CITZN_AGM_CNT")
    private String citizenAgreementCount;
}
