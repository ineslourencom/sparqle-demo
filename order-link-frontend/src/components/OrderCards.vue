<script setup lang="ts">
import { computed } from 'vue';
import type { Order } from '../types/order';
import { OrderStatus } from '../types/order';

const props = defineProps<{ orders: Order[] }>();

const metrics = computed(() => {
    const summary = {
    total: props.orders.length,
        completed: 0,
        processing: 0,
        retrying: 0,
        pending: 0,
        cancelled: 0,
        failed: 0,
    };

  props.orders.forEach(order => {
        switch (order.status) {
            case OrderStatus.COMPLETED:
                summary.completed += 1;
                break;
            case OrderStatus.PROCESSING:
                summary.processing += 1;
                break;
            case OrderStatus.RETRYING:
                summary.retrying += 1;
                break;
            case OrderStatus.PENDING:
                summary.pending += 1;
                break;
            case OrderStatus.CANCELLED:
                summary.cancelled += 1;
                break;
            case OrderStatus.FAILED:
                summary.failed += 1;
                break;
            default:
                break;
        }
    });

    return summary;
});
</script>


<template>
    <section class="summary-grid" aria-label="Order overview">
      <article class="summary-card total">
        <span class="summary-label">Total Orders</span>
        <span class="summary-value">{{ metrics.total }}</span>
      </article>
      <article class="summary-card completed">
        <span class="summary-label">Completed</span>
        <span class="summary-value">{{ metrics.completed }}</span>
      </article>
      <article class="summary-card processing">
        <span class="summary-label">Processing</span>
        <span class="summary-value">{{ metrics.processing }}</span>
      </article>
      <article class="summary-card pending">
        <span class="summary-label">Pending</span>
        <span class="summary-value">{{ metrics.pending }}</span>
      </article>
      <article class="summary-card retrying">
        <span class="summary-label">Retrying</span>
        <span class="summary-value">{{ metrics.retrying }}</span>
      </article>
      <article class="summary-card cancelled">
        <span class="summary-label">Cancelled</span>
        <span class="summary-value">{{ metrics.cancelled }}</span>
      </article>
      <article class="summary-card failed">
        <span class="summary-label">Failed</span>
        <span class="summary-value">{{ metrics.failed }}</span>
      </article>
    </section>
</template>
