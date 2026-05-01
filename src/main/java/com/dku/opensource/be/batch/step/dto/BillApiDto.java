package com.dku.opensource.be.batch.step.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillApiDto {

    @JsonProperty("BILL_ID")
    private String billId;

    @JsonProperty("BILL_NO")
    private String billNo;

    @JsonProperty("BILL_NAME")
    private String billName;

    @JsonProperty("LINK_URL")
    private String linkUrl;

    @JsonProperty("PROPOSE_DT")
    private String proposeDt;

    @JsonProperty("PROC_DT")
    private String procDt;

    @JsonProperty("CURR_COMMITTEE")
    private String committee;

    @JsonProperty("RST_PROPOSER")
    private String proposer;

    @JsonProperty("PASS_GUBUN")
    private String passGubun;
}
