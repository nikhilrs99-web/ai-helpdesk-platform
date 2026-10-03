package com.helpdesk.ai.web;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketAnalysisTest {

    private final ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();

    @Test
    void parsesPlainJson() {
        TicketAnalysis a = TicketAnalysis.parse("{\"sentiment\":\"negative\",\"category\":\"ACCESS\"}", mapper);
        assertThat(a).isEqualTo(new TicketAnalysis("NEGATIVE", "ACCESS"));
    }

    @Test
    void parsesJsonWrappedInCodeFences() {
        String fence = "`".repeat(3);
        TicketAnalysis a = TicketAnalysis.parse(fence + "json\n{\"sentiment\":\"NEUTRAL\",\"category\":\"BUG\"}\n" + fence, mapper);
        assertThat(a.category()).isEqualTo("BUG");
    }

    @Test
    void rejectsUnknownValuesAndGarbage() {
        assertThatThrownBy(() -> TicketAnalysis.parse("{\"sentiment\":\"ANGRY\",\"category\":\"BUG\"}", mapper))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TicketAnalysis.parse("sure! here you go", mapper))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
