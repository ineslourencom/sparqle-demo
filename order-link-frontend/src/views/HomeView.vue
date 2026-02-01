<script setup lang="ts">

import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import OrdersTable from '@/components/OrdersTable.vue';
import SearchBar from '../components/SearchBar.vue';
import OrderCards from '../components/OrderCards.vue';
import { fetchOrders as apiFetchOrders, subscribeToEvents as apiSubscribeToEvents, fetchOrderByReference as apiFetchOrderByReference } from '../services/api';
import type { Order, OrderEvent } from '../types/order';

const orders = ref<Order[]>([]);
const displayedOrders = ref<Order[]>([]);
const loading = ref(true);
const searchLoading = ref(false);
const searchQuery = ref('');

const fetchOrders = async () => {
  try {
    const page = await apiFetchOrders();
    orders.value = page.content;
    if (!searchQuery.value.trim()) {
      displayedOrders.value = page.content;
    }
  } catch (error) {
    console.error('Failed to fetch orders:', error);
  } finally {
    loading.value = false;
  }
};

const latestUpdate = computed(() => {
  if (!orders.value.length) {
    return null;
  }
  const timestamps = orders.value
    .map(order => new Date(order.updatedAt).getTime())
    .filter(timestamp => Number.isFinite(timestamp));

  if (!timestamps.length) {
    return null;
  }

  const latest = Math.max(...timestamps);
  return new Date(latest);
});

const formatDateTime = (value: Date | null) => {
  if (!value) {
    return 'No activity yet';
  }
  const locale = navigator.language || 'en-US';
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(value);
};

const handleOrderEvent = (event: OrderEvent) => {
  const updateCollection = (collection: Order[]) => {
    const index = collection.findIndex(order => order.merchantReference === event.orderId);
    if (index !== -1) {
      collection[index] = {
        ...collection[index],
        state: event.state,
        status: event.status,
        updatedAt: event.timestamp,
      };
      return true;
    }
    return false;
  };

  const updatedOrders = updateCollection(orders.value);
  const updatedDisplayed = updateCollection(displayedOrders.value);

  if (!updatedOrders && !updatedDisplayed) {
    console.warn('Received event for unknown order, refreshing list...', event.orderId);
    fetchOrders();
  }
};

let stopStream: (() => void) | null = null;

const subscribeToEvents = () => {
  stopStream?.();
  stopStream = apiSubscribeToEvents(
    handleOrderEvent,
    (error) => console.error('Subscription error:', error),
  );
};

watch(searchQuery, (value, _previous, onCleanup) => {
  const trimmed = value.trim();

  if (!trimmed) {
    searchLoading.value = false;
    displayedOrders.value = orders.value;
    return;
  }

  searchLoading.value = true;
  let cancelled = false;
  onCleanup(() => {
    cancelled = true;
  });

  (async () => {
    try {
      const result = await apiFetchOrderByReference(trimmed);
      if (cancelled) {
        return;
      }
      displayedOrders.value = result ? [result] : [];
    } catch (error) {
      if (cancelled) {
        return;
      }
      console.error('Failed to search orders:', error);
      displayedOrders.value = [];
    } finally {
      if (!cancelled) {
        searchLoading.value = false;
      }
    }
  })();
});

const isBusy = computed(() => loading.value || searchLoading.value);
const busyMessage = computed(() => (searchLoading.value ? 'Searching orders...' : 'Loading your orders...'));

onMounted(() => {
  fetchOrders();
  subscribeToEvents();
});

onUnmounted(() => {
  stopStream?.();
});
</script>

<template>
  <main class="orders-page">
    <section class="page-header">
      <div>
        <h1>My Orders</h1>
        <p class="page-subtitle">Track fulfilment progress and follow up on exceptions in real time.</p>
      </div>
      <div class="page-meta">
        <span class="meta-label">Last activity</span>
        <span class="meta-value">{{ formatDateTime(latestUpdate) }}</span>
      </div>
    </section>

    <OrderCards :orders="displayedOrders" />

    <SearchBar v-model="searchQuery" />

    <div v-if="isBusy" class="loading-panel">{{ busyMessage }}</div>
    <OrdersTable v-else :orders="displayedOrders" />
  </main>
</template>