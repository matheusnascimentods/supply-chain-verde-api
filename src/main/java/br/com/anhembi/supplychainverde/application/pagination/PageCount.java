package br.com.anhembi.supplychainverde.application.pagination;

public final class PageCount {
    private PageCount() {}

    public static int totalPages(long totalElements, int pageSize) {
        if (totalElements <= 0) {
            return 0;
        }
        return Math.toIntExact((totalElements - 1) / pageSize + 1);
    }
}
