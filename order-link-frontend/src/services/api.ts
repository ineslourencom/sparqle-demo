import axios from 'axios';
import type { Order, OrderEvent, OrderApi } from '../types/order';
import { OrderStatus, OrderStateApi } from '../types/order';
import type { PaginatedResponse } from '../types/pagination';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api',
});

const statusMapping: Record<OrderStateApi, OrderStatus> = {
    [OrderStateApi.PENDING]: OrderStatus.PENDING,
    [OrderStateApi.INVENTORY_RESERVED]: OrderStatus.PROCESSING,
    [OrderStateApi.LOGISTICS_CONFIRMED]: OrderStatus.PROCESSING,
    [OrderStateApi.COMPLETED]: OrderStatus.COMPLETED,
    [OrderStateApi.FAILED_INVENTORY]: OrderStatus.RETRYING,
    [OrderStateApi.FAILED_LOGISTICS]: OrderStatus.RETRYING,
    [OrderStateApi.FAILED]: OrderStatus.FAILED,
    [OrderStateApi.CANCELLED]: OrderStatus.CANCELLED,
};

const mapStatus = (state: OrderStateApi): OrderStatus => statusMapping[state] ?? OrderStatus.PROCESSING;

const mapOrder = (order: OrderApi): Order => ({
    merchantReference: order.merchantRef,
    externalReference: order.trackingRef,
    details: order.details,
    state: order.state,
    status: mapStatus(order.state),
    updatedAt: order.updatedAt,
});

export const fetchOrders = async (): Promise<PaginatedResponse<Order>> => {
    const response = await apiClient.get<PaginatedResponse<OrderApi>>('/orders');
    return {
        ...response.data,
        content: response.data.content.map(mapOrder),
    };
};

export const fetchOrderByReference = async (merchantReference: string): Promise<Order | null> => {
    try {
        const response = await apiClient.get<OrderApi>(`/orders/${encodeURIComponent(merchantReference)}`);
        return mapOrder(response.data);
    } catch (error) {
        if (axios.isAxiosError(error) && error.response?.status === 404) {
            return null;
        }
        throw error;
    }
};


export const subscribeToEvents = (
    onEvent: (event: OrderEvent) => void,
    onError?: (error: Event) => void
): (() => void) => {
    const baseUrl = apiClient.defaults.baseURL ?? '';
    const normalized = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
    const source = new EventSource(`${normalized}/events/subscribe`);

    const listener: EventListener = (event) => {
        try {
            const rawEvent = event as MessageEvent<string>;
            const payload = JSON.parse(rawEvent.data) as OrderApi;
            onEvent({
                orderId: payload.merchantRef,
                state: payload.state,
                status: mapStatus(payload.state),
                timestamp: payload.updatedAt,
            });
        } catch (parseError) {
            console.error('Failed to parse SSE payload', parseError);
        }
    };

    source.addEventListener('order-update', listener);
    source.onerror = (event) => {
        onError?.(event);
    };

    return () => {
        source.removeEventListener('order-update', listener);
        source.close();
    };
};