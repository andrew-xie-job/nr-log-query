package com.example.nrlogs;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QueryController {

    private final LogQueryService service;

    public QueryController(LogQueryService service) {
        this.service = service;
    }

    /** Accounts for the dropdown, plus whether English (LLM) mode is available. */
    @GetMapping("/accounts")
    public ResponseEntity<?> accounts() {
        try {
            List<AccountInfo> accounts = service.accounts();
            return ResponseEntity.ok(Map.of(
                    "accounts", accounts,
                    "translationAvailable", service.translationAvailable()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "accounts", List.of(),
                    "translationAvailable", service.translationAvailable(),
                    "error", String.valueOf(e.getMessage())));
        }
    }

    /** English -> NRQL only (no execution). Used by the preview/edit step. */
    @PostMapping("/translate")
    public ResponseEntity<?> translate(@RequestBody TranslateRequest req) {
        if (req == null || req.text() == null || req.text().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question text is required."));
        }
        try {
            NrqlResponse r = service.translateOnly(req.text());
            return ResponseEntity.ok(Map.of(
                    "nrql", r.nrql(),
                    "explanation", r.explanation() == null ? "" : r.explanation()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    /** Run a query. If raw=false the text is treated as English and translated first. */
    @PostMapping("/query")
    public ResponseEntity<?> query(@RequestBody QueryRequest req) {
        if (req == null || req.text() == null || req.text().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Query text is required."));
        }
        try {
            QueryResult result = req.raw()
                    ? service.runNrql(req.accountId(), req.text())
                    : service.ask(req.accountId(), req.text());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
        }
    }

    public record TranslateRequest(String text) {
    }

    public record QueryRequest(long accountId, String text, boolean raw) {
    }
}
