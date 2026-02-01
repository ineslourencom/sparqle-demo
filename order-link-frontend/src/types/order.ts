export enum OrderStatus {
    PENDING = 'PENDING',
    PROCESSING = 'PROCESSING',
    RETRYING = 'RETRYING',
    COMPLETED = 'COMPLETED',
    FAILED = 'FAILED',
    CANCELLED = 'CANCELLED'
}

export enum OrderStateApi {
    PENDING = 'PENDING',
    INVENTORY_RESERVED = 'INVENTORY_RESERVED',
    LOGISTICS_CONFIRMED = 'LOGISTICS_CONFIRMED',
    COMPLETED = 'COMPLETED',
    FAILED_INVENTORY = 'FAILED_INVENTORY',
    FAILED_LOGISTICS = 'FAILED_LOGISTICS',
    FAILED = 'FAILED',
    CANCELLED = 'CANCELLED'
}

export interface OrderApi {
    merchantRef: string;
    trackingRef: string | null;
    state: OrderStateApi;
    details: string;
    updatedAt: string;
}

export interface OrderEventApi {
    orderId: string;
    state: OrderStateApi;
    timestamp: string;
}

export interface Order {
    merchantReference: string;
    externalReference: string | null;
    details: string;
    state: OrderStateApi;
    status: OrderStatus;
    updatedAt: string;
}

export interface OrderEvent {
    orderId: string;
    state: OrderStateApi;
    status: OrderStatus;
    timestamp: string;
}