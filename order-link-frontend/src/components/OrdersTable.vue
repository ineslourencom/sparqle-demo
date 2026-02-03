<script setup lang="ts">
import DataTable from 'primevue/datatable';
import Column from 'primevue/column';
import Badge from 'primevue/badge';
import type { Order } from '../types/order';
import { OrderStatus } from '../types/order';

defineProps<{
    orders: Order[];
}>();

const getSeverity = (status: OrderStatus) => {
    switch (status) {
        case OrderStatus.COMPLETED:
            return 'success';
        case OrderStatus.PROCESSING:
            return 'info';
        case OrderStatus.RETRYING:
            return 'warning';
        case OrderStatus.PENDING:
            return 'warning';
        case OrderStatus.FAILED:
            return 'danger';
        case OrderStatus.CANCELLED:
            return 'secondary';
        default:
            return undefined;
    }
};

const rowStyle = (data: Order) => {
    if (data.status === OrderStatus.FAILED) {
        return { background: 'rgba(67, 56, 202, 0.12)' };
    }
    if (data.status === OrderStatus.CANCELLED) {
        return { background: 'rgba(67, 56, 202, 0.08)' };
    }
};

const statusLabel = (status: OrderStatus) => status.replaceAll('_', ' ');

const formatDateTime = (value: string | Date) => {
    const locale = navigator.language || 'en-US';
    return new Intl.DateTimeFormat(locale, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(new Date(value));
};
</script>

<template>
    <div class="card orders-table-card">
        <DataTable
            class="orders-table"
            :value="orders"
            dataKey="merchantReference"
            :rowStyle="rowStyle"
            stripedRows
            showGridlines
            responsiveLayout="scroll"
            paginator
            :rows="5"
            tableStyle="min-width: 52rem"
            emptyMessage="No orders to display yet."
        >
            <template #header>
                <div class="orders-table-header">
                    <span class="orders-table-title">Order History</span>
                    <span class="orders-table-hint">{{ orders.length }} total</span>
                </div>
            </template>
            <Column field="merchantReference" header="Reference" sortable>
                <template #body="slotProps">
                    <span class="order-ref">{{ slotProps.data.merchantReference }}</span>
                </template>
            </Column>
            <Column field="externalReference" header="Third Party Tracker" sortable>
                <template #body="slotProps">
                    <span class="order-tracking" :class="{ 'order-tracking--missing': !slotProps.data.trackingRef }">
                        {{ slotProps.data.trackingRef || 'Not assigned' }}
                    </span>
                </template>
            </Column>
            <Column field="details" header="Details">
                <template #body="slotProps">
                    <span class="order-details">{{ slotProps.data.details }}</span>
                </template>
            </Column>
            <Column field="status" header="Status" sortable>
                <template #body="slotProps">
                    <Badge class="status-badge" :value="statusLabel(slotProps.data.status)" :severity="getSeverity(slotProps.data.status)" />
                </template>
            </Column>
            <Column field="updatedAt" header="Last Updated" sortable>
                <template #body="slotProps">
                    {{ formatDateTime(slotProps.data.updatedAt) }}
                </template>
            </Column>
        </DataTable>
    </div>
</template>