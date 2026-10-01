package br.com.anhembi.supplychainverde.application.dto.report;

import java.util.List;

public record ReportPageDTO(List<ReportListItemDTO> items, int limit, int offset, boolean hasNext, int totalPages) {}
