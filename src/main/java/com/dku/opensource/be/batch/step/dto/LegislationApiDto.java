package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LegislationApiDto {

    @JsonProperty("BILL_ID")
    private String billId;

    @JsonProperty("BILL_NO")
    private String billNo;

    @JsonProperty("BILL_NAME")
    private String billName;

    @JsonProperty("NOTI_ED_DT")
    private String notiEdDt;
}
