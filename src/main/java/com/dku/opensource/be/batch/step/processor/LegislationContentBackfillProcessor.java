package com.dku.opensource.be.batch.step.processor;

import com.dku.opensource.be.domain.legislation.LegislationNotice;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LegislationContentBackfillProcessor implements ItemProcessor<LegislationNotice, LegislationNotice> {

    private static final String URL_ONGOING = "https://pal.assembly.go.kr/napal/lgsltpa/lgsltpaOngoing/view.do?lgsltPaId=";
    private static final String URL_DONE    = "https://pal.assembly.go.kr/napal/lgsltpa/lgsltpaDone/view.do?menuNo=1100027&lgsltPaId=";
    private static final String USER_AGENT  = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 5_000;

    @Override
    public LegislationNotice process(LegislationNotice notice) {
        String content = tryFetch(URL_ONGOING + notice.getBillId());
        if (content == null) content = tryFetch(URL_DONE + notice.getBillId());

        if (content == null) {
            log.warn("본문 fetch 실패 — 스킵 (billId={})", notice.getBillId());
            return null;
        }
        notice.updateContent(content);
        return notice;
    }

    private String tryFetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MS)
                    .userAgent(USER_AGENT)
                    .get();
            Element el = doc.selectFirst("div.desc");
            String text = el != null ? el.text() : null;
            if (text == null || text.isBlank()) return null;
            return text.length() > 4000 ? text.substring(0, 4000) : text;
        } catch (Exception e) {
            log.debug("fetch 실패 (url={}): {}", url, e.getMessage());
            return null;
        }
    }
}
