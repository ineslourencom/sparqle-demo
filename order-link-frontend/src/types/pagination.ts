export interface PageMetadata {
    pageNumber: number;
    pageSize: number;
    sort: unknown;
    offset: number;
    paged: boolean;
    unpaged: boolean;
}

export interface PaginatedResponse<T> {
    content: T[];
    pageable: PageMetadata;
    totalElements: number;
    totalPages: number;
    last: boolean;
    numberOfElements: number;
    sort: unknown;
    first: boolean;
    size: number;
    number: number;
    empty: boolean;
}